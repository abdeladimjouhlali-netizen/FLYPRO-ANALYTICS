package com.flypro.analytics.importbatch.parser;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Normalizes heterogeneous column headers to canonical import field names.
 */
public final class ImportColumnNormalizer {

    public static final String EVENT_TIMESTAMP = "eventTimestamp";
    public static final String BADGE_NUMBER = "badgeNumber";
    public static final String ACCESS_POINT_CODE = "accessPointCode";
    public static final String DIRECTION = "direction";
    public static final String ACCESS_RESULT = "result";
    public static final String REFUSAL_REASON = "refusalReason";

    private ImportColumnNormalizer() {
    }

    public static Optional<String> toCanonical(String header) {
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        String key = header.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (key) {
            case "eventtimestamp", "event_timestamp", "timestamp", "event_time", "datetime", "date_time" ->
                    Optional.of(EVENT_TIMESTAMP);
            case "badgenumber", "badge_number", "badge", "badge_no", "badgeid", "badge_id" ->
                    Optional.of(BADGE_NUMBER);
            case "accesspointcode", "access_point_code", "accesspoint", "access_point",
                 "accesspointid", "access_point_id", "reader", "reader_code", "ap_code" ->
                    Optional.of(ACCESS_POINT_CODE);
            case "direction", "access_direction" ->
                    Optional.of(DIRECTION);
            case "result", "accessresult", "access_result", "access_status", "status" ->
                    Optional.of(ACCESS_RESULT);
            case "refusalreason", "refusal_reason", "reason", "denial_reason" ->
                    Optional.of(REFUSAL_REASON);
            default -> Optional.empty();
        };
    }

    public static Map<String, String> mapRow(Map<String, String> rawByHeader) {
        Map<String, String> canonical = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawByHeader.entrySet()) {
            toCanonical(entry.getKey()).ifPresent(field ->
                    canonical.putIfAbsent(field, blankToNull(entry.getValue()))
            );
        }
        return canonical;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
