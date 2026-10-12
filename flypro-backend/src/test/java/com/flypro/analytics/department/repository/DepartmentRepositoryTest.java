package com.flypro.analytics.department.repository;

import com.flypro.analytics.common.test.FlyproDataJpaTest;
import com.flypro.analytics.department.dto.DepartmentSearchCriteria;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.department.specification.DepartmentSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("persists department and enforces unique code lookup")
    void saveAndExistsByCode() {
        Department department = newDepartment("Finance", "FIN", true);

        Department saved = departmentRepository.saveAndFlush(department);

        assertThat(saved.getId()).isNotNull();
        assertThat(departmentRepository.existsByCodeIgnoreCase("fin")).isTrue();
        assertThat(departmentRepository.existsByNameIgnoreCase("finance")).isTrue();
        assertThat(departmentRepository.hasEmployees(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("specification filters by name and supports pagination/sorting")
    void searchWithSpecification() {
        departmentRepository.save(newDepartment("Alpha", "ALP", true));
        departmentRepository.save(newDepartment("Beta Ops", "BETA", true));
        departmentRepository.save(newDepartment("Gamma", "GAM", false));
        departmentRepository.flush();

        DepartmentSearchCriteria criteria = new DepartmentSearchCriteria("Beta", null, true, null, null);
        Page<Department> page = departmentRepository.findAll(
                DepartmentSpecification.withCriteria(criteria),
                PageRequest.of(0, 10, Sort.by("name").ascending())
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getCode()).isEqualTo("BETA");
    }

    @Test
    @DisplayName("existsByCodeIgnoreCaseAndIdNot ignores current department")
    void existsExcludingSelf() {
        Department first = departmentRepository.save(newDepartment("Legal", "LEG", true));
        departmentRepository.save(newDepartment("Marketing", "MKT", true));
        departmentRepository.flush();

        assertThat(departmentRepository.existsByCodeIgnoreCaseAndIdNot("LEG", first.getId())).isFalse();
        assertThat(departmentRepository.existsByCodeIgnoreCaseAndIdNot("MKT", first.getId())).isTrue();
    }

    private Department newDepartment(String name, String code, boolean active) {
        Instant now = Instant.now();
        Department department = Department.builder()
                .name(name)
                .code(code)
                .description(name + " description")
                .active(active)
                .build();
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        return department;
    }
}
