package com.flypro.analytics.notification.dto;

import com.flypro.analytics.notification.enums.NotificationChannelType;
import com.flypro.analytics.notification.enums.NotificationStatus;
import com.flypro.analytics.notification.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationChannelType channel,
        NotificationType type,
        String recipient,
        String subject,
        String body,
        NotificationStatus status,
        String severity,
        String relatedEntity,
        String relatedId,
        Instant sentAt,
        Instant readAt,
        Instant createdAt,
        String errorMessage
) {
}
