package com.flypro.analytics.accessevent.service;

import com.flypro.analytics.accessevent.dto.AccessEventRequestDTO;
import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.accessevent.dto.AccessEventSearchCriteria;
import com.flypro.analytics.common.pagination.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Application service contract for the Access Event module.
 */
public interface AccessEventService {

    PageResponse<AccessEventResponseDTO> search(AccessEventSearchCriteria criteria, Pageable pageable);

    AccessEventResponseDTO getById(Long id);

    AccessEventResponseDTO create(AccessEventRequestDTO request);
}
