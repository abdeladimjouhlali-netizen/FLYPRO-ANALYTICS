from enum import Enum


class AccessResult(str, Enum):
    AUTHORIZED = "AUTHORIZED"
    DENIED = "DENIED"
    EXPIRED = "EXPIRED"
    UNKNOWN = "UNKNOWN"
    TECHNICAL_ERROR = "TECHNICAL_ERROR"


class AccessDirection(str, Enum):
    ENTRY = "ENTRY"
    EXIT = "EXIT"


class EmployeeStatus(str, Enum):
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    SUSPENDED = "SUSPENDED"
    ARCHIVED = "ARCHIVED"


class BadgeStatus(str, Enum):
    ACTIVE = "ACTIVE"
    SUSPENDED = "SUSPENDED"
    EXPIRED = "EXPIRED"
    REVOKED = "REVOKED"
    LOST = "LOST"
    PENDING_ACTIVATION = "PENDING_ACTIVATION"


class PredictionLabel(str, Enum):
    NORMAL = "NORMAL"
    ANOMALY = "ANOMALY"


class RecommendedAction(str, Enum):
    NONE = "No action required"
    MONITOR = "Monitor closely"
    REVIEW = "Review access logs"
    INVESTIGATE = "Investigate immediately"
    BLOCK = "Block badge and escalate"
