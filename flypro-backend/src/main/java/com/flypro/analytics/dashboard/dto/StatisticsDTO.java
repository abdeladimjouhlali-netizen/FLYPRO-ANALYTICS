package com.flypro.analytics.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Aggregated analytics statistics for the dashboard.
 */
@Schema(description = "Dashboard statistics")
public record StatisticsDTO(

        List<NamedCountDTO> topAccessPoints,

        List<NamedCountDTO> topZones,

        List<NamedCountDTO> mostActiveEmployees,

        List<NamedCountDTO> employeesWithDeniedAccesses,

        List<NamedCountDTO> badgesExpiringSoon,

        List<NamedCountDTO> departmentsWithMostAccesses,

        List<NamedCountDTO> mostFrequentAlertTypes
) {
}
