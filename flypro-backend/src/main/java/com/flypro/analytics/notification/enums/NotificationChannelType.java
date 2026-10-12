package com.flypro.analytics.notification.enums;

/**
 * Notification delivery channel. Extensible for SMS / Teams / Slack / WebSocket.
 */
public enum NotificationChannelType {
    EMAIL,
    SYSTEM,
    SMS,
    MICROSOFT_TEAMS,
    SLACK,
    WEBSOCKET
}
