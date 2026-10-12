package com.flypro.analytics.dashboard.service;

import com.flypro.analytics.dashboard.dto.ChartDTO;
import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;
import com.flypro.analytics.dashboard.dto.KpiDTO;
import com.flypro.analytics.dashboard.dto.NamedCountDTO;
import com.flypro.analytics.dashboard.dto.StatisticsDTO;
import com.flypro.analytics.dashboard.dto.TrendDTO;

import java.util.List;

/**
 * Read-only analytics service for the dashboard.
 */
public interface DashboardService {

    List<KpiDTO> getKpis(DashboardFilterCriteria filter);

    List<ChartDTO> getCharts(DashboardFilterCriteria filter);

    StatisticsDTO getStatistics(DashboardFilterCriteria filter);

    List<TrendDTO> getTrends(DashboardFilterCriteria filter);

    List<NamedCountDTO> getTopAccessPoints(DashboardFilterCriteria filter, int limit);

    List<NamedCountDTO> getTopZones(DashboardFilterCriteria filter, int limit);

    List<NamedCountDTO> getTopEmployees(DashboardFilterCriteria filter, int limit);

    List<NamedCountDTO> getPeakHours(DashboardFilterCriteria filter);
}
