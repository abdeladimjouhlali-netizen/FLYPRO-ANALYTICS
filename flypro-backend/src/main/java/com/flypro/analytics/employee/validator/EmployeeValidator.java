package com.flypro.analytics.employee.validator;

import com.flypro.analytics.department.exception.DepartmentNotFoundException;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.employee.dto.EmployeeRequestDTO;
import com.flypro.analytics.employee.exception.DuplicateEmployeeEmailException;
import com.flypro.analytics.employee.exception.DuplicateEmployeeNumberException;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Business validation rules for employees (uniqueness + department existence).
 */
@Component
public class EmployeeValidator {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeValidator(EmployeeRepository employeeRepository,
                             DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public void validateCreate(EmployeeRequestDTO request) {
        validateDepartmentExists(request.departmentId());
        if (employeeRepository.existsByEmployeeNumberIgnoreCase(request.employeeNumber())) {
            throw new DuplicateEmployeeNumberException(request.employeeNumber());
        }
        if (employeeRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmployeeEmailException(request.email());
        }
    }

    public void validateUpdate(Long id, EmployeeRequestDTO request) {
        validateDepartmentExists(request.departmentId());
        if (employeeRepository.existsByEmployeeNumberIgnoreCaseAndIdNot(request.employeeNumber(), id)) {
            throw new DuplicateEmployeeNumberException(request.employeeNumber());
        }
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateEmployeeEmailException(request.email());
        }
    }

    public void validateDepartmentExists(Long departmentId) {
        if (departmentId == null || !departmentRepository.existsById(departmentId)) {
            throw new DepartmentNotFoundException(departmentId == null ? -1L : departmentId);
        }
    }

    public EmployeeRequestDTO normalize(EmployeeRequestDTO request) {
        return new EmployeeRequestDTO(
                trimToNull(request.employeeNumber()),
                trimToNull(request.firstName()),
                trimToNull(request.lastName()),
                trimToNull(request.email()) == null ? null : request.email().trim().toLowerCase(),
                trimToNull(request.phone()),
                trimToNull(request.position()),
                request.status(),
                request.departmentId(),
                request.accessProfileId()
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return value == null ? null : value.trim();
        }
        return value.trim();
    }
}
