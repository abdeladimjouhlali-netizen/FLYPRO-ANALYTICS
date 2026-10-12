package com.flypro.analytics.zone.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.zone.dto.ZoneRequestDTO;
import com.flypro.analytics.zone.dto.ZoneResponseDTO;
import com.flypro.analytics.zone.dto.ZoneSearchCriteria;
import org.springframework.data.domain.Pageable;

/**
 * Application service contract for the Zone module.
 */
public interface ZoneService {

    PageResponse<ZoneResponseDTO> search(ZoneSearchCriteria criteria, Pageable pageable);

    ZoneResponseDTO getById(Long id);

    ZoneResponseDTO create(ZoneRequestDTO request);

    ZoneResponseDTO update(Long id, ZoneRequestDTO request);

    void delete(Long id);
}
