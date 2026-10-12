package com.flypro.analytics.zone.dto;

/**
 * Search / filter criteria for zones.
 */
public record ZoneSearchCriteria(
        String name,
        String building,
        String floor,
        Boolean sensitive,
        String keyword
) {
}
