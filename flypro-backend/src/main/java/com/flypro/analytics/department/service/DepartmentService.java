package com.flypro.analytics.department.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.department.dto.DepartmentRequest;
import com.flypro.analytics.department.dto.DepartmentResponse;
import com.flypro.analytics.department.dto.DepartmentSearchCriteria;
import org.springframework.data.domain.Pageable;

/**
 * Application service contract for the Department module.
 */
public interface DepartmentService {

    PageResponse<DepartmentResponse> search(DepartmentSearchCriteria criteria, Pageable pageable);

    DepartmentResponse getById(Long id);

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse update(Long id, DepartmentRequest request);

    /**
     * Hard-deletes when no employees are assigned; otherwise soft-deactivates.
     *
     * @param id department identifier
     * @return updated state when soft-deleted, or {@code null} when hard-deleted
     */
    DepartmentResponse delete(Long id);
}
