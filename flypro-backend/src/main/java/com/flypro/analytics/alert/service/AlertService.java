package com.flypro.analytics.alert.service;

import com.flypro.analytics.alert.dto.AlertRequestDTO;
import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.dto.AlertSearchCriteria;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.pagination.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Application service contract for the Alert Management module.
 */
public interface AlertService {

    PageResponse<AlertResponseDTO> search(AlertSearchCriteria criteria, Pageable pageable);

    AlertResponseDTO getById(Long id);

    AlertResponseDTO create(AlertRequestDTO request);

    AlertResponseDTO acknowledge(Long id);

    AlertResponseDTO resolve(Long id);

    AlertResponseDTO close(Long id);

    List<AlertResponseDTO> findByEmployee(Long employeeId);

    List<AlertResponseDTO> findByBadge(Long badgeId);

    List<AlertResponseDTO> findBySeverity(AlertSeverity severity);
}
