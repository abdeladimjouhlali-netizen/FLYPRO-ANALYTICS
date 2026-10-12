package com.flypro.analytics.employee.repository;

import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.test.FlyproDataJpaTest;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.employee.dto.EmployeeSearchCriteria;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.specification.EmployeeSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department department;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        department = Department.builder().name("Operations").code("OPS").active(true).build();
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        department = departmentRepository.saveAndFlush(department);
    }

    @Test
    @DisplayName("persists employee and uniqueness lookups")
    void saveAndExists() {
        Employee employee = newEmployee("EMP-100", "Sara", "Benali", "sara@flypro.local", EmployeeStatus.ACTIVE);
        Employee saved = employeeRepository.saveAndFlush(employee);

        assertThat(saved.getId()).isNotNull();
        assertThat(employeeRepository.existsByEmployeeNumberIgnoreCase("emp-100")).isTrue();
        assertThat(employeeRepository.existsByEmailIgnoreCase("SARA@FLYPRO.LOCAL")).isTrue();
        assertThat(employeeRepository.hasActiveBadgeAssignments(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("specification filters by keyword and status with pagination")
    void searchSpecification() {
        employeeRepository.save(newEmployee("EMP-1", "Sara", "Benali", "sara@flypro.local", EmployeeStatus.ACTIVE));
        employeeRepository.save(newEmployee("EMP-2", "Omar", "Alaoui", "omar@flypro.local", EmployeeStatus.INACTIVE));
        employeeRepository.flush();

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                null, null, null, department.getId(), EmployeeStatus.ACTIVE, null, null, "sara"
        );

        Page<Employee> page = employeeRepository.findAll(
                EmployeeSpecification.withCriteria(criteria),
                PageRequest.of(0, 10, Sort.by("lastName"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getEmployeeNumber()).isEqualTo("EMP-1");
    }

    private Employee newEmployee(String number, String first, String last, String email, EmployeeStatus status) {
        Instant now = Instant.now();
        Employee employee = Employee.builder()
                .employeeNumber(number)
                .firstName(first)
                .lastName(last)
                .email(email)
                .phone("+212600000000")
                .position("Analyst")
                .status(status)
                .department(department)
                .build();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        return employee;
    }
}
