package com.flypro.analytics.dashboard.repository;

/**
 * Projection for label/status distribution charts.
 */
public interface LabelCountProjection {

    String getLabel();

    Long getCount();
}
