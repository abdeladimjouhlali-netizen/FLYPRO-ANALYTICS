package com.flypro.analytics.accessprofile.service;

import com.flypro.analytics.accessprofile.dto.AccessProfileRequestDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileResponseDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileSearchCriteria;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.accessprofile.exception.AccessProfileCannotBeDeletedException;
import com.flypro.analytics.accessprofile.exception.AccessProfileMustHaveZoneException;
import com.flypro.analytics.accessprofile.exception.AccessProfileNotFoundException;
import com.flypro.analytics.accessprofile.exception.ZoneAlreadyAssignedException;
import com.flypro.analytics.accessprofile.mapper.AccessProfileMapper;
import com.flypro.analytics.accessprofile.repository.AccessProfileRepository;
import com.flypro.analytics.accessprofile.specification.AccessProfileSpecification;
import com.flypro.analytics.accessprofile.validator.AccessProfileValidator;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AccessProfileService}.
 */
@Service
@Transactional
public class AccessProfileServiceImpl implements AccessProfileService {

    private final AccessProfileRepository accessProfileRepository;
    private final ZoneRepository zoneRepository;
    private final AccessProfileMapper accessProfileMapper;
    private final AccessProfileValidator accessProfileValidator;

    public AccessProfileServiceImpl(AccessProfileRepository accessProfileRepository,
                                    ZoneRepository zoneRepository,
                                    AccessProfileMapper accessProfileMapper,
                                    AccessProfileValidator accessProfileValidator) {
        this.accessProfileRepository = accessProfileRepository;
        this.zoneRepository = zoneRepository;
        this.accessProfileMapper = accessProfileMapper;
        this.accessProfileValidator = accessProfileValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccessProfileResponseDTO> search(AccessProfileSearchCriteria criteria, Pageable pageable) {
        Specification<AccessProfile> specification = AccessProfileSpecification.withCriteria(criteria);
        Page<AccessProfileResponseDTO> page = accessProfileRepository.findAll(specification, pageable)
                .map(accessProfileMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessProfileResponseDTO getById(Long id) {
        return accessProfileMapper.toResponse(findProfile(id));
    }

    @Override
    public AccessProfileResponseDTO create(AccessProfileRequestDTO request) {
        AccessProfileRequestDTO normalized = accessProfileValidator.normalize(request);
        accessProfileValidator.validateCreate(normalized);

        AccessProfile profile = accessProfileMapper.toEntity(normalized);
        profile.setZones(resolveZones(normalized.zoneIds()));
        AccessProfile saved = accessProfileRepository.save(profile);
        return accessProfileMapper.toResponse(saved);
    }

    @Override
    public AccessProfileResponseDTO update(Long id, AccessProfileRequestDTO request) {
        AccessProfile profile = findProfile(id);
        AccessProfileRequestDTO normalized = accessProfileValidator.normalize(request);
        accessProfileValidator.validateUpdate(id, normalized);

        accessProfileMapper.updateEntity(normalized, profile);
        profile.setZones(resolveZones(normalized.zoneIds()));
        return accessProfileMapper.toResponse(accessProfileRepository.save(profile));
    }

    @Override
    public void delete(Long id) {
        AccessProfile profile = findProfile(id);
        if (accessProfileRepository.hasEmployees(id)) {
            throw new AccessProfileCannotBeDeletedException(
                    "Access profile cannot be deleted because employees are still assigned"
            );
        }
        profile.getZones().clear();
        accessProfileRepository.delete(profile);
    }

    @Override
    public AccessProfileResponseDTO assignZone(Long profileId, Long zoneId) {
        AccessProfile profile = findProfile(profileId);
        Zone zone = findZone(zoneId);

        boolean alreadyAssigned = profile.getZones().stream()
                .anyMatch(z -> z.getId().equals(zoneId));
        if (alreadyAssigned) {
            throw new ZoneAlreadyAssignedException(profileId, zoneId);
        }

        profile.getZones().add(zone);
        return accessProfileMapper.toResponse(accessProfileRepository.save(profile));
    }

    @Override
    public AccessProfileResponseDTO removeZone(Long profileId, Long zoneId) {
        AccessProfile profile = findProfile(profileId);
        findZone(zoneId);

        boolean assigned = profile.getZones().stream()
                .anyMatch(z -> z.getId().equals(zoneId));
        if (!assigned) {
            throw new ZoneNotFoundException(zoneId);
        }
        if (profile.getZones().size() <= 1) {
            throw new AccessProfileMustHaveZoneException();
        }

        profile.getZones().removeIf(z -> z.getId().equals(zoneId));
        return accessProfileMapper.toResponse(accessProfileRepository.save(profile));
    }

    private Set<Zone> resolveZones(Set<Long> zoneIds) {
        List<Zone> zones = zoneRepository.findByIdIn(zoneIds);
        if (zones.size() != zoneIds.size()) {
            Set<Long> found = zones.stream().map(Zone::getId).collect(Collectors.toSet());
            Long missing = zoneIds.stream().filter(id -> !found.contains(id)).findFirst().orElse(-1L);
            throw new ZoneNotFoundException(missing);
        }
        return new HashSet<>(zones);
    }

    private AccessProfile findProfile(Long id) {
        return accessProfileRepository.findById(id)
                .orElseThrow(() -> new AccessProfileNotFoundException(id));
    }

    private Zone findZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ZoneNotFoundException(id));
    }
}
