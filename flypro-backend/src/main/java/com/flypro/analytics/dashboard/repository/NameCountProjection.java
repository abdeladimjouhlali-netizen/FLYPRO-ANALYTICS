package com.flypro.analytics.dashboard.repository;

/**
 * Projection for ranked / grouped name-count analytics.
 */
public interface NameCountProjection {

    Long getId();

    String getName();

    Long getCount();
}
