package com.flypro.analytics.accesspoint.service;

import com.flypro.analytics.accesspoint.dto.AccessPointRequestDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointResponseDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointSearchCriteria;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accesspoint.exception.AccessPointCannotBeDeletedException;
import com.flypro.analytics.accesspoint.exception.AccessPointNotFoundException;
import com.flypro.analytics.accesspoint.mapper.AccessPointMapper;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.accesspoint.specification.AccessPointSpecification;
import com.flypro.analytics.accesspoint.validator.AccessPointValidator;
import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link AccessPointService}.
 */
@Service
@Transactional
public class AccessPointServiceImpl implements AccessPointService {

    private final AccessPointRepository accessPointRepository;
    private final ZoneRepository zoneRepository;
    private final AccessPointMapper accessPointMapper;
    private final AccessPointValidator accessPointValidator;

    public AccessPointServiceImpl(AccessPointRepository accessPointRepository,
                                  ZoneRepository zoneRepository,
                                  AccessPointMapper accessPointMapper,
                                  AccessPointValidator accessPointValidator) {
        this.accessPointRepository = accessPointRepository;
        this.zoneRepository = zoneRepository;
        this.accessPointMapper = accessPointMapper;
        this.accessPointValidator = accessPointValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccessPointResponseDTO> search(AccessPointSearchCriteria criteria, Pageable pageable) {
        Specification<AccessPoint> specification = AccessPointSpecification.withCriteria(criteria);
        Page<AccessPointResponseDTO> page = accessPointRepository.findAll(specification, pageable)
                .map(accessPointMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessPointResponseDTO getById(Long id) {
        return accessPointMapper.toResponse(findAccessPoint(id));
    }

    @Override
    public AccessPointResponseDTO create(AccessPointRequestDTO request) {
        AccessPointRequestDTO normalized = accessPointValidator.normalize(request);
        accessPointValidator.validateCreate(normalized);

        AccessPoint accessPoint = accessPointMapper.toEntity(normalized);
        accessPoint.setZone(resolveZone(normalized.zoneId()));
        AccessPoint saved = accessPointRepository.save(accessPoint);
        return accessPointMapper.toResponse(saved);
    }

    @Override
    public AccessPointResponseDTO update(Long id, AccessPointRequestDTO request) {
        AccessPoint accessPoint = findAccessPoint(id);
        AccessPointRequestDTO normalized = accessPointValidator.normalize(request);
        accessPointValidator.validateUpdate(id, normalized);

        accessPointMapper.updateEntity(normalized, accessPoint);
        accessPoint.setZone(resolveZone(normalized.zoneId()));
        return accessPointMapper.toResponse(accessPointRepository.save(accessPoint));
    }

    @Override
    public void delete(Long id) {
        AccessPoint accessPoint = findAccessPoint(id);
        if (accessPointRepository.hasAccessEvents(id)) {
            throw new AccessPointCannotBeDeletedException(
                    "Access point cannot be deleted because access events are linked"
            );
        }
        accessPointRepository.delete(accessPoint);
    }

    @Override
    public AccessPointResponseDTO activate(Long id) {
        AccessPoint accessPoint = findAccessPoint(id);
        accessPoint.setStatus(AccessPointStatus.ACTIVE);
        return accessPointMapper.toResponse(accessPointRepository.save(accessPoint));
    }

    @Override
    public AccessPointResponseDTO deactivate(Long id) {
        AccessPoint accessPoint = findAccessPoint(id);
        accessPoint.setStatus(AccessPointStatus.INACTIVE);
        return accessPointMapper.toResponse(accessPointRepository.save(accessPoint));
    }

    @Override
    public AccessPointResponseDTO putInMaintenance(Long id) {
        AccessPoint accessPoint = findAccessPoint(id);
        accessPoint.setStatus(AccessPointStatus.MAINTENANCE);
        return accessPointMapper.toResponse(accessPointRepository.save(accessPoint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessPointResponseDTO> listByZone(Long zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ZoneNotFoundException(zoneId);
        }
        return accessPointRepository.findByZoneIdOrderByNameAsc(zoneId).stream()
                .map(accessPointMapper::toResponse)
                .toList();
    }

    private AccessPoint findAccessPoint(Long id) {
        return accessPointRepository.findById(id)
                .orElseThrow(() -> new AccessPointNotFoundException(id));
    }

    private Zone resolveZone(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
    }
}
