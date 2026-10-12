package com.flypro.analytics.accesspoint.service;

import com.flypro.analytics.accesspoint.dto.AccessPointRequestDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointResponseDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointSearchCriteria;
import com.flypro.analytics.common.pagination.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Application service contract for the Access Point module.
 */
public interface AccessPointService {

    PageResponse<AccessPointResponseDTO> search(AccessPointSearchCriteria criteria, Pageable pageable);

    AccessPointResponseDTO getById(Long id);

    AccessPointResponseDTO create(AccessPointRequestDTO request);

    AccessPointResponseDTO update(Long id, AccessPointRequestDTO request);

    void delete(Long id);

    AccessPointResponseDTO activate(Long id);

    AccessPointResponseDTO deactivate(Long id);

    AccessPointResponseDTO putInMaintenance(Long id);

    List<AccessPointResponseDTO> listByZone(Long zoneId);
}
