package com.flypro.analytics.employee.service;

import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.accessprofile.exception.AccessProfileNotFoundException;
import com.flypro.analytics.accessprofile.repository.AccessProfileRepository;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.department.exception.DepartmentNotFoundException;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.employee.dto.EmployeeRequestDTO;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.employee.dto.EmployeeSearchCriteria;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.exception.EmployeeCannotBeDeletedException;
import com.flypro.analytics.employee.exception.EmployeeNotFoundException;
import com.flypro.analytics.employee.mapper.EmployeeMapper;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import com.flypro.analytics.employee.specification.EmployeeSpecification;
import com.flypro.analytics.employee.validator.EmployeeValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link EmployeeService}.
 */
@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AccessProfileRepository accessProfileRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeValidator employeeValidator;
    private final EmployeePortalAccountService portalAccountService;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               DepartmentRepository departmentRepository,
                               AccessProfileRepository accessProfileRepository,
                               EmployeeMapper employeeMapper,
                               EmployeeValidator employeeValidator,
                               EmployeePortalAccountService portalAccountService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.accessProfileRepository = accessProfileRepository;
        this.employeeMapper = employeeMapper;
        this.employeeValidator = employeeValidator;
        this.portalAccountService = portalAccountService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponseDTO> search(EmployeeSearchCriteria criteria, Pageable pageable) {
        Specification<Employee> specification = EmployeeSpecification.withCriteria(criteria);
        Page<EmployeeResponseDTO> page = employeeRepository.findAll(specification, pageable)
                .map(employeeMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getById(Long id) {
        return employeeMapper.toResponse(findEmployee(id));
    }

    @Override
    public EmployeeResponseDTO create(EmployeeRequestDTO request) {
        EmployeeRequestDTO normalized = employeeValidator.normalize(request);
        employeeValidator.validateCreate(normalized);

        Employee employee = employeeMapper.toEntity(normalized);
        employee.setDepartment(resolveDepartment(normalized.departmentId()));
        employee.setAccessProfile(resolveAccessProfile(normalized.accessProfileId()));

        Employee saved = employeeRepository.save(employee);
        var credentials = portalAccountService.createPortalAccount(saved);
        EmployeeResponseDTO base = employeeMapper.toResponse(saved);
        return withPortalCredentials(base, credentials.username(), credentials.temporaryPassword());
    }

    @Override
    public EmployeeResponseDTO update(Long id, EmployeeRequestDTO request) {
        Employee employee = findEmployee(id);
        EmployeeRequestDTO normalized = employeeValidator.normalize(request);
        employeeValidator.validateUpdate(id, normalized);

        employeeMapper.updateEntity(normalized, employee);
        employee.setDepartment(resolveDepartment(normalized.departmentId()));
        employee.setAccessProfile(resolveAccessProfile(normalized.accessProfileId()));

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponseDTO deactivate(Long id) {
        Employee employee = findEmployee(id);
        employee.setStatus(EmployeeStatus.INACTIVE);
        Employee saved = employeeRepository.save(employee);
        portalAccountService.syncEnabledFlag(id, false);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponseDTO activate(Long id) {
        Employee employee = findEmployee(id);
        employee.setStatus(EmployeeStatus.ACTIVE);
        Employee saved = employeeRepository.save(employee);
        portalAccountService.syncEnabledFlag(id, true);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponseDTO archive(Long id) {
        Employee employee = findEmployee(id);
        employee.setStatus(EmployeeStatus.ARCHIVED);
        Employee saved = employeeRepository.save(employee);
        portalAccountService.syncEnabledFlag(id, false);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Employee employee = findEmployee(id);

        if (employee.getStatus() != EmployeeStatus.ARCHIVED) {
            throw new EmployeeCannotBeDeletedException(
                    "Employee must be archived before permanent deletion"
            );
        }
        if (employeeRepository.hasActiveBadgeAssignments(id)) {
            throw new EmployeeCannotBeDeletedException(
                    "Employee has active badge assignments and cannot be deleted"
            );
        }

        employeeRepository.delete(employee);
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Department resolveDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId));
    }

    private AccessProfile resolveAccessProfile(Long accessProfileId) {
        if (accessProfileId == null) {
            return null;
        }
        return accessProfileRepository.findById(accessProfileId)
                .orElseThrow(() -> new AccessProfileNotFoundException(accessProfileId));
    }

    private static EmployeeResponseDTO withPortalCredentials(
            EmployeeResponseDTO base,
            String portalUsername,
            String temporaryPassword
    ) {
        return new EmployeeResponseDTO(
                base.id(),
                base.employeeNumber(),
                base.firstName(),
                base.lastName(),
                base.email(),
                base.phone(),
                base.position(),
                base.status(),
                base.department(),
                base.accessProfileId(),
                base.createdAt(),
                base.updatedAt(),
                portalUsername,
                temporaryPassword
        );
    }
}
