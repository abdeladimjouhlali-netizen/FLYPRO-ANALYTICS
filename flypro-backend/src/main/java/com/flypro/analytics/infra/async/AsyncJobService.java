package com.flypro.analytics.infra.async;

import com.flypro.analytics.infra.messaging.AsyncJobPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Facade that offloads long-running work to the message broker / async pool.
 * Business APIs stay synchronous; callers may optionally enqueue via this port.
 */
@Service
public class AsyncJobService {

    private final AsyncJobPublisher publisher;

    public AsyncJobService(AsyncJobPublisher publisher) {
        this.publisher = publisher;
    }

    @Async
    public CompletableFuture<String> enqueueImport(Map<String, Object> payload) {
        return CompletableFuture.completedFuture(publisher.publishImportJob(payload));
    }

    @Async
    public CompletableFuture<String> enqueueAiPrediction(Map<String, Object> payload) {
        return CompletableFuture.completedFuture(publisher.publishAiPrediction(payload));
    }

    @Async
    public CompletableFuture<String> enqueueDashboardRefresh(Map<String, Object> payload) {
        return CompletableFuture.completedFuture(publisher.publishDashboardRefresh(payload));
    }

    @Async
    public CompletableFuture<String> enqueueNotification(Map<String, Object> payload) {
        return CompletableFuture.completedFuture(publisher.publishNotification(payload));
    }

    @Async
    public CompletableFuture<String> enqueueAlertGeneration(Map<String, Object> payload) {
        return CompletableFuture.completedFuture(publisher.publishAlert(payload));
    }
}
