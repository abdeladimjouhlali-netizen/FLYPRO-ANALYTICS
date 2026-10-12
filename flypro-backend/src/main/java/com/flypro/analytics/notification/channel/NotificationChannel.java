package com.flypro.analytics.notification.channel;

import com.flypro.analytics.notification.enums.NotificationChannelType;

/**
 * Pluggable notification channel (EMAIL today; SMS/Teams/Slack/WebSocket later).
 */
public interface NotificationChannel {

    NotificationChannelType type();

    boolean supports(NotificationChannelType channelType);

    void send(String recipient, String subject, String htmlOrTextBody);
}
