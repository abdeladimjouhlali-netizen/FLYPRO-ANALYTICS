package com.flypro.analytics.employee.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.employee.dto.EmployeeRequestDTO;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.employee.dto.EmployeeSearchCriteria;
import org.springframework.data.domain.Pageable;

/**
 * Application service contract for the Employee module.
 */
public interface EmployeeService {

    PageResponse<EmployeeResponseDTO> search(EmployeeSearchCriteria criteria, Pageable pageable);

    EmployeeResponseDTO getById(Long id);

    EmployeeResponseDTO create(EmployeeRequestDTO request);

    EmployeeResponseDTO update(Long id, EmployeeRequestDTO request);

    EmployeeResponseDTO deactivate(Long id);

    EmployeeResponseDTO activate(Long id);

    EmployeeResponseDTO archive(Long id);

    void delete(Long id);
}
