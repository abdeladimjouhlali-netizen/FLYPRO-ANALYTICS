from fastapi.testclient import TestClient

from tests.conftest import make_training_events


def _event_payload(**overrides):
    base = {
        "eventTimestamp": "2026-07-20T10:00:00Z",
        "employeeId": 1,
        "badgeId": 1,
        "accessPointId": 1,
        "zoneId": 1,
        "accessResult": "AUTHORIZED",
        "accessDirection": "ENTRY",
        "deniedLastHour": 0,
        "accessesToday": 3,
        "employeeStatus": "ACTIVE",
        "badgeStatus": "ACTIVE",
    }
    base.update(overrides)
    return base


def test_health(client: TestClient):
    response = client.get("/api/v1/ai/health")
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["data"]["status"] == "UP"


def test_train_predict_model_flow(client: TestClient):
    events = []
    for i, event in enumerate(make_training_events(30)):
        events.append(
            {
                "eventTimestamp": event.event_timestamp.isoformat().replace("+00:00", "Z"),
                "employeeId": event.employee_id,
                "badgeId": event.badge_id,
                "accessPointId": event.access_point_id,
                "zoneId": event.zone_id,
                "accessResult": event.access_result.value,
                "accessDirection": event.access_direction.value,
                "deniedLastHour": event.denied_last_hour,
                "accessesToday": event.accesses_today,
            }
        )

    train = client.post("/api/v1/ai/train", json={"events": events, "versionLabel": "api"})
    assert train.status_code == 201
    assert train.json()["data"]["samples"] == 30

    model = client.get("/api/v1/ai/model")
    assert model.status_code == 200
    assert model.json()["data"]["loaded"] is True

    predict = client.post(
        "/api/v1/ai/predict",
        json={
            "event": _event_payload(
                eventTimestamp="2026-07-20T23:30:00Z",
                accessResult="DENIED",
                deniedLastHour=4,
                badgeStatus="REVOKED",
            )
        },
    )
    assert predict.status_code == 200
    data = predict.json()["data"]
    assert data["prediction"] == "ANOMALY"
    assert data["riskScore"] >= 0.5
    assert "reason" in data
    assert "recommendedAction" in data

    retrain = client.post("/api/v1/ai/retrain", json={"events": events, "versionLabel": "api2"})
    assert retrain.status_code == 201


def test_predict_without_model_returns_404(client: TestClient):
    response = client.post("/api/v1/ai/predict", json={"event": _event_payload()})
    assert response.status_code == 404
    assert response.json()["code"] == "MODEL_NOT_FOUND"
