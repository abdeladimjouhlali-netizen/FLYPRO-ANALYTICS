package com.flypro.analytics.infra.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Infrastructure listeners – process async envelopes without altering domain APIs.
 * Domain modules can later subscribe via events / ports.
 */
@Component
@ConditionalOnProperty(prefix = "flypro.rabbitmq", name = "enabled", havingValue = "true")
public class AsyncJobListeners {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobListeners.class);

    @RabbitListener(queues = MessagingQueues.IMPORT_JOBS)
    public void onImportJob(Map<String, Object> message) {
        log.info("Async import job received jobId={}", message.get("jobId"));
    }

    @RabbitListener(queues = MessagingQueues.AI_PREDICTIONS)
    public void onAiPrediction(Map<String, Object> message) {
        log.info("Async AI prediction job received jobId={}", message.get("jobId"));
    }

    @RabbitListener(queues = MessagingQueues.ALERTS)
    public void onAlert(Map<String, Object> message) {
        log.info("Async alert job received jobId={}", message.get("jobId"));
    }

    @RabbitListener(queues = MessagingQueues.NOTIFICATIONS)
    public void onNotification(Map<String, Object> message) {
        log.info("Async notification job received jobId={}", message.get("jobId"));
    }

    @RabbitListener(queues = MessagingQueues.DASHBOARD_REFRESH)
    public void onDashboardRefresh(Map<String, Object> message) {
        log.info("Async dashboard refresh received jobId={}", message.get("jobId"));
    }

    @RabbitListener(queues = MessagingQueues.ACCESS_EVENTS)
    public void onAccessEvent(Map<String, Object> message) {
        log.debug("Async access-event envelope jobId={}", message.get("jobId"));
    }

    @RabbitListener(queues = MessagingQueues.AUDIT_LOGS)
    public void onAuditLog(Map<String, Object> message) {
        log.debug("Async audit envelope jobId={}", message.get("jobId"));
    }

    @RabbitListener(queues = MessagingQueues.DLQ)
    public void onDeadLetter(Map<String, Object> message) {
        log.error("Dead-letter message received payload={}", message);
    }
}
