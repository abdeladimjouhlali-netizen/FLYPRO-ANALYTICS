package com.flypro.analytics.infra.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes async job envelopes to RabbitMQ, or drops them with a debug log
 * when {@code flypro.rabbitmq.enabled=false} (RabbitTemplate then never exists).
 */
@Component
public class AsyncJobPublisher {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final boolean enabled;

    public AsyncJobPublisher(
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
            @Value("${flypro.rabbitmq.enabled:false}") boolean enabled
    ) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.enabled = enabled;
    }

    public String publishImportJob(Map<String, Object> payload) {
        return publish(MessagingQueues.IMPORT_JOBS, payload);
    }

    public String publishAiPrediction(Map<String, Object> payload) {
        return publish(MessagingQueues.AI_PREDICTIONS, payload);
    }

    public String publishDashboardRefresh(Map<String, Object> payload) {
        return publish(MessagingQueues.DASHBOARD_REFRESH, payload);
    }

    public String publishNotification(Map<String, Object> payload) {
        return publish(MessagingQueues.NOTIFICATIONS, payload);
    }

    public String publishAlert(Map<String, Object> payload) {
        return publish(MessagingQueues.ALERTS, payload);
    }

    private String publish(String queue, Map<String, Object> payload) {
        String jobId = UUID.randomUUID().toString();
        RabbitTemplate template = enabled ? rabbitTemplateProvider.getIfAvailable() : null;
        if (template == null) {
            log.debug("RabbitMQ disabled — dropping async job queue={} jobId={}", queue, jobId);
            return jobId;
        }
        Map<String, Object> envelope = new HashMap<>(payload);
        envelope.put("jobId", jobId);
        template.convertAndSend(queue, envelope);
        return jobId;
    }
}
