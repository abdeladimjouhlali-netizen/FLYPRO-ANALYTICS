package com.flypro.analytics.badge.repository;

import com.flypro.analytics.badge.dto.BadgeSearchCriteria;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.specification.BadgeSpecification;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import com.flypro.analytics.badgeassignment.repository.BadgeAssignmentRepository;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.test.FlyproDataJpaTest;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class BadgeRepositoryTest {

    @Autowired
    private BadgeRepository badgeRepository;
    @Autowired
    private BadgeAssignmentRepository badgeAssignmentRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private Department department;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        department = Department.builder().name("Operations").code("OPS").active(true).build();
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        department = departmentRepository.saveAndFlush(department);

        employee = Employee.builder()
                .employeeNumber("EMP-100")
                .firstName("Sara")
                .lastName("Benali")
                .email("sara@flypro.local")
                .phone("+212600000000")
                .position("Analyst")
                .status(EmployeeStatus.ACTIVE)
                .department(department)
                .build();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        employee = employeeRepository.saveAndFlush(employee);
    }

    @Test
    @DisplayName("persists badge and uniqueness lookups")
    void saveAndExists() {
        Badge badge = newBadge("BDG-100", BadgeStatus.PENDING_ACTIVATION, BadgeType.RFID);
        Badge saved = badgeRepository.saveAndFlush(badge);

        assertThat(saved.getId()).isNotNull();
        assertThat(badgeRepository.existsByBadgeNumberIgnoreCase("bdg-100")).isTrue();
        assertThat(badgeRepository.hasAnyAssignment(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("specification filters by status, type, department and keyword")
    void searchSpecification() {
        Badge active = newBadge("BDG-1", BadgeStatus.ACTIVE, BadgeType.RFID);
        Badge pending = newBadge("BDG-2", BadgeStatus.PENDING_ACTIVATION, BadgeType.NFC);
        active = badgeRepository.saveAndFlush(active);
        badgeRepository.saveAndFlush(pending);

        BadgeAssignment assignment = BadgeAssignment.builder()
                .badge(active)
                .employee(employee)
                .assignedAt(Instant.now())
                .active(true)
                .build();
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());
        badgeAssignmentRepository.saveAndFlush(assignment);

        BadgeSearchCriteria criteria = new BadgeSearchCriteria(
                null, null, BadgeStatus.ACTIVE, BadgeType.RFID,
                null, null, department.getId(), "sara"
        );

        Page<Badge> page = badgeRepository.findAll(
                BadgeSpecification.withCriteria(criteria),
                PageRequest.of(0, 10, Sort.by("badgeNumber"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getBadgeNumber()).isEqualTo("BDG-1");
    }

    @Test
    @DisplayName("find expired candidates")
    void findExpiredCandidates() {
        Badge overdue = newBadge("BDG-EXP", BadgeStatus.ACTIVE, BadgeType.QR_CODE);
        overdue.setExpirationDate(LocalDate.now().minusDays(2));
        badgeRepository.saveAndFlush(overdue);

        Badge valid = newBadge("BDG-OK", BadgeStatus.ACTIVE, BadgeType.RFID);
        badgeRepository.saveAndFlush(valid);

        assertThat(badgeRepository.findExpiredCandidates(LocalDate.now()))
                .extracting(Badge::getBadgeNumber)
                .containsExactly("BDG-EXP");
    }

    private Badge newBadge(String number, BadgeStatus status, BadgeType type) {
        Instant now = Instant.now();
        Badge badge = Badge.builder()
                .badgeNumber(number)
                .status(status)
                .type(type)
                .issueDate(LocalDate.now().minusMonths(1))
                .expirationDate(LocalDate.now().plusYears(1))
                .build();
        badge.setCreatedAt(now);
        badge.setUpdatedAt(now);
        return badge;
    }
}
