package com.flypro.analytics.dashboard.repository;

/**
 * Projection for time-bucketed event counts (day / week / month / hour).
 */
public interface TimeBucketCountProjection {

    String getBucket();

    Long getCount();
}
