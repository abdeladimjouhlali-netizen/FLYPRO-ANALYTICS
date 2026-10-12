package com.flypro.analytics.department.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.department.dto.DepartmentRequest;
import com.flypro.analytics.department.dto.DepartmentResponse;
import com.flypro.analytics.department.dto.DepartmentSearchCriteria;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.department.exception.DepartmentNotFoundException;
import com.flypro.analytics.department.exception.DuplicateDepartmentCodeException;
import com.flypro.analytics.department.exception.DuplicateDepartmentNameException;
import com.flypro.analytics.department.mapper.DepartmentMapper;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.department.validator.DepartmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private DepartmentValidator departmentValidator;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department department;
    private DepartmentRequest request;
    private DepartmentResponse response;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .name("Human Resources")
                .code("HR")
                .description("HR dept")
                .active(true)
                .build();
        department.setId(1L);
        department.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        department.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        request = new DepartmentRequest("Human Resources", "hr", "HR dept");
        response = new DepartmentResponse(
                1L, "Human Resources", "HR", "HR dept", true,
                department.getCreatedAt(), department.getUpdatedAt()
        );

        when(departmentValidator.normalizeName(any())).thenAnswer(inv -> {
            String value = inv.getArgument(0);
            return value == null ? null : value.trim();
        });
        when(departmentValidator.normalizeCode(any())).thenAnswer(inv -> {
            String value = inv.getArgument(0);
            return value == null ? null : value.trim().toUpperCase();
        });
    }

    @Test
    @DisplayName("create persists a normalized department")
    void createSuccess() {
        when(departmentMapper.toEntity(any())).thenReturn(department);
        when(departmentRepository.save(any())).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        DepartmentResponse created = departmentService.create(request);

        assertThat(created.code()).isEqualTo("HR");
        verify(departmentValidator).validateCreate(any(DepartmentRequest.class));
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("create rejects duplicate code")
    void createDuplicateCode() {
        when(departmentValidator.normalizeCode("hr")).thenReturn("HR");
        when(departmentValidator.normalizeName("Human Resources")).thenReturn("Human Resources");
        org.mockito.Mockito.doThrow(new DuplicateDepartmentCodeException("HR"))
                .when(departmentValidator).validateCreate(any());

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(DuplicateDepartmentCodeException.class);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects duplicate name")
    void createDuplicateName() {
        org.mockito.Mockito.doThrow(new DuplicateDepartmentNameException("Human Resources"))
                .when(departmentValidator).validateCreate(any());

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(DuplicateDepartmentNameException.class);
    }

    @Test
    @DisplayName("getById throws when missing")
    void getByIdNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(99L))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    @DisplayName("update maps request onto existing entity")
    void updateSuccess() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        DepartmentResponse updated = departmentService.update(1L, request);

        assertThat(updated.id()).isEqualTo(1L);
        verify(departmentValidator).validateUpdate(eq(1L), any(DepartmentRequest.class));
        verify(departmentMapper).updateEntity(any(DepartmentRequest.class), eq(department));
    }

    @Test
    @DisplayName("delete hard-removes when no employees")
    void deleteHard() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.hasEmployees(1L)).thenReturn(false);

        DepartmentResponse result = departmentService.delete(1L);

        assertThat(result).isNull();
        verify(departmentRepository).delete(department);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete soft-deactivates when employees exist")
    void deleteSoft() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.hasEmployees(1L)).thenReturn(true);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(
                new DepartmentResponse(1L, "Human Resources", "HR", "HR dept", false,
                        department.getCreatedAt(), department.getUpdatedAt())
        );

        DepartmentResponse result = departmentService.delete(1L);

        assertThat(result.active()).isFalse();
        assertThat(department.isActive()).isFalse();
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    @Test
    @DisplayName("search returns paginated mapped results")
    void searchPaginated() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Department> page = new PageImpl<>(List.of(department), pageable, 1);
        when(departmentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        PageResponse<DepartmentResponse> result = departmentService.search(
                new DepartmentSearchCriteria("Human", "HR", true, null, null),
                pageable
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
    }
}
