package com.flypro.analytics.infra.async;

import com.flypro.analytics.infra.messaging.AsyncJobPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncJobServiceTest {

    @Mock
    private AsyncJobPublisher publisher;
    @InjectMocks
    private AsyncJobService service;

    @Test
    @DisplayName("enqueueImport delegates to publisher")
    void enqueueImport() throws Exception {
        when(publisher.publishImportJob(Map.of("batchId", 1))).thenReturn("job-1");

        CompletableFuture<String> future = service.enqueueImport(Map.of("batchId", 1));

        assertThat(future.get()).isEqualTo("job-1");
        verify(publisher).publishImportJob(Map.of("batchId", 1));
    }

    @Test
    @DisplayName("enqueueAiPrediction delegates to publisher")
    void enqueueAi() throws Exception {
        when(publisher.publishAiPrediction(Map.of())).thenReturn("job-ai");
        assertThat(service.enqueueAiPrediction(Map.of()).get()).isEqualTo("job-ai");
    }
}
