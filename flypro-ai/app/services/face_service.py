from __future__ import annotations

import base64
import json
from pathlib import Path

import cv2
import numpy as np

from app.core.config import Settings
from app.core.exceptions import FaceNotEnrolledError, NoFaceDetectedError
from app.core.logging import get_logger
from app.schemas.face import FaceEnrollResponse, FaceVerifyResponse

logger = get_logger(__name__)

FACE_SIZE = (200, 200)
MATCH_DISTANCE_THRESHOLD = 75.0


class FaceRecognitionService:
    """Enrôlement et vérification de visages, entièrement locaux et hors-ligne.

    Utilise un détecteur Haar Cascade (OpenCV) pour localiser le visage puis un
    reconnaisseur LBPH (Local Binary Pattern Histogram) pour la comparaison.
    Indépendant du module Isolation Forest : artefacts et échecs séparés,
    aucun service de vision externe requis.

    Avertissement : ce module traite des données biométriques à des fins de
    démonstration académique (PFE). Il ne met pas en œuvre de gestion de
    consentement ni de chiffrement dédié des gabarits biométriques et ne doit
    pas être utilisé tel quel en production.
    """

    def __init__(self, settings: Settings) -> None:
        self._dir = Path(settings.models_dir)
        self._dir.mkdir(parents=True, exist_ok=True)
        self._model_path = self._dir / "face_recognizer.yml"
        self._labels_path = self._dir / "face_labels.json"

        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        self._detector = cv2.CascadeClassifier(cascade_path)
        self._recognizer = cv2.face.LBPHFaceRecognizer_create()

        self._labels: dict[str, int] = {}
        self._samples: dict[str, int] = {}
        self._next_label = 0
        self._trained = False

        self._load()

    def _load(self) -> None:
        if self._labels_path.exists():
            data = json.loads(self._labels_path.read_text(encoding="utf-8"))
            self._labels = data.get("labels", {})
            self._samples = data.get("samples", {})
            self._next_label = data.get("next_label", 0)
        if self._model_path.exists() and self._labels:
            try:
                self._recognizer.read(str(self._model_path))
                self._trained = True
                logger.info("Face model loaded, %d enrolled employee(s)", len(self._labels))
            except Exception as exc:  # noqa: BLE001
                logger.warning("Failed to load face model: %s", exc)

    def _persist(self) -> None:
        self._recognizer.write(str(self._model_path))
        self._labels_path.write_text(
            json.dumps(
                {"labels": self._labels, "samples": self._samples, "next_label": self._next_label},
                indent=2,
            ),
            encoding="utf-8",
        )

    @staticmethod
    def _decode(image_base64: str) -> np.ndarray:
        raw = image_base64.split(",")[-1]
        try:
            buf = np.frombuffer(base64.b64decode(raw), dtype=np.uint8)
        except Exception as exc:  # noqa: BLE001
            raise NoFaceDetectedError("Invalid image payload") from exc
        img = cv2.imdecode(buf, cv2.IMREAD_GRAYSCALE)
        if img is None:
            raise NoFaceDetectedError("Unreadable image payload")
        return img

    def _extract_face(self, gray: np.ndarray) -> np.ndarray:
        faces = self._detector.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(80, 80))
        if len(faces) == 0:
            raise NoFaceDetectedError()
        x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
        face = gray[y : y + h, x : x + w]
        face = cv2.resize(face, FACE_SIZE)
        face = cv2.equalizeHist(face)
        return face

    def enroll(self, employee_id: int, image_base64: str) -> FaceEnrollResponse:
        gray = self._decode(image_base64)
        face = self._extract_face(gray)
        key = str(employee_id)

        if key not in self._labels:
            self._labels[key] = self._next_label
            self._next_label += 1
            self._samples[key] = 0

        label = self._labels[key]
        if self._trained:
            self._recognizer.update([face], np.array([label]))
        else:
            self._recognizer.train([face], np.array([label]))
            self._trained = True

        self._samples[key] = self._samples.get(key, 0) + 1
        self._persist()
        logger.info("Face enrolled employeeId=%s samples=%d", employee_id, self._samples[key])
        return FaceEnrollResponse(employee_id=employee_id, enrolled=True, samples_count=self._samples[key])

    def verify(self, employee_id: int, image_base64: str) -> FaceVerifyResponse:
        key = str(employee_id)
        if not self._trained or key not in self._labels:
            raise FaceNotEnrolledError()

        gray = self._decode(image_base64)
        face = self._extract_face(gray)
        predicted_label, distance = self._recognizer.predict(face)

        expected_label = self._labels[key]
        matched = predicted_label == expected_label and distance <= MATCH_DISTANCE_THRESHOLD
        confidence = max(0.0, min(1.0, 1 - (distance / (MATCH_DISTANCE_THRESHOLD * 2))))

        message = (
            "Visage reconnu : correspond au profil enrôlé pour cet employé."
            if matched
            else "Le visage capturé ne correspond pas au profil enrôlé pour cet employé."
        )
        return FaceVerifyResponse(
            employee_id=employee_id,
            matched=matched,
            confidence=round(confidence, 4),
            distance=round(float(distance), 2),
            message=message,
        )

    @property
    def enrolled_count(self) -> int:
        return len(self._labels)
