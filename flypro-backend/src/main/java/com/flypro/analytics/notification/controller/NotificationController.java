package com.flypro.analytics.notification.controller;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.notification.dto.NotificationResponse;
import com.flypro.analytics.notification.dto.SendNotificationRequest;
import com.flypro.analytics.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification REST API (Phase 5 – Step 19).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Send and list system / email notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Send a notification",
            description = "Supports EMAIL and SYSTEM channels. SMS / Teams / Slack / WebSocket reserved for future releases.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "channel": "EMAIL",
                              "type": "ALERT",
                              "recipient": "admin@flypro.local",
                              "subject": "Test alert",
                              "templateName": "email/alert",
                              "templateModel": {
                                "title": "Denied access spike",
                                "severity": "HIGH",
                                "details": "3 denials in 5 minutes"
                              },
                              "severity": "HIGH"
                            }
                            """))
            )
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Notification processed", notificationService.send(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "List notifications", description = "Optional filter by recipient email/username")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @RequestParam(required = false) String recipient,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved",
                notificationService.list(recipient, pageable)));
    }
}
