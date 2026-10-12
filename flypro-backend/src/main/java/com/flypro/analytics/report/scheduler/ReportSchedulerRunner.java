package com.flypro.analytics.report.scheduler;

import com.flypro.analytics.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically triggers {@link ReportService#runDueSchedules()}.
 * Disabled via {@code flypro.reports.scheduler-enabled=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "flypro.reports", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReportSchedulerRunner {

    private static final Logger log = LoggerFactory.getLogger(ReportSchedulerRunner.class);

    private final ReportService reportService;

    public ReportSchedulerRunner(ReportService reportService) {
        this.reportService = reportService;
    }

    @Scheduled(
            fixedDelayString = "${flypro.reports.scheduler-interval-ms:300000}",
            initialDelayString = "${flypro.reports.scheduler-initial-delay-ms:30000}"
    )
    public void run() {
        try {
            reportService.runDueSchedules();
        } catch (RuntimeException ex) {
            log.error("Scheduled report run failed", ex);
        }
    }
}
