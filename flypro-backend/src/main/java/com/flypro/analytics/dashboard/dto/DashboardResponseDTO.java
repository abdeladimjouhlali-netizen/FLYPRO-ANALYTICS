package com.flypro.analytics.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Optional aggregate dashboard payload (KPIs + charts snapshot).
 */
@Schema(description = "Dashboard aggregate response")
public record DashboardResponseDTO(

        List<KpiDTO> kpis,

        List<ChartDTO> charts,

        List<TrendDTO> trends
) {
}
