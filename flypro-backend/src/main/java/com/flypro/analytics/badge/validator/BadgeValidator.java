package com.flypro.analytics.badge.validator;

import com.flypro.analytics.badge.dto.BadgeRequestDTO;
import com.flypro.analytics.badge.exception.DuplicateBadgeNumberException;
import com.flypro.analytics.badge.exception.InvalidBadgeDatesException;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.common.enums.BadgeStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Business validation rules for badges (uniqueness + dates).
 */
@Component
public class BadgeValidator {

    private final BadgeRepository badgeRepository;

    public BadgeValidator(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    public void validateCreate(BadgeRequestDTO request) {
        validateDates(request.issueDate(), request.expirationDate());
        if (badgeRepository.existsByBadgeNumberIgnoreCase(request.badgeNumber())) {
            throw new DuplicateBadgeNumberException(request.badgeNumber());
        }
    }

    public void validateUpdate(Long id, BadgeRequestDTO request) {
        validateDates(request.issueDate(), request.expirationDate());
        if (badgeRepository.existsByBadgeNumberIgnoreCaseAndIdNot(request.badgeNumber(), id)) {
            throw new DuplicateBadgeNumberException(request.badgeNumber());
        }
    }

    public void validateDates(java.time.LocalDate issueDate, java.time.LocalDate expirationDate) {
        if (issueDate != null && expirationDate != null && !expirationDate.isAfter(issueDate)) {
            throw new InvalidBadgeDatesException("Expiration date must be after issue date");
        }
    }

    /**
     * Normalizes payload: trim badge number; force EXPIRED when expiration already passed for ACTIVE.
     */
    public BadgeRequestDTO normalize(BadgeRequestDTO request) {
        BadgeStatus status = request.status();
        if (status == BadgeStatus.ACTIVE
                && request.expirationDate() != null
                && request.expirationDate().isBefore(java.time.LocalDate.now())) {
            status = BadgeStatus.EXPIRED;
        }

        return new BadgeRequestDTO(
                trimToNull(request.badgeNumber()),
                request.type(),
                status,
                request.issueDate(),
                request.expirationDate()
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return value == null ? null : value.trim();
        }
        return value.trim();
    }
}
