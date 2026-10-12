from __future__ import annotations

from dataclasses import dataclass

from app.core.config import Settings
from app.models.enums import AccessResult, BadgeStatus, EmployeeStatus, RecommendedAction
from app.schemas.train import AccessEventFeatures


@dataclass
class RuleAssessment:
    flags: list[str]
    reasons: list[str]
    severity: float  # 0..1 additive boost towards anomaly


class BusinessRulesEngine:
    """Deterministic business rules that enrich ML predictions with explanations."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def evaluate(self, event: AccessEventFeatures) -> RuleAssessment:
        flags: list[str] = []
        reasons: list[str] = []
        severity = 0.0

        hour = event.event_timestamp.hour
        if hour < self._settings.workday_start_hour or hour >= self._settings.workday_end_hour:
            flags.append("OUTSIDE_WORKING_HOURS")
            reasons.append("Access outside normal working hours")
            severity += 0.25

        if event.denied_last_hour >= 3:
            flags.append("REPEATED_DENIED_ACCESSES")
            reasons.append("Repeated denied attempts in the last hour")
            severity += 0.35
        elif event.denied_last_hour >= 1 and event.access_result == AccessResult.DENIED:
            flags.append("DENIED_ACCESS")
            reasons.append("Access attempt was denied")
            severity += 0.15

        if event.accesses_today >= 30:
            flags.append("UNUSUAL_FREQUENCY")
            reasons.append("Unusual access frequency during the day")
            severity += 0.2

        if event.is_expected_zone is False:
            flags.append("UNEXPECTED_ZONE")
            reasons.append("Access to an unexpected zone")
            severity += 0.3

        if (
            event.previous_zone_id is not None
            and event.previous_zone_id != event.zone_id
            and event.minutes_since_previous_access is not None
            and event.minutes_since_previous_access < 5
        ):
            flags.append("IMPOSSIBLE_TRAVEL")
            reasons.append("Impossible travel between locations in a short time")
            severity += 0.4

        if event.employee_status in {
            EmployeeStatus.INACTIVE,
            EmployeeStatus.SUSPENDED,
            EmployeeStatus.ARCHIVED,
        }:
            flags.append("INACTIVE_EMPLOYEE")
            reasons.append("Inactive or suspended employee attempting access")
            severity += 0.45

        if event.badge_status == BadgeStatus.REVOKED:
            flags.append("REVOKED_BADGE")
            reasons.append("Revoked badge used")
            severity += 0.5
        elif event.badge_status == BadgeStatus.EXPIRED or event.access_result == AccessResult.EXPIRED:
            flags.append("EXPIRED_BADGE")
            reasons.append("Expired badge used")
            severity += 0.45
        elif event.badge_status in {BadgeStatus.SUSPENDED, BadgeStatus.LOST}:
            flags.append("INVALID_BADGE_STATUS")
            reasons.append(f"Badge status is {event.badge_status.value}")
            severity += 0.4

        return RuleAssessment(flags=flags, reasons=reasons, severity=min(severity, 1.0))

    @staticmethod
    def recommend_action(is_anomaly: bool, risk_score: float, flags: list[str]) -> RecommendedAction:
        critical = {
            "REVOKED_BADGE",
            "EXPIRED_BADGE",
            "INACTIVE_EMPLOYEE",
            "IMPOSSIBLE_TRAVEL",
        }
        if any(flag in critical for flag in flags) or risk_score >= 0.9:
            return RecommendedAction.BLOCK if "REVOKED_BADGE" in flags else RecommendedAction.INVESTIGATE
        if is_anomaly and risk_score >= 0.7:
            return RecommendedAction.INVESTIGATE
        if is_anomaly or risk_score >= 0.5:
            return RecommendedAction.REVIEW
        if risk_score >= 0.3:
            return RecommendedAction.MONITOR
        return RecommendedAction.NONE
