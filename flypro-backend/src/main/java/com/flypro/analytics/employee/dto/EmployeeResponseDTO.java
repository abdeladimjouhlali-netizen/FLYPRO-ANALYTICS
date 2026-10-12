package com.flypro.analytics.employee.dto;

import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.department.dto.DepartmentSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Full employee representation returned by the API.
 */
@Schema(description = "Employee details")
public record EmployeeResponseDTO(
        Long id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String position,
        EmployeeStatus status,
        DepartmentSummaryResponse department,
        Long accessProfileId,
        Instant createdAt,
        Instant updatedAt,
        /** Présent uniquement à la création : identifiant portail généré */
        String portalUsername,
        /** Présent uniquement à la création : mot de passe temporaire (montré une fois) */
        String temporaryPassword
) {
}
