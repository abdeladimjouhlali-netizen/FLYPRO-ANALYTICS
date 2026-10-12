package com.flypro.analytics.accessprofile.validator;

import com.flypro.analytics.accessprofile.dto.AccessProfileRequestDTO;
import com.flypro.analytics.accessprofile.exception.DuplicateAccessProfileException;
import com.flypro.analytics.accessprofile.exception.InvalidWorkingHoursException;
import com.flypro.analytics.accessprofile.repository.AccessProfileRepository;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Business validation rules for access profiles.
 */
@Component
public class AccessProfileValidator {

    private final AccessProfileRepository accessProfileRepository;
    private final ZoneRepository zoneRepository;

    public AccessProfileValidator(AccessProfileRepository accessProfileRepository,
                                  ZoneRepository zoneRepository) {
        this.accessProfileRepository = accessProfileRepository;
        this.zoneRepository = zoneRepository;
    }

    public void validateCreate(AccessProfileRequestDTO request) {
        validateWorkingHours(request);
        validateZonesExist(request.zoneIds());
        if (accessProfileRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateAccessProfileException(request.name());
        }
    }

    public void validateUpdate(Long id, AccessProfileRequestDTO request) {
        validateWorkingHours(request);
        validateZonesExist(request.zoneIds());
        if (accessProfileRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateAccessProfileException(request.name());
        }
    }

    public void validateWorkingHours(AccessProfileRequestDTO request) {
        if (request.startTime() != null
                && request.endTime() != null
                && !request.startTime().isBefore(request.endTime())) {
            throw new InvalidWorkingHoursException("Start time must be before end time");
        }
    }

    public void validateZonesExist(Set<Long> zoneIds) {
        if (zoneIds == null || zoneIds.isEmpty()) {
            return;
        }
        Set<Long> requested = new HashSet<>(zoneIds);
        long found = zoneRepository.findByIdIn(requested).size();
        if (found != requested.size()) {
            Set<Long> existing = new HashSet<>();
            zoneRepository.findByIdIn(requested).forEach(z -> existing.add(z.getId()));
            Long missing = requested.stream()
                    .filter(id -> !existing.contains(id))
                    .findFirst()
                    .orElse(-1L);
            throw new ZoneNotFoundException(missing);
        }
    }

    public AccessProfileRequestDTO normalize(AccessProfileRequestDTO request) {
        return new AccessProfileRequestDTO(
                trim(request.name()),
                trimToNull(request.description()),
                request.securityLevel(),
                request.startTime(),
                request.endTime(),
                request.zoneIds() == null ? Set.of() : Set.copyOf(request.zoneIds())
        );
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
