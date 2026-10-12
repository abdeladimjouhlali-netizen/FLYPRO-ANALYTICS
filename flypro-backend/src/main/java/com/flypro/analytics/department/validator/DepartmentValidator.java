package com.flypro.analytics.department.validator;

import com.flypro.analytics.department.dto.DepartmentRequest;
import com.flypro.analytics.department.exception.DuplicateDepartmentCodeException;
import com.flypro.analytics.department.exception.DuplicateDepartmentNameException;
import com.flypro.analytics.department.repository.DepartmentRepository;
import org.springframework.stereotype.Component;

/**
 * Business uniqueness rules for departments.
 */
@Component
public class DepartmentValidator {

    private final DepartmentRepository departmentRepository;

    public DepartmentValidator(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /**
     * Validates uniqueness rules for create operations.
     *
     * @param request incoming request (code already expected uppercase)
     */
    public void validateCreate(DepartmentRequest request) {
        String code = normalizeCode(request.code());
        String name = normalizeName(request.name());

        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateDepartmentCodeException(code);
        }
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateDepartmentNameException(name);
        }
    }

    /**
     * Validates uniqueness rules for update operations.
     *
     * @param id      department being updated
     * @param request incoming request
     */
    public void validateUpdate(Long id, DepartmentRequest request) {
        String code = normalizeCode(request.code());
        String name = normalizeName(request.name());

        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateDepartmentCodeException(code);
        }
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateDepartmentNameException(name);
        }
    }

    public String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    public String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
