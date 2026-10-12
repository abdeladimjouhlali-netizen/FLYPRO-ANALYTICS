package com.flypro.analytics.infra.async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Async executor for long-running infrastructure jobs (imports, AI, mail, dashboard refresh).
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Value("${flypro.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${flypro.async.max-pool-size:16}")
    private int maxPoolSize;

    @Value("${flypro.async.queue-capacity:500}")
    private int queueCapacity;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("flypro-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                        .error("Async error in {} : {}", method.getName(), ex.toString());
    }
}
