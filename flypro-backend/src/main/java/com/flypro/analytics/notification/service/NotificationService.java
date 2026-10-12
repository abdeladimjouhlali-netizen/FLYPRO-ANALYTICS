package com.flypro.analytics.notification.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.notification.dto.NotificationResponse;
import com.flypro.analytics.notification.dto.SendNotificationRequest;
import com.flypro.analytics.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    NotificationResponse send(SendNotificationRequest request);

    PageResponse<NotificationResponse> list(String recipient, Pageable pageable);

    NotificationResponse notifyAlert(String recipient, String title, String severity, String details);

    NotificationResponse notifyCriticalAlert(String recipient, String title, String details);

    NotificationResponse notifyImportCompleted(String recipient, String fileName, int valid, int invalid);

    NotificationResponse notifyBadgeExpiration(String recipient, List<Map<String, Object>> badges);

    NotificationResponse notifyWeeklySummary(String recipient, Map<String, Object> summary);

    NotificationResponse notifyMonthlySummary(String recipient, Map<String, Object> summary);

    NotificationResponse notifyReportReady(String recipient, String reportName, NotificationType type);
}
