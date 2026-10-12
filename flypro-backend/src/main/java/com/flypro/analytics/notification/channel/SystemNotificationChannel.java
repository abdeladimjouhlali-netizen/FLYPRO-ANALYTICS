package com.flypro.analytics.notification.channel;

import com.flypro.analytics.notification.enums.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-app / system notification channel (persisted only; no external push).
 */
@Component
public class SystemNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SystemNotificationChannel.class);

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.SYSTEM;
    }

    @Override
    public boolean supports(NotificationChannelType channelType) {
        return channelType == NotificationChannelType.SYSTEM;
    }

    @Override
    public void send(String recipient, String subject, String htmlOrTextBody) {
        log.debug("System notification queued for recipient={} subject={}", recipient, subject);
    }
}
