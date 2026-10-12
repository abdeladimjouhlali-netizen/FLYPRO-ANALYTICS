package com.flypro.analytics.zone.validator;

import com.flypro.analytics.zone.dto.ZoneRequestDTO;
import com.flypro.analytics.zone.exception.DuplicateZoneException;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Business validation rules for zones.
 */
@Component
public class ZoneValidator {

    private final ZoneRepository zoneRepository;

    public ZoneValidator(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    public void validateCreate(ZoneRequestDTO request) {
        if (zoneRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateZoneException(request.name());
        }
    }

    public void validateUpdate(Long id, ZoneRequestDTO request) {
        if (zoneRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateZoneException(request.name());
        }
    }

    public ZoneRequestDTO normalize(ZoneRequestDTO request) {
        return new ZoneRequestDTO(
                trim(request.name()),
                trim(request.building()),
                trim(request.floor()),
                request.sensitive(),
                trimToNull(request.description())
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
