package com.flypro.analytics.accesspoint.validator;

import com.flypro.analytics.accesspoint.dto.AccessPointRequestDTO;
import com.flypro.analytics.accesspoint.exception.DuplicateAccessPointCodeException;
import com.flypro.analytics.accesspoint.exception.DuplicateAccessPointNameException;
import com.flypro.analytics.accesspoint.exception.InvalidIpAddressException;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Business validation rules for access points.
 */
@Component
public class AccessPointValidator {

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );
    private static final Pattern IPV6 = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
                    + "|^(([0-9a-fA-F]{1,4}:){1,7}:)"
                    + "|^(([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4})"
                    + "|^(([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2})"
                    + "|^(([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3})"
                    + "|^(([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4})"
                    + "|^(([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5})"
                    + "|^([0-9a-fA-F]{1,4}:)((:[0-9a-fA-F]{1,4}){1,6})"
                    + "|^:((:[0-9a-fA-F]{1,4}){1,7}|:)$"
    );

    private final AccessPointRepository accessPointRepository;
    private final ZoneRepository zoneRepository;

    public AccessPointValidator(AccessPointRepository accessPointRepository,
                                ZoneRepository zoneRepository) {
        this.accessPointRepository = accessPointRepository;
        this.zoneRepository = zoneRepository;
    }

    public void validateCreate(AccessPointRequestDTO request) {
        validateZoneExists(request.zoneId());
        validateIpAddress(request.ipAddress());
        if (accessPointRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateAccessPointCodeException(request.code());
        }
        if (accessPointRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateAccessPointNameException(request.name());
        }
    }

    public void validateUpdate(Long id, AccessPointRequestDTO request) {
        validateZoneExists(request.zoneId());
        validateIpAddress(request.ipAddress());
        if (accessPointRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new DuplicateAccessPointCodeException(request.code());
        }
        if (accessPointRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateAccessPointNameException(request.name());
        }
    }

    public void validateZoneExists(Long zoneId) {
        if (zoneId == null || !zoneRepository.existsById(zoneId)) {
            throw new ZoneNotFoundException(zoneId == null ? -1L : zoneId);
        }
    }

    public void validateIpAddress(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return;
        }
        String trimmed = ipAddress.trim();
        if (!IPV4.matcher(trimmed).matches() && !IPV6.matcher(trimmed).matches()) {
            throw new InvalidIpAddressException(trimmed);
        }
    }

    public AccessPointRequestDTO normalize(AccessPointRequestDTO request) {
        return new AccessPointRequestDTO(
                trim(request.name()),
                trim(request.code()) == null ? null : request.code().trim().toUpperCase(),
                request.type(),
                trimToNull(request.description()),
                request.status(),
                trimToNull(request.ipAddress()),
                trimToNull(request.location()),
                request.zoneId()
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
