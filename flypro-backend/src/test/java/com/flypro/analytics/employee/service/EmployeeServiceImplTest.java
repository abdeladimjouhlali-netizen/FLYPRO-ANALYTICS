package com.flypro.analytics.employee.service;

import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.department.exception.DepartmentNotFoundException;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.employee.dto.EmployeeRequestDTO;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.employee.dto.EmployeeSearchCriteria;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.exception.DuplicateEmployeeEmailException;
import com.flypro.analytics.employee.exception.DuplicateEmployeeNumberException;
import com.flypro.analytics.employee.exception.EmployeeCannotBeDeletedException;
import com.flypro.analytics.employee.exception.EmployeeNotFoundException;
import com.flypro.analytics.employee.mapper.EmployeeMapper;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import com.flypro.analytics.employee.validator.EmployeeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private com.flypro.analytics.accessprofile.repository.AccessProfileRepository accessProfileRepository;
    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private EmployeeValidator employeeValidator;
    @Mock
    private EmployeePortalAccountService portalAccountService;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Department department;
    private Employee employee;
    private EmployeeRequestDTO request;
    private EmployeeResponseDTO response;

    @BeforeEach
    void setUp() {
        department = Department.builder().name("IT").code("IT").active(true).build();
        department.setId(1L);

        employee = Employee.builder()
                .employeeNumber("EMP-001")
                .firstName("Sara")
                .lastName("Benali")
                .email("sara.benali@flypro.local")
                .phone("+212612345678")
                .position("Analyst")
                .status(EmployeeStatus.ACTIVE)
                .department(department)
                .build();
        employee.setId(10L);

        request = new EmployeeRequestDTO(
                "EMP-001", "Sara", "Benali", "sara.benali@flypro.local",
                "+212612345678", "Analyst", EmployeeStatus.ACTIVE, 1L, null
        );
        response = new EmployeeResponseDTO(
                10L, "EMP-001", "Sara", "Benali", "sara.benali@flypro.local",
                "+212612345678", "Analyst", EmployeeStatus.ACTIVE, null, null, null, null, null, null
        );

        when(employeeValidator.normalize(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("create employee successfully")
    void createSuccess() {
        when(employeeMapper.toEntity(request)).thenReturn(employee);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any())).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);
        when(portalAccountService.createPortalAccount(any())).thenReturn(
                new EmployeePortalAccountService.PortalCredentials("emp-001", "Employee@123")
        );

        EmployeeResponseDTO created = employeeService.create(request);

        assertThat(created.employeeNumber()).isEqualTo("EMP-001");
        assertThat(created.portalUsername()).isEqualTo("emp-001");
        assertThat(created.temporaryPassword()).isEqualTo("Employee@123");
        verify(employeeValidator).validateCreate(request);
        verify(employeeRepository).save(any(Employee.class));
        verify(portalAccountService).createPortalAccount(any(Employee.class));
    }

    @Test
    @DisplayName("create rejects duplicate employee number")
    void createDuplicateNumber() {
        doThrow(new DuplicateEmployeeNumberException("EMP-001"))
                .when(employeeValidator).validateCreate(any());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateEmployeeNumberException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects duplicate email")
    void createDuplicateEmail() {
        doThrow(new DuplicateEmployeeEmailException("sara.benali@flypro.local"))
                .when(employeeValidator).validateCreate(any());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateEmployeeEmailException.class);
    }

    @Test
    @DisplayName("create rejects missing department")
    void createDepartmentNotFound() {
        doThrow(new DepartmentNotFoundException(99L))
                .when(employeeValidator).validateCreate(any());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    @DisplayName("update employee")
    void updateSuccess() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        assertThat(employeeService.update(10L, request).id()).isEqualTo(10L);
        verify(employeeMapper).updateEntity(eq(request), eq(employee));
    }

    @Test
    @DisplayName("getById not found")
    void getByIdNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> employeeService.getById(99L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    @DisplayName("deactivate sets INACTIVE")
    void deactivate() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        employeeService.deactivate(10L);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
    }

    @Test
    @DisplayName("activate sets ACTIVE")
    void activate() {
        employee.setStatus(EmployeeStatus.INACTIVE);
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        employeeService.activate(10L);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("archive sets ARCHIVED")
    void archive() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        employeeService.archive(10L);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ARCHIVED);
    }

    @Test
    @DisplayName("delete requires archived status")
    void deleteRequiresArchived() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.delete(10L))
                .isInstanceOf(EmployeeCannotBeDeletedException.class);
        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    @DisplayName("delete archived employee without badges")
    void deleteSuccess() {
        employee.setStatus(EmployeeStatus.ARCHIVED);
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.hasActiveBadgeAssignments(10L)).thenReturn(false);

        employeeService.delete(10L);
        verify(employeeRepository).delete(employee);
    }

    @Test
    @DisplayName("search returns page")
    void search() {
        Pageable pageable = PageRequest.of(0, 10);
        when(employeeRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        PageResponse<EmployeeResponseDTO> page = employeeService.search(
                new EmployeeSearchCriteria(null, "Sara", null, null, null, null, null, null),
                pageable
        );

        assertThat(page.totalElements()).isEqualTo(1);
    }
}
