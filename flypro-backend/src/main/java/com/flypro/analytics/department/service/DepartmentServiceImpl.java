package com.flypro.analytics.department.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.config.CacheConfig;
import com.flypro.analytics.department.dto.DepartmentRequest;
import com.flypro.analytics.department.dto.DepartmentResponse;
import com.flypro.analytics.department.dto.DepartmentSearchCriteria;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.department.exception.DepartmentNotFoundException;
import com.flypro.analytics.department.mapper.DepartmentMapper;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.department.specification.DepartmentSpecification;
import com.flypro.analytics.department.validator.DepartmentValidator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link DepartmentService}.
 */
@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final DepartmentValidator departmentValidator;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 DepartmentMapper departmentMapper,
                                 DepartmentValidator departmentValidator) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
        this.departmentValidator = departmentValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> search(DepartmentSearchCriteria criteria, Pageable pageable) {
        Specification<Department> specification = DepartmentSpecification.withCriteria(criteria);
        Page<DepartmentResponse> page = departmentRepository.findAll(specification, pageable)
                .map(departmentMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REF_DATA, key = "'department:' + #id")
    public DepartmentResponse getById(Long id) {
        return departmentMapper.toResponse(findDepartment(id));
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.REF_DATA, allEntries = true)
    public DepartmentResponse create(DepartmentRequest request) {
        DepartmentRequest normalized = normalize(request);
        departmentValidator.validateCreate(normalized);

        Department entity = departmentMapper.toEntity(normalized);
        entity.setActive(true);
        Department saved = departmentRepository.save(entity);
        return departmentMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.REF_DATA, allEntries = true)
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findDepartment(id);
        DepartmentRequest normalized = normalize(request);
        departmentValidator.validateUpdate(id, normalized);

        departmentMapper.updateEntity(normalized, department);
        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.REF_DATA, allEntries = true)
    public DepartmentResponse delete(Long id) {
        Department department = findDepartment(id);

        if (departmentRepository.hasEmployees(id)) {
            department.setActive(false);
            Department deactivated = departmentRepository.save(department);
            return departmentMapper.toResponse(deactivated);
        }

        departmentRepository.delete(department);
        return null;
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
    }

    private DepartmentRequest normalize(DepartmentRequest request) {
        return new DepartmentRequest(
                departmentValidator.normalizeName(request.name()),
                departmentValidator.normalizeCode(request.code()),
                request.description() == null ? null : request.description().trim()
        );
    }
}
