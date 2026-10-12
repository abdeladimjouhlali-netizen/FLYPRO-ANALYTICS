package com.flypro.analytics.badge.service;

import com.flypro.analytics.badge.dto.BadgeRenewRequestDTO;
import com.flypro.analytics.badge.dto.BadgeReplaceRequestDTO;
import com.flypro.analytics.badge.dto.BadgeRequestDTO;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.dto.BadgeSearchCriteria;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.exception.BadgeAlreadyAssignedException;
import com.flypro.analytics.badge.exception.BadgeAlreadyRevokedException;
import com.flypro.analytics.badge.exception.BadgeCannotBeDeletedException;
import com.flypro.analytics.badge.exception.BadgeExpiredException;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.badge.exception.DuplicateBadgeNumberException;
import com.flypro.analytics.badge.exception.EmployeeInactiveException;
import com.flypro.analytics.badge.mapper.BadgeMapper;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.badge.validator.BadgeValidator;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import com.flypro.analytics.badgeassignment.repository.BadgeAssignmentRepository;
import com.flypro.analytics.alert.engine.AlertRuleEngine;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.repository.EmployeeRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
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
class BadgeServiceImplTest {

    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private BadgeAssignmentRepository badgeAssignmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BadgeMapper badgeMapper;
    @Mock
    private BadgeValidator badgeValidator;
    @Mock
    private AlertRuleEngine alertRuleEngine;

    @InjectMocks
    private BadgeServiceImpl badgeService;

    private Badge badge;
    private Employee employee;
    private BadgeRequestDTO request;
    private BadgeResponseDTO response;

    @BeforeEach
    void setUp() {
        Department department = Department.builder().name("IT").code("IT").active(true).build();
        department.setId(1L);

        employee = Employee.builder()
                .employeeNumber("EMP-001")
                .firstName("Sara")
                .lastName("Benali")
                .email("sara@flypro.local")
                .position("Analyst")
                .status(EmployeeStatus.ACTIVE)
                .department(department)
                .build();
        employee.setId(10L);

        badge = Badge.builder()
                .badgeNumber("BDG-001")
                .type(BadgeType.RFID)
                .status(BadgeStatus.PENDING_ACTIVATION)
                .issueDate(LocalDate.of(2026, 1, 15))
                .expirationDate(LocalDate.of(2027, 1, 15))
                .build();
        badge.setId(1L);

        request = new BadgeRequestDTO(
                "BDG-001", BadgeType.RFID, BadgeStatus.PENDING_ACTIVATION,
                LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15)
        );
        response = new BadgeResponseDTO(
                1L, "BDG-001", BadgeType.RFID, BadgeStatus.PENDING_ACTIVATION,
                LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15), null, null, null
        );

        when(badgeValidator.normalize(any())).thenAnswer(inv -> inv.getArgument(0));
        when(badgeRepository.findExpiredCandidates(any())).thenReturn(List.of());
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(any())).thenReturn(Optional.empty());
        when(badgeMapper.toResponse(any(Badge.class), any())).thenReturn(response);
        when(badgeMapper.toResponse(any(Badge.class), eq(null))).thenReturn(response);
    }

    @Test
    @DisplayName("create badge successfully")
    void createSuccess() {
        when(badgeMapper.toEntity(request)).thenReturn(badge);
        when(badgeRepository.save(any())).thenReturn(badge);

        BadgeResponseDTO created = badgeService.create(request);

        assertThat(created.badgeNumber()).isEqualTo("BDG-001");
        verify(badgeValidator).validateCreate(request);
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    @DisplayName("create rejects duplicate badge number")
    void createDuplicate() {
        doThrow(new DuplicateBadgeNumberException("BDG-001"))
                .when(badgeValidator).validateCreate(any());

        assertThatThrownBy(() -> badgeService.create(request))
                .isInstanceOf(DuplicateBadgeNumberException.class);
        verify(badgeRepository, never()).save(any());
    }

    @Test
    @DisplayName("assign badge to active employee")
    void assignSuccess() {
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(badgeAssignmentRepository.existsByBadgeIdAndActiveTrue(1L)).thenReturn(false);
        when(badgeAssignmentRepository.existsByEmployeeIdAndActiveTrue(10L)).thenReturn(false);
        when(badgeRepository.save(badge)).thenReturn(badge);
        when(badgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BadgeResponseDTO activeResponse = new BadgeResponseDTO(
                1L, "BDG-001", BadgeType.RFID, BadgeStatus.ACTIVE,
                badge.getIssueDate(), badge.getExpirationDate(), null, null, null
        );
        when(badgeMapper.toResponse(any(Badge.class), any())).thenReturn(activeResponse);

        BadgeResponseDTO result = badgeService.assign(1L, 10L);

        assertThat(result.status()).isEqualTo(BadgeStatus.ACTIVE);
        assertThat(badge.getStatus()).isEqualTo(BadgeStatus.ACTIVE);
        verify(badgeAssignmentRepository).save(any(BadgeAssignment.class));
    }

    @Test
    @DisplayName("assign rejects inactive employee")
    void assignInactiveEmployee() {
        employee.setStatus(EmployeeStatus.INACTIVE);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> badgeService.assign(1L, 10L))
                .isInstanceOf(EmployeeInactiveException.class);
    }

    @Test
    @DisplayName("assign rejects already assigned badge")
    void assignAlreadyAssigned() {
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(badgeAssignmentRepository.existsByBadgeIdAndActiveTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> badgeService.assign(1L, 10L))
                .isInstanceOf(BadgeAlreadyAssignedException.class);
    }

    @Test
    @DisplayName("renew updates expiration")
    void renewSuccess() {
        badge.setStatus(BadgeStatus.EXPIRED);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeRepository.save(badge)).thenReturn(badge);

        BadgeRenewRequestDTO renew = new BadgeRenewRequestDTO(LocalDate.of(2028, 1, 15), LocalDate.of(2026, 7, 24));
        badgeService.renew(1L, renew);

        assertThat(badge.getExpirationDate()).isEqualTo(LocalDate.of(2028, 1, 15));
        assertThat(badge.getStatus()).isEqualTo(BadgeStatus.PENDING_ACTIVATION);
        verify(badgeValidator).validateDates(LocalDate.of(2026, 7, 24), LocalDate.of(2028, 1, 15));
    }

    @Test
    @DisplayName("replace transfers assignment to new badge")
    void replaceSuccess() {
        Badge replacement = Badge.builder()
                .badgeNumber("BDG-002")
                .type(BadgeType.NFC)
                .status(BadgeStatus.PENDING_ACTIVATION)
                .issueDate(LocalDate.of(2026, 1, 15))
                .expirationDate(LocalDate.of(2027, 1, 15))
                .build();
        replacement.setId(2L);

        BadgeAssignment active = BadgeAssignment.builder()
                .badge(badge)
                .employee(employee)
                .assignedAt(java.time.Instant.now())
                .active(true)
                .build();
        active.setId(100L);

        badge.setStatus(BadgeStatus.ACTIVE);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeRepository.findById(2L)).thenReturn(Optional.of(replacement));
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(1L)).thenReturn(Optional.of(active));
        when(badgeAssignmentRepository.existsByBadgeIdAndActiveTrue(2L)).thenReturn(false);
        when(badgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(badgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BadgeResponseDTO replaced = new BadgeResponseDTO(
                2L, "BDG-002", BadgeType.NFC, BadgeStatus.ACTIVE,
                replacement.getIssueDate(), replacement.getExpirationDate(), null, null, null
        );
        when(badgeMapper.toResponse(eq(replacement), any())).thenReturn(replaced);

        BadgeResponseDTO result = badgeService.replace(1L, new BadgeReplaceRequestDTO(2L));

        assertThat(result.id()).isEqualTo(2L);
        assertThat(active.isActive()).isFalse();
        assertThat(replacement.getStatus()).isEqualTo(BadgeStatus.ACTIVE);
    }

    @Test
    @DisplayName("suspend sets SUSPENDED")
    void suspendSuccess() {
        badge.setStatus(BadgeStatus.ACTIVE);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeRepository.save(badge)).thenReturn(badge);

        badgeService.suspend(1L);

        assertThat(badge.getStatus()).isEqualTo(BadgeStatus.SUSPENDED);
    }

    @Test
    @DisplayName("revoke closes assignment and sets REVOKED")
    void revokeSuccess() {
        BadgeAssignment active = BadgeAssignment.builder()
                .badge(badge).employee(employee).assignedAt(java.time.Instant.now()).active(true).build();
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(1L)).thenReturn(Optional.of(active));
        when(badgeRepository.save(badge)).thenReturn(badge);

        badgeService.revoke(1L);

        assertThat(badge.getStatus()).isEqualTo(BadgeStatus.REVOKED);
        assertThat(active.isActive()).isFalse();
    }

    @Test
    @DisplayName("report lost sets LOST")
    void reportLost() {
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeRepository.save(badge)).thenReturn(badge);

        badgeService.reportLost(1L);

        assertThat(badge.getStatus()).isEqualTo(BadgeStatus.LOST);
    }

    @Test
    @DisplayName("delete blocked when assignment history exists")
    void deleteWithHistory() {
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeRepository.hasAnyAssignment(1L)).thenReturn(true);

        assertThatThrownBy(() -> badgeService.delete(1L))
                .isInstanceOf(BadgeCannotBeDeletedException.class);
        verify(badgeRepository, never()).delete(any(Badge.class));
    }

    @Test
    @DisplayName("activate rejects expired badge")
    void activateExpired() {
        badge.setStatus(BadgeStatus.EXPIRED);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));

        assertThatThrownBy(() -> badgeService.activate(1L))
                .isInstanceOf(BadgeExpiredException.class);
    }

    @Test
    @DisplayName("activate rejects revoked badge")
    void activateRevoked() {
        badge.setStatus(BadgeStatus.REVOKED);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));

        assertThatThrownBy(() -> badgeService.activate(1L))
                .isInstanceOf(BadgeAlreadyRevokedException.class);
    }

    @Test
    @DisplayName("getById not found")
    void getNotFound() {
        when(badgeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.getById(99L))
                .isInstanceOf(BadgeNotFoundException.class);
    }

    @Test
    @DisplayName("search with pagination")
    void searchPagination() {
        when(badgeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(badge), PageRequest.of(0, 10), 1));

        PageResponse<BadgeResponseDTO> page = badgeService.search(
                new BadgeSearchCriteria(null, null, null, null, null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertThat(page.totalElements()).isEqualTo(1);
        verify(badgeRepository).findExpiredCandidates(any());
    }

    @Test
    @DisplayName("auto-expire overdue badge on get")
    void autoExpire() {
        badge.setStatus(BadgeStatus.ACTIVE);
        badge.setExpirationDate(LocalDate.now().minusDays(1));
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeRepository.save(badge)).thenReturn(badge);

        ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
        badgeService.getById(1L);

        verify(badgeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BadgeStatus.EXPIRED);
    }
}
