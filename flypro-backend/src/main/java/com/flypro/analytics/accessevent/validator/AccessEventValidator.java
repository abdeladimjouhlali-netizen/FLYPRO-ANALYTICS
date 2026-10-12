package com.flypro.analytics.accessevent.validator;

import com.flypro.analytics.accessevent.dto.AccessEventRequestDTO;
import com.flypro.analytics.accessevent.exception.InvalidAccessEventException;
import com.flypro.analytics.accesspoint.exception.AccessPointNotFoundException;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Business validation for access event creation.
 */
@Component
public class AccessEventValidator {

    private final BadgeRepository badgeRepository;
    private final AccessPointRepository accessPointRepository;
    private final ZoneRepository zoneRepository;

    public AccessEventValidator(BadgeRepository badgeRepository,
                                AccessPointRepository accessPointRepository,
                                ZoneRepository zoneRepository) {
        this.badgeRepository = badgeRepository;
        this.accessPointRepository = accessPointRepository;
        this.zoneRepository = zoneRepository;
    }

    public void validateCreate(AccessEventRequestDTO request) {
        if (request.eventTimestamp() == null) {
            throw new InvalidAccessEventException("Event timestamp is required");
        }
        if (request.direction() == null) {
            throw new InvalidAccessEventException("Direction is required");
        }
        if (request.result() == null) {
            throw new InvalidAccessEventException("Access result is required");
        }
        if (request.source() == null) {
            throw new InvalidAccessEventException("Event source is required");
        }
        if (request.badgeId() == null || !badgeRepository.existsById(request.badgeId())) {
            throw new BadgeNotFoundException(request.badgeId() == null ? -1L : request.badgeId());
        }
        if (request.accessPointId() == null || !accessPointRepository.existsById(request.accessPointId())) {
            throw new AccessPointNotFoundException(
                    request.accessPointId() == null ? -1L : request.accessPointId()
            );
        }
        if (request.zoneId() != null && !zoneRepository.existsById(request.zoneId())) {
            throw new ZoneNotFoundException(request.zoneId());
        }
    }

    public AccessEventRequestDTO normalize(AccessEventRequestDTO request) {
        return new AccessEventRequestDTO(
                request.eventTimestamp(),
                request.direction(),
                request.result(),
                trimToNull(request.refusalReason()),
                request.source(),
                trimToNull(request.rawPayload()),
                request.badgeId(),
                request.accessPointId(),
                request.zoneId()
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
