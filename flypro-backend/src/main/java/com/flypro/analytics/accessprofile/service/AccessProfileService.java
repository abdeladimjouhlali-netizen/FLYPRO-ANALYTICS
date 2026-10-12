package com.flypro.analytics.accessprofile.service;

import com.flypro.analytics.accessprofile.dto.AccessProfileRequestDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileResponseDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileSearchCriteria;
import com.flypro.analytics.common.pagination.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Application service contract for the Access Profile module.
 */
public interface AccessProfileService {

    PageResponse<AccessProfileResponseDTO> search(AccessProfileSearchCriteria criteria, Pageable pageable);

    AccessProfileResponseDTO getById(Long id);

    AccessProfileResponseDTO create(AccessProfileRequestDTO request);

    AccessProfileResponseDTO update(Long id, AccessProfileRequestDTO request);

    void delete(Long id);

    AccessProfileResponseDTO assignZone(Long profileId, Long zoneId);

    AccessProfileResponseDTO removeZone(Long profileId, Long zoneId);
}
