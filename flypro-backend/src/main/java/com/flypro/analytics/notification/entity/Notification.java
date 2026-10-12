package com.flypro.analytics.notification.entity;

import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.notification.enums.NotificationChannelType;
import com.flypro.analytics.notification.enums.NotificationStatus;
import com.flypro.analytics.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Persisted notification (table created by {@code V11__reports_notifications.sql}).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseAuditableEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannelType channel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @NotBlank
    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @NotBlank
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "severity", length = 30)
    private String severity;

    @Column(name = "related_entity", length = 100)
    private String relatedEntity;

    @Column(name = "related_id", length = 100)
    private String relatedId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;
}
