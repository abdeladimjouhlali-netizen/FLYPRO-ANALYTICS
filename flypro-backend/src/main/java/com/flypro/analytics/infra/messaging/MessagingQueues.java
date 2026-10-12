package com.flypro.analytics.infra.messaging;

/**
 * Well-known RabbitMQ queue names used by the async job pipeline.
 */
public final class MessagingQueues {

    public static final String IMPORT_JOBS = "flypro.import-jobs";
    public static final String AI_PREDICTIONS = "flypro.ai-predictions";
    public static final String ALERTS = "flypro.alerts";
    public static final String NOTIFICATIONS = "flypro.notifications";
    public static final String DASHBOARD_REFRESH = "flypro.dashboard-refresh";
    public static final String ACCESS_EVENTS = "flypro.access-events";
    public static final String AUDIT_LOGS = "flypro.audit-logs";
    public static final String DLQ = "flypro.dlq";

    private MessagingQueues() {
    }
}
