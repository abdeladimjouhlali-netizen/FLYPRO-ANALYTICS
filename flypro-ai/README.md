# FlyPro AI — Anomaly Detection Microservice

Independent FastAPI service that detects anomalous badge access events for **FlyPro Analytics**.

It communicates with the Spring Boot backend **only through REST APIs** (no direct database access).

## Stack

- Python 3.12
- FastAPI + Uvicorn
- Scikit-Learn (Isolation Forest)
- Pandas / NumPy / Joblib
- Pydantic v2
- Pytest

## Quick start

```bash
cd flypro-ai
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux/macOS
source .venv/bin/activate

pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Open Swagger UI: [http://localhost:8000/docs](http://localhost:8000/docs)

## Docker

```bash
docker build -t flypro-ai .
docker run --rm -p 8000:8000 -v flypro-ai-models:/app/artifacts/models flypro-ai
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/ai/health` | Health + model status |
| `GET` | `/api/v1/ai/model` | Current model metadata |
| `POST` | `/api/v1/ai/model/load` | Load latest or specific version |
| `POST` | `/api/v1/ai/model/save` | Persist in-memory model |
| `POST` | `/api/v1/ai/train` | Train Isolation Forest |
| `POST` | `/api/v1/ai/retrain` | Retrain and version a new model |
| `POST` | `/api/v1/ai/evaluate` | Evaluate loaded model |
| `POST` | `/api/v1/ai/predict` | Predict single event |
| `POST` | `/api/v1/ai/predict/batch` | Predict batch |

### Predict example

```http
POST /api/v1/ai/predict
Content-Type: application/json

{
  "event": {
    "eventTimestamp": "2026-07-20T22:15:00Z",
    "employeeId": 42,
    "badgeId": 7,
    "accessPointId": 3,
    "zoneId": 2,
    "accessResult": "DENIED",
    "accessDirection": "ENTRY",
    "deniedLastHour": 4,
    "accessesToday": 12,
    "employeeStatus": "ACTIVE",
    "badgeStatus": "ACTIVE"
  }
}
```

```json
{
  "success": true,
  "message": "Prediction completed",
  "data": {
    "prediction": "ANOMALY",
    "riskScore": 0.94,
    "confidence": 98.5,
    "reason": "Access outside normal working hours with repeated denied attempts.",
    "recommendedAction": "Investigate immediately",
    "modelVersion": "20260727120000",
    "ruleFlags": ["OUTSIDE_WORKING_HOURS", "REPEATED_DENIED_ACCESSES"]
  }
}
```

## Features used by the model

1. Hour of access  
2. Day of week  
3. Employee ID  
4. Badge ID  
5. Access Point ID  
6. Zone ID  
7. Access Result (encoded)  
8. Access Direction (encoded)  
9. Denied accesses in the last hour  
10. Accesses during the day  

Optional context fields enrich **business rules** (inactive employee, revoked/expired badge, unexpected zone, impossible travel).

## Architecture

```
app/
  api/          # FastAPI routers + DI
  core/         # config, logging, exceptions
  schemas/      # Pydantic request/response DTOs
  models/       # domain enums
  services/     # training, prediction, registry, rules
  ml/           # feature engineering, preprocessor, Isolation Forest
  utils/
```

Models are versioned under `artifacts/models/` and loaded automatically at startup when available.

## Tests

```bash
pytest
```

## Integration with Spring Boot

Spring Boot should call this service over HTTP (e.g. `http://flypro-ai:8000/api/v1/ai/predict`) after access events are created or imported. Payload fields use **camelCase** for Java interoperability.
