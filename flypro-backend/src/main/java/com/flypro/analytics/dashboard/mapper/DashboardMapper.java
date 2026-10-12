package com.flypro.analytics.dashboard.mapper;

import com.flypro.analytics.dashboard.dto.ChartDTO;
import com.flypro.analytics.dashboard.dto.ChartSeriesDTO;
import com.flypro.analytics.dashboard.dto.KpiDTO;
import com.flypro.analytics.dashboard.dto.NamedCountDTO;
import com.flypro.analytics.dashboard.dto.TrendDTO;
import com.flypro.analytics.dashboard.repository.LabelCountProjection;
import com.flypro.analytics.dashboard.repository.NameCountProjection;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MapStruct helpers for dashboard DTO assembly.
 */
@Mapper(componentModel = "spring")
public interface DashboardMapper {

    default KpiDTO toKpi(String key, String label, long value) {
        return new KpiDTO(key, label, value, "count");
    }

    default NamedCountDTO toNamedCount(NameCountProjection projection) {
        if (projection == null) {
            return null;
        }
        return new NamedCountDTO(
                projection.getId(),
                projection.getName(),
                projection.getCount() == null ? 0L : projection.getCount()
        );
    }

    default List<NamedCountDTO> toNamedCounts(List<NameCountProjection> projections) {
        if (projections == null) {
            return List.of();
        }
        return projections.stream().map(this::toNamedCount).toList();
    }

    default ChartDTO toDistributionChart(String key, String title, List<LabelCountProjection> projections) {
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        if (projections != null) {
            for (LabelCountProjection p : projections) {
                labels.add(p.getLabel());
                data.add(p.getCount() == null ? 0L : p.getCount());
            }
        }
        return new ChartDTO(key, title, labels, List.of(new ChartSeriesDTO("count", data)));
    }

    default ChartDTO toSingleSeriesChart(String key, String title, List<String> labels, List<Long> values) {
        return new ChartDTO(key, title, labels, List.of(new ChartSeriesDTO("count", values)));
    }

    default ChartDTO toMultiSeriesChart(String key, String title, List<String> labels, Map<String, List<Long>> seriesMap) {
        List<ChartSeriesDTO> series = seriesMap.entrySet().stream()
                .map(e -> new ChartSeriesDTO(e.getKey(), e.getValue()))
                .toList();
        return new ChartDTO(key, title, labels, series);
    }

    default TrendDTO toTrend(String key, String label, long current, long previous) {
        double change = previous == 0
                ? (current == 0 ? 0.0 : 100.0)
                : ((double) (current - previous) / previous) * 100.0;
        return new TrendDTO(key, label, current, previous, Math.round(change * 100.0) / 100.0);
    }
}
