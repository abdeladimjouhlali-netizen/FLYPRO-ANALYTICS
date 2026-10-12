package com.flypro.analytics.zone.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.config.CacheConfig;
import com.flypro.analytics.zone.dto.ZoneRequestDTO;
import com.flypro.analytics.zone.dto.ZoneResponseDTO;
import com.flypro.analytics.zone.dto.ZoneSearchCriteria;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.exception.ZoneCannotBeDeletedException;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.mapper.ZoneMapper;
import com.flypro.analytics.zone.repository.ZoneRepository;
import com.flypro.analytics.zone.specification.ZoneSpecification;
import com.flypro.analytics.zone.validator.ZoneValidator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ZoneService}.
 */
@Service
@Transactional
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final ZoneMapper zoneMapper;
    private final ZoneValidator zoneValidator;

    public ZoneServiceImpl(ZoneRepository zoneRepository,
                           ZoneMapper zoneMapper,
                           ZoneValidator zoneValidator) {
        this.zoneRepository = zoneRepository;
        this.zoneMapper = zoneMapper;
        this.zoneValidator = zoneValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ZoneResponseDTO> search(ZoneSearchCriteria criteria, Pageable pageable) {
        Specification<Zone> specification = ZoneSpecification.withCriteria(criteria);
        Page<ZoneResponseDTO> page = zoneRepository.findAll(specification, pageable)
                .map(zoneMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REF_DATA, key = "'zone:' + #id")
    public ZoneResponseDTO getById(Long id) {
        return zoneMapper.toResponse(findZone(id));
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.REF_DATA, allEntries = true)
    public ZoneResponseDTO create(ZoneRequestDTO request) {
        ZoneRequestDTO normalized = zoneValidator.normalize(request);
        zoneValidator.validateCreate(normalized);

        Zone zone = zoneMapper.toEntity(normalized);
        zone.setSensitive(Boolean.TRUE.equals(normalized.sensitive()));
        Zone saved = zoneRepository.save(zone);
        return zoneMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.REF_DATA, allEntries = true)
    public ZoneResponseDTO update(Long id, ZoneRequestDTO request) {
        Zone zone = findZone(id);
        ZoneRequestDTO normalized = zoneValidator.normalize(request);
        zoneValidator.validateUpdate(id, normalized);

        zoneMapper.updateEntity(normalized, zone);
        if (normalized.sensitive() != null) {
            zone.setSensitive(normalized.sensitive());
        }
        return zoneMapper.toResponse(zoneRepository.save(zone));
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.REF_DATA, allEntries = true)
    public void delete(Long id) {
        Zone zone = findZone(id);
        if (zoneRepository.hasAccessPoints(id)) {
            throw new ZoneCannotBeDeletedException(
                    "Zone cannot be deleted because access points are still linked"
            );
        }
        if (zoneRepository.isAssignedToAnyProfile(id)) {
            throw new ZoneCannotBeDeletedException(
                    "Zone cannot be deleted because it is assigned to access profiles"
            );
        }
        zoneRepository.delete(zone);
    }

    private Zone findZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ZoneNotFoundException(id));
    }
}
