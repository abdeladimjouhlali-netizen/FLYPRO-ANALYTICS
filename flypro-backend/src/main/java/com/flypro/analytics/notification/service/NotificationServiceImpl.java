package com.flypro.analytics.notification.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.notification.channel.NotificationChannel;
import com.flypro.analytics.notification.dto.NotificationResponse;
import com.flypro.analytics.notification.dto.SendNotificationRequest;
import com.flypro.analytics.notification.entity.Notification;
import com.flypro.analytics.notification.enums.NotificationChannelType;
import com.flypro.analytics.notification.enums.NotificationStatus;
import com.flypro.analytics.notification.enums.NotificationType;
import com.flypro.analytics.notification.mapper.NotificationMapper;
import com.flypro.analytics.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Default {@link NotificationService}: persists every notification, then dispatches
 * it through whichever {@link NotificationChannel} supports the requested channel type.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EmailTemplateService emailTemplateService;
    private final List<NotificationChannel> channels;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                    NotificationMapper notificationMapper,
                                    EmailTemplateService emailTemplateService,
                                    List<NotificationChannel> channels) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.emailTemplateService = emailTemplateService;
        this.channels = channels;
    }

    @Override
    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        String body = (request.templateName() != null && !request.templateName().isBlank())
                ? emailTemplateService.render(request.templateName(), request.templateModel())
                : request.body();

        Notification notification = Notification.builder()
                .channel(request.channel())
                .type(request.type())
                .recipient(request.recipient())
                .subject(request.subject())
                .body(body != null ? body : "")
                .status(NotificationStatus.PENDING)
                .severity(request.severity())
                .relatedEntity(request.relatedEntity())
                .relatedId(request.relatedId())
                .build();

        dispatch(notification);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(String recipient, Pageable pageable) {
        Page<Notification> page = (recipient == null || recipient.isBlank())
                ? notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                : notificationRepository.findByRecipientIgnoreCaseOrderByCreatedAtDesc(recipient, pageable);
        return PageResponse.from(page.map(notificationMapper::toResponse));
    }

    @Override
    public NotificationResponse notifyAlert(String recipient, String title, String severity, String details) {
        return send(new SendNotificationRequest(NotificationChannelType.SYSTEM, NotificationType.ALERT, recipient,
                title, details, null, null, severity, "Alert", null));
    }

    @Override
    public NotificationResponse notifyCriticalAlert(String recipient, String title, String details) {
        return send(new SendNotificationRequest(NotificationChannelType.EMAIL, NotificationType.CRITICAL_ALERT,
                recipient, title, details, null, null, "CRITICAL", "Alert", null));
    }

    @Override
    public NotificationResponse notifyImportCompleted(String recipient, String fileName, int valid, int invalid) {
        String body = "Import '" + fileName + "' completed — valid=" + valid + ", invalid=" + invalid;
        return send(new SendNotificationRequest(NotificationChannelType.SYSTEM, NotificationType.IMPORT_COMPLETED,
                recipient, "Import completed: " + fileName, body, null, null, null, "ImportBatch", null));
    }

    @Override
    public NotificationResponse notifyBadgeExpiration(String recipient, List<Map<String, Object>> badges) {
        String body = "Badges expiring soon: " + badges.size();
        return send(new SendNotificationRequest(NotificationChannelType.EMAIL, NotificationType.BADGE_EXPIRATION,
                recipient, "Badge expiration reminder", body, null, Map.of("badges", badges), null, "Badge", null));
    }

    @Override
    public NotificationResponse notifyWeeklySummary(String recipient, Map<String, Object> summary) {
        return send(new SendNotificationRequest(NotificationChannelType.EMAIL, NotificationType.WEEKLY_SUMMARY,
                recipient, "Weekly summary", String.valueOf(summary), null, summary, null, null, null));
    }

    @Override
    public NotificationResponse notifyMonthlySummary(String recipient, Map<String, Object> summary) {
        return send(new SendNotificationRequest(NotificationChannelType.EMAIL, NotificationType.MONTHLY_SUMMARY,
                recipient, "Monthly summary", String.valueOf(summary), null, summary, null, null, null));
    }

    @Override
    public NotificationResponse notifyReportReady(String recipient, String reportName, NotificationType type) {
        return send(new SendNotificationRequest(NotificationChannelType.SYSTEM, type, recipient,
                "Report ready: " + reportName, "Your report '" + reportName + "' is ready.",
                null, null, null, "Report", null));
    }

    private void dispatch(Notification notification) {
        NotificationChannel channel = channels.stream()
                .filter(c -> c.supports(notification.getChannel()))
                .findFirst()
                .orElse(null);
        if (channel == null) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage("No channel handler for " + notification.getChannel());
            return;
        }
        try {
            channel.send(notification.getRecipient(), notification.getSubject(), notification.getBody());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (RuntimeException ex) {
            log.warn("Notification dispatch failed channel={} recipient={}", notification.getChannel(),
                    notification.getRecipient(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }
    }
}
