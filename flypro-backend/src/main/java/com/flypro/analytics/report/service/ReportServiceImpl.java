package com.flypro.analytics.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flypro.analytics.notification.enums.NotificationType;
import com.flypro.analytics.notification.service.NotificationService;
import com.flypro.analytics.report.dto.ExportFileResult;
import com.flypro.analytics.report.dto.ExportReportRequest;
import com.flypro.analytics.report.dto.ReportFilterCriteria;
import com.flypro.analytics.report.dto.ReportResponseDTO;
import com.flypro.analytics.report.dto.ReportScheduleResponse;
import com.flypro.analytics.report.dto.ScheduleReportRequest;
import com.flypro.analytics.report.entity.ReportSchedule;
import com.flypro.analytics.report.enums.ExportFormat;
import com.flypro.analytics.report.enums.ReportFrequency;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import com.flypro.analytics.report.exception.ExportException;
import com.flypro.analytics.report.generator.ReportGenerator;
import com.flypro.analytics.report.model.ReportDocument;
import com.flypro.analytics.report.repository.ReportScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default {@link ReportService}: delegates row-building to {@link ReportDocumentFactory},
 * rendering to the {@link ReportGenerator} matching the requested {@link ExportFormat},
 * and recipient notification to the existing {@link NotificationService}.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final ReportDocumentFactory documentFactory;
    private final Map<ExportFormat, ReportGenerator> generators;
    private final ReportScheduleRepository scheduleRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ReportServiceImpl(ReportDocumentFactory documentFactory,
                              List<ReportGenerator> generatorList,
                              ReportScheduleRepository scheduleRepository,
                              NotificationService notificationService,
                              ObjectMapper objectMapper) {
        this.documentFactory = documentFactory;
        this.generators = generatorList.stream()
                .collect(Collectors.toMap(ReportGenerator::format, g -> g));
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReportResponseDTO getReport(ReportType type, ReportFilterCriteria filters) {
        ReportDocument doc = documentFactory.build(type, filters);
        return new ReportResponseDTO(doc.getReportType(), doc.getReportKind(), doc.getTitle(), doc.getGeneratedAt(),
                doc.getSummary(), doc.getColumns(), doc.getRows(), doc.getRows().size());
    }

    @Override
    public ExportFileResult export(ExportReportRequest request) {
        ReportDocument document = documentFactory.build(request.reportType(), request.filters());
        ReportGenerator generator = generatorFor(request.exportFormat());
        byte[] content = generator.generate(document);
        String fileName = fileName(request.reportType(), generator);
        notifyRecipients(request.notifyEmails(), document.getTitle());
        return new ExportFileResult(fileName, generator.contentType(), content);
    }

    @Override
    @Transactional
    public ReportScheduleResponse schedule(ScheduleReportRequest request) {
        ReportSchedule entity = ReportSchedule.builder()
                .name(request.name())
                .reportType(request.reportType())
                .exportFormat(request.exportFormat())
                .reportKind(request.reportKind())
                .frequency(request.frequency())
                .filtersJson(writeFiltersJson(request.filters()))
                .recipients(String.join(",", request.recipients()))
                .enabled(true)
                .nextRunAt(nextRun(request.frequency(), Instant.now()))
                .build();
        return toScheduleResponse(scheduleRepository.save(entity));
    }

    @Override
    @Transactional
    public void runDueSchedules() {
        Instant now = Instant.now();
        for (ReportSchedule schedule : scheduleRepository.findByEnabledTrueAndNextRunAtLessThanEqual(now)) {
            try {
                ReportFilterCriteria filters = readFiltersJson(schedule.getFiltersJson(), schedule.getReportKind());
                ReportDocument document = documentFactory.build(schedule.getReportType(), filters);
                ReportGenerator generator = generatorFor(schedule.getExportFormat());
                generator.generate(document);
                notifyRecipients(splitRecipients(schedule.getRecipients()), schedule.getName());
            } catch (RuntimeException ex) {
                log.error("Failed to run scheduled report id={} name={}", schedule.getId(), schedule.getName(), ex);
            } finally {
                schedule.setLastRunAt(now);
                schedule.setNextRunAt(nextRun(schedule.getFrequency(), now));
                scheduleRepository.save(schedule);
            }
        }
    }

    private void notifyRecipients(List<String> recipients, String reportName) {
        if (recipients == null) {
            return;
        }
        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) {
                continue;
            }
            try {
                notificationService.notifyReportReady(recipient, reportName, NotificationType.REPORT_READY);
            } catch (RuntimeException ex) {
                log.warn("Failed to notify {} about report '{}'", recipient, reportName, ex);
            }
        }
    }

    private ReportGenerator generatorFor(ExportFormat format) {
        ReportGenerator generator = generators.get(format);
        if (generator == null) {
            throw new ExportException("No generator registered for format " + format);
        }
        return generator;
    }

    private static String fileName(ReportType type, ReportGenerator generator) {
        return type.name().toLowerCase(Locale.ROOT) + "-" + FILE_STAMP.format(Instant.now())
                + "." + generator.fileExtension();
    }

    private String writeFiltersJson(ReportFilterCriteria filters) {
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException ex) {
            throw new ExportException("Failed to serialize report filters", ex);
        }
    }

    private ReportFilterCriteria readFiltersJson(String json, ReportKind fallbackKind) {
        if (json == null || json.isBlank()) {
            return new ReportFilterCriteria(null, null, null, null, null, null, null, null, null, null, null, fallbackKind);
        }
        try {
            return objectMapper.readValue(json, ReportFilterCriteria.class);
        } catch (JsonProcessingException ex) {
            throw new ExportException("Failed to deserialize report filters", ex);
        }
    }

    private static List<String> splitRecipients(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static Instant nextRun(ReportFrequency frequency, Instant from) {
        return switch (frequency) {
            case DAILY -> from.plus(Duration.ofDays(1));
            case WEEKLY -> from.plus(Duration.ofDays(7));
            case MONTHLY -> from.plus(Duration.ofDays(30));
        };
    }

    private ReportScheduleResponse toScheduleResponse(ReportSchedule entity) {
        return new ReportScheduleResponse(
                entity.getId(),
                entity.getName(),
                entity.getReportType(),
                entity.getExportFormat(),
                entity.getReportKind(),
                entity.getFrequency(),
                splitRecipients(entity.getRecipients()),
                entity.isEnabled(),
                entity.getLastRunAt(),
                entity.getNextRunAt(),
                entity.getCreatedAt()
        );
    }
}
