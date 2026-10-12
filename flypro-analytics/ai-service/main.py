"""
FlyPro Analytics – AI Service (FastAPI)

Point d'entrée du microservice d'analyse et de détection d'anomalies.

Rôle futur :
  - Exposer des endpoints REST pour scoring / anomaly detection
  - Orchestrer les pipelines ML (Pandas, NumPy, Scikit-learn)
  - Être appelé par le backend Spring Boot (module analytics)

Aucune logique métier ni modèle ML n'est implémenté à ce stade.
"""

from fastapi import FastAPI

app = FastAPI(
    title="FlyPro Analytics – AI Service",
    description="Microservice IA pour la détection d'anomalies d'accès (squelette).",
    version="0.0.1",
)


@app.get("/health")
def health() -> dict[str, str]:
    """Endpoint de santé — vérifie que le service démarre correctement."""
    return {"status": "ok", "service": "flypro-ai"}
