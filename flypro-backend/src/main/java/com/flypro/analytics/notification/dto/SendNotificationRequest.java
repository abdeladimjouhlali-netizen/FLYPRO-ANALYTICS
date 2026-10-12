package com.flypro.analytics.notification.dto;

import com.flypro.analytics.notification.enums.NotificationChannelType;
import com.flypro.analytics.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Send a notification")
public record SendNotificationRequest(
        @NotNull NotificationChannelType channel,
        @NotNull NotificationType type,
        @NotBlank @Size(max = 255) String recipient,
        @NotBlank @Size(max = 255) String subject,
        @Size(max = 10000) String body,
        String templateName,
        Map<String, Object> templateModel,
        String severity,
        String relatedEntity,
        String relatedId
) {
}
