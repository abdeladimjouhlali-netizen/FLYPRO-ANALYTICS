package com.flypro.analytics.badge.service;

import com.flypro.analytics.badge.dto.BadgeAssignmentDTO;
import com.flypro.analytics.badge.dto.BadgeRenewRequestDTO;
import com.flypro.analytics.badge.dto.BadgeReplaceRequestDTO;
import com.flypro.analytics.badge.dto.BadgeRequestDTO;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.dto.BadgeSearchCriteria;
import com.flypro.analytics.common.pagination.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Application service contract for the Badge Management module.
 */
public interface BadgeService {

    PageResponse<BadgeResponseDTO> search(BadgeSearchCriteria criteria, Pageable pageable);

    BadgeResponseDTO getById(Long id);

    BadgeResponseDTO create(BadgeRequestDTO request);

    BadgeResponseDTO update(Long id, BadgeRequestDTO request);

    void delete(Long id);

    BadgeResponseDTO assign(Long badgeId, Long employeeId);

    BadgeResponseDTO unassign(Long badgeId);

    BadgeResponseDTO renew(Long badgeId, BadgeRenewRequestDTO request);

    BadgeResponseDTO replace(Long badgeId, BadgeReplaceRequestDTO request);

    BadgeResponseDTO suspend(Long badgeId);

    BadgeResponseDTO activate(Long badgeId);

    BadgeResponseDTO revoke(Long badgeId);

    BadgeResponseDTO reportLost(Long badgeId);

    List<BadgeAssignmentDTO> getHistory(Long badgeId);

    List<BadgeResponseDTO> listEmployeeBadges(Long employeeId);
}
