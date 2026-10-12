package com.flypro.analytics.importbatch.service;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.alert.engine.AlertRuleEngine;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import com.flypro.analytics.badgeassignment.repository.BadgeAssignmentRepository;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EventSource;
import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.common.enums.ImportStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.dataquality.entity.DataQualityIssue;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.importbatch.dto.DataQualityIssueDTO;
import com.flypro.analytics.importbatch.dto.ImportRequestDTO;
import com.flypro.analytics.importbatch.dto.ImportResponseDTO;
import com.flypro.analytics.importbatch.dto.ImportSearchCriteria;
import com.flypro.analytics.importbatch.dto.ImportSummaryDTO;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import com.flypro.analytics.importbatch.exception.DataValidationException;
import com.flypro.analytics.importbatch.exception.DuplicateImportException;
import com.flypro.analytics.importbatch.exception.ImportBatchNotFoundException;
import com.flypro.analytics.importbatch.exception.InvalidFileFormatException;
import com.flypro.analytics.importbatch.exception.UnsupportedFileTypeException;
import com.flypro.analytics.importbatch.mapper.ImportMapper;
import com.flypro.analytics.importbatch.parser.ImportColumnNormalizer;
import com.flypro.analytics.importbatch.parser.ImportFileParserRegistry;
import com.flypro.analytics.importbatch.parser.ImportRawRecord;
import com.flypro.analytics.importbatch.repository.ImportBatchRepository;
import com.flypro.analytics.importbatch.specification.ImportBatchSpecification;
import com.flypro.analytics.importbatch.storage.ImportFileStorage;
import com.flypro.analytics.importbatch.validator.ImportValidator;
import com.flypro.analytics.zone.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Default implementation of {@link ImportService}.
 * Continues processing on row-level errors and never deletes previous imports.
 */
@Service
@Transactional
public class ImportServiceImpl implements ImportService {

    private final ImportBatchRepository importBatchRepository;
    private final AccessEventRepository accessEventRepository;
    private final BadgeRepository badgeRepository;
    private final AccessPointRepository accessPointRepository;
    private final BadgeAssignmentRepository badgeAssignmentRepository;
    private final ImportFileParserRegistry parserRegistry;
    private final ImportFileStorage fileStorage;
    private final ImportValidator importValidator;
    private final ImportMapper importMapper;
    private final AlertRuleEngine alertRuleEngine;

    public ImportServiceImpl(ImportBatchRepository importBatchRepository,
                             AccessEventRepository accessEventRepository,
                             BadgeRepository badgeRepository,
                             AccessPointRepository accessPointRepository,
                             BadgeAssignmentRepository badgeAssignmentRepository,
                             ImportFileParserRegistry parserRegistry,
                             ImportFileStorage fileStorage,
                             ImportValidator importValidator,
                             ImportMapper importMapper,
                             AlertRuleEngine alertRuleEngine) {
        this.importBatchRepository = importBatchRepository;
        this.accessEventRepository = accessEventRepository;
        this.badgeRepository = badgeRepository;
        this.accessPointRepository = accessPointRepository;
        this.badgeAssignmentRepository = badgeAssignmentRepository;
        this.parserRegistry = parserRegistry;
        this.fileStorage = fileStorage;
        this.importValidator = importValidator;
        this.importMapper = importMapper;
        this.alertRuleEngine = alertRuleEngine;
    }

    @Override
    public ImportResponseDTO upload(MultipartFile file) {
        importValidator.validateFilePresent(file);
        ImportFileType fileType = parserRegistry.detect(file);
        byte[] content = readBytes(file);

        ImportBatch batch = ImportBatch.builder()
                .fileName(file.getOriginalFilename())
                .source(fileType.name())
                .status(ImportStatus.PENDING)
                .totalRecords(0)
                .validRecords(0)
                .invalidRecords(0)
                .duplicateRecords(0)
                .build();

        ImportBatch saved = importBatchRepository.save(batch);
        fileStorage.store(saved.getId(), new ImportFileStorage.StagedImportFile(
                saved.getFileName(), fileType, content
        ));
        return importMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportSummaryDTO preview(MultipartFile file) {
        importValidator.validateFilePresent(file);
        ImportFileType fileType = parserRegistry.detect(file);
        byte[] content = readBytes(file);
        Instant started = Instant.now();
        ProcessingResult result = processRecords(
                parserRegistry.parse(fileType, content, file.getOriginalFilename()),
                fileType,
                false,
                null
        );
        long elapsed = Duration.between(started, Instant.now()).toMillis();
        return toSummary(null, file.getOriginalFilename(), fileType, ImportStatus.PENDING, result, elapsed);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportSummaryDTO preview(Long batchId) {
        ImportBatch batch = findBatch(batchId);
        ImportFileStorage.StagedImportFile staged = fileStorage.get(batchId)
                .orElseThrow(() -> new InvalidFileFormatException(
                        "No staged file found for batch " + batchId + ". Upload the file again."));
        Instant started = Instant.now();
        ProcessingResult result = processRecords(
                parserRegistry.parse(staged.fileType(), staged.content(), staged.fileName()),
                staged.fileType(),
                false,
                null
        );
        long elapsed = Duration.between(started, Instant.now()).toMillis();
        return toSummary(batch.getId(), batch.getFileName(), staged.fileType(),
                batch.getStatus(), result, elapsed);
    }

    @Override
    @Transactional(noRollbackFor = {
            InvalidFileFormatException.class,
            UnsupportedFileTypeException.class,
            DataValidationException.class
    })
    public ImportResponseDTO execute(ImportRequestDTO request) {
        if (request == null || request.batchId() == null) {
            throw new InvalidFileFormatException("batchId is required to execute an import");
        }
        ImportBatch batch = findBatch(request.batchId());
        assertExecutable(batch);

        ImportFileStorage.StagedImportFile staged = fileStorage.get(batch.getId())
                .orElseThrow(() -> new InvalidFileFormatException(
                        "No staged file found for batch " + batch.getId() + ". Upload the file again."));

        return runImport(batch, staged.fileType(), staged.content(), staged.fileName());
    }

    @Override
    @Transactional(noRollbackFor = {
            InvalidFileFormatException.class,
            UnsupportedFileTypeException.class,
            DataValidationException.class
    })
    public ImportResponseDTO execute(MultipartFile file) {
        importValidator.validateFilePresent(file);
        ImportFileType fileType = parserRegistry.detect(file);
        byte[] content = readBytes(file);

        ImportBatch batch = ImportBatch.builder()
                .fileName(file.getOriginalFilename())
                .source(fileType.name())
                .status(ImportStatus.PENDING)
                .totalRecords(0)
                .validRecords(0)
                .invalidRecords(0)
                .duplicateRecords(0)
                .build();
        ImportBatch saved = importBatchRepository.save(batch);
        return runImport(saved, fileType, content, saved.getFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ImportResponseDTO> search(ImportSearchCriteria criteria, Pageable pageable) {
        Specification<ImportBatch> specification = ImportBatchSpecification.withCriteria(criteria);
        Page<ImportResponseDTO> page = importBatchRepository.findAll(specification, pageable)
                .map(importMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportResponseDTO getById(Long id) {
        return importMapper.toResponse(findBatch(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ImportSummaryDTO getReport(Long id) {
        ImportBatch batch = findBatch(id);
        ImportFileType fileType = importMapper.toFileType(batch.getSource());
        List<DataQualityIssueDTO> issues = importMapper.toIssueDtos(batch.getDataQualityIssues());

        List<DataQualityIssueDTO> errors = issues.stream()
                .filter(i -> i.severity() == AlertSeverity.HIGH || i.severity() == AlertSeverity.CRITICAL)
                .toList();
        List<DataQualityIssueDTO> warnings = issues.stream()
                .filter(i -> i.severity() == AlertSeverity.MEDIUM || i.severity() == AlertSeverity.LOW)
                .toList();

        long processingTimeMs = 0L;
        if (batch.getStartedAt() != null && batch.getFinishedAt() != null) {
            processingTimeMs = Duration.between(batch.getStartedAt(), batch.getFinishedAt()).toMillis();
        }

        return new ImportSummaryDTO(
                batch.getId(),
                batch.getFileName(),
                fileType,
                batch.getStatus(),
                batch.getTotalRecords(),
                batch.getValidRecords(),
                batch.getInvalidRecords(),
                batch.getDuplicateRecords(),
                errors,
                warnings,
                processingTimeMs
        );
    }

    private ImportResponseDTO runImport(ImportBatch batch,
                                        ImportFileType fileType,
                                        byte[] content,
                                        String fileName) {
        Instant started = Instant.now();
        batch.setStatus(ImportStatus.PROCESSING);
        batch.setStartedAt(started);
        batch.getDataQualityIssues().clear();
        importBatchRepository.save(batch);

        try {
            List<ImportRawRecord> records = parserRegistry.parse(fileType, content, fileName);
            ProcessingResult result = processRecords(records, fileType, true, batch);

            batch.setTotalRecords(result.totalRecords());
            batch.setValidRecords(result.importedRecords());
            batch.setInvalidRecords(result.rejectedRecords());
            batch.setDuplicateRecords(result.duplicateRecords());
            batch.setFinishedAt(Instant.now());
            batch.setStatus(resolveStatus(result));

            ImportBatch saved = importBatchRepository.save(batch);
            fileStorage.remove(saved.getId());
            alertRuleEngine.evaluateImportQuality(saved);
            return importMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            batch.setStatus(ImportStatus.FAILED);
            batch.setFinishedAt(Instant.now());
            addIssue(batch, null, "file", ex.getMessage(), AlertSeverity.CRITICAL);
            importBatchRepository.save(batch);
            fileStorage.remove(batch.getId());
            if (ex instanceof InvalidFileFormatException invalid) {
                throw invalid;
            }
            throw new InvalidFileFormatException("Import failed: " + ex.getMessage());
        }
    }

    private ProcessingResult processRecords(List<ImportRawRecord> records,
                                            ImportFileType fileType,
                                            boolean persist,
                                            ImportBatch batch) {
        importValidator.validateStructure(records);

        int imported = 0;
        int rejected = 0;
        int duplicates = 0;
        List<DataQualityIssueDTO> errors = new ArrayList<>();
        List<DataQualityIssueDTO> warnings = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        EventSource eventSource = toEventSource(fileType);

        for (ImportRawRecord record : records) {
            List<ImportValidator.RowIssue> rowIssues = importValidator.validateRow(record);
            if (!rowIssues.isEmpty()) {
                rejected++;
                for (ImportValidator.RowIssue issue : rowIssues) {
                    DataQualityIssueDTO dto = toDto(issue);
                    errors.add(dto);
                    if (persist && batch != null) {
                        addIssue(batch, issue.lineNumber(), issue.fieldName(), issue.message(), issue.severity());
                    }
                }
                continue;
            }

            Instant timestamp = importValidator.parseTimestamp(
                    record.get(ImportColumnNormalizer.EVENT_TIMESTAMP));
            AccessDirection direction = importValidator.parseDirection(
                    record.get(ImportColumnNormalizer.DIRECTION));
            AccessResult requestedResult = importValidator.parseResult(
                    record.get(ImportColumnNormalizer.ACCESS_RESULT));
            String badgeNumber = record.get(ImportColumnNormalizer.BADGE_NUMBER);
            String accessPointCode = record.get(ImportColumnNormalizer.ACCESS_POINT_CODE);
            String refusalReason = record.get(ImportColumnNormalizer.REFUSAL_REASON);

            Badge badge = badgeRepository.findByBadgeNumberIgnoreCase(badgeNumber).orElse(null);
            if (badge == null) {
                rejected++;
                String message = "Badge number not found: " + badgeNumber;
                errors.add(issueDto(record.lineNumber(), ImportColumnNormalizer.BADGE_NUMBER, message, AlertSeverity.HIGH));
                if (persist && batch != null) {
                    addIssue(batch, record.lineNumber(), ImportColumnNormalizer.BADGE_NUMBER, message, AlertSeverity.HIGH);
                }
                continue;
            }

            AccessPoint accessPoint = accessPointRepository.findByCodeIgnoreCase(accessPointCode).orElse(null);
            if (accessPoint == null) {
                rejected++;
                String message = "Access point not found: " + accessPointCode;
                errors.add(issueDto(record.lineNumber(), ImportColumnNormalizer.ACCESS_POINT_CODE, message, AlertSeverity.HIGH));
                if (persist && batch != null) {
                    addIssue(batch, record.lineNumber(), ImportColumnNormalizer.ACCESS_POINT_CODE, message, AlertSeverity.HIGH);
                }
                continue;
            }

            String duplicateKey = (badge.getId() + "|" + accessPoint.getId() + "|"
                    + timestamp + "|" + direction).toLowerCase(Locale.ROOT);

            boolean duplicateInFile = !seenKeys.add(duplicateKey);
            boolean duplicateInDb = accessEventRepository
                    .existsByBadgeIdAndAccessPointIdAndEventTimestampAndDirection(
                            badge.getId(), accessPoint.getId(), timestamp, direction);

            if (duplicateInFile || duplicateInDb) {
                duplicates++;
                String message = duplicateInFile
                        ? "Duplicate event within import file ignored"
                        : "Duplicate event already exists in database ignored";
                warnings.add(issueDto(record.lineNumber(), null, message, AlertSeverity.MEDIUM));
                if (persist && batch != null) {
                    addIssue(batch, record.lineNumber(), null, message, AlertSeverity.MEDIUM);
                }
                continue;
            }

            if (!persist) {
                imported++;
                continue;
            }

            applyBadgeExpiration(badge);
            Zone zone = accessPoint.getZone();
            if (zone == null) {
                rejected++;
                String message = "Access point has no associated zone: " + accessPointCode;
                errors.add(issueDto(record.lineNumber(), ImportColumnNormalizer.ACCESS_POINT_CODE, message, AlertSeverity.HIGH));
                addIssue(batch, record.lineNumber(), ImportColumnNormalizer.ACCESS_POINT_CODE, message, AlertSeverity.HIGH);
                continue;
            }

            Employee employee = badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badge.getId())
                    .map(BadgeAssignment::getEmployee)
                    .orElse(null);
            if (employee == null) {
                String warn = "No active badge assignment found; employee left empty";
                warnings.add(issueDto(record.lineNumber(), ImportColumnNormalizer.BADGE_NUMBER, warn, AlertSeverity.LOW));
                addIssue(batch, record.lineNumber(), ImportColumnNormalizer.BADGE_NUMBER, warn, AlertSeverity.LOW);
            }

            AccessResultDecision decision = resolveResult(badge, requestedResult, refusalReason);

            AccessEvent event = AccessEvent.builder()
                    .eventTimestamp(timestamp)
                    .direction(direction)
                    .result(decision.result())
                    .refusalReason(decision.refusalReason())
                    .source(eventSource)
                    .rawPayload(null)
                    .badge(badge)
                    .employee(employee)
                    .accessPoint(accessPoint)
                    .zone(zone)
                    .build();
            accessEventRepository.save(event);
            imported++;
        }

        return new ProcessingResult(records.size(), imported, rejected, duplicates, errors, warnings);
    }

    private ImportStatus resolveStatus(ProcessingResult result) {
        if (result.importedRecords() == 0 && result.rejectedRecords() > 0) {
            return ImportStatus.FAILED;
        }
        if (result.rejectedRecords() > 0 || result.duplicateRecords() > 0) {
            if (result.importedRecords() > 0) {
                return ImportStatus.PARTIALLY_COMPLETED;
            }
        }
        if (result.importedRecords() > 0 && result.rejectedRecords() == 0) {
            return result.duplicateRecords() > 0
                    ? ImportStatus.PARTIALLY_COMPLETED
                    : ImportStatus.COMPLETED;
        }
        if (result.totalRecords() == 0) {
            return ImportStatus.FAILED;
        }
        return ImportStatus.COMPLETED;
    }

    private void assertExecutable(ImportBatch batch) {
        if (batch.getStatus() == ImportStatus.PROCESSING) {
            throw new DuplicateImportException(batch.getId());
        }
        if (batch.getStatus() == ImportStatus.COMPLETED
                || batch.getStatus() == ImportStatus.PARTIALLY_COMPLETED
                || batch.getStatus() == ImportStatus.FAILED) {
            throw new DuplicateImportException(batch.getId());
        }
    }

    private AccessResultDecision resolveResult(Badge badge, AccessResult requested, String refusalReason) {
        return switch (badge.getStatus()) {
            case EXPIRED -> new AccessResultDecision(
                    AccessResult.EXPIRED,
                    firstNonBlank(refusalReason, "Badge is expired"));
            case REVOKED -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(refusalReason, "Badge is revoked"));
            case SUSPENDED -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(refusalReason, "Badge is suspended"));
            case LOST -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(refusalReason, "Badge is reported lost"));
            case PENDING_ACTIVATION -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(refusalReason, "Badge is pending activation"));
            case ACTIVE -> new AccessResultDecision(
                    requested,
                    requested == AccessResult.AUTHORIZED ? null : refusalReason);
        };
    }

    private void applyBadgeExpiration(Badge badge) {
        if (badge.getStatus() != BadgeStatus.EXPIRED
                && badge.getStatus() != BadgeStatus.REVOKED
                && badge.getExpirationDate() != null
                && badge.getExpirationDate().isBefore(LocalDate.now())) {
            badge.setStatus(BadgeStatus.EXPIRED);
            badgeRepository.save(badge);
        }
    }

    private void addIssue(ImportBatch batch,
                          Integer lineNumber,
                          String fieldName,
                          String message,
                          AlertSeverity severity) {
        DataQualityIssue issue = DataQualityIssue.builder()
                .lineNumber(lineNumber)
                .fieldName(fieldName)
                .message(truncate(message, 1000))
                .severity(severity)
                .importBatch(batch)
                .build();
        batch.getDataQualityIssues().add(issue);
    }

    private ImportSummaryDTO toSummary(Long batchId,
                                       String fileName,
                                       ImportFileType fileType,
                                       ImportStatus status,
                                       ProcessingResult result,
                                       long elapsedMs) {
        return new ImportSummaryDTO(
                batchId,
                fileName,
                fileType,
                status,
                result.totalRecords(),
                result.importedRecords(),
                result.rejectedRecords(),
                result.duplicateRecords(),
                result.errors(),
                result.warnings(),
                elapsedMs
        );
    }

    private DataQualityIssueDTO toDto(ImportValidator.RowIssue issue) {
        return issueDto(issue.lineNumber(), issue.fieldName(), issue.message(), issue.severity());
    }

    private DataQualityIssueDTO issueDto(Integer line, String field, String message, AlertSeverity severity) {
        return new DataQualityIssueDTO(null, line, field, message, severity);
    }

    private EventSource toEventSource(ImportFileType fileType) {
        return switch (fileType) {
            case CSV -> EventSource.CSV;
            case JSON -> EventSource.JSON;
            case XLSX -> EventSource.EXCEL;
        };
    }

    private ImportBatch findBatch(Long id) {
        return importBatchRepository.findById(id)
                .orElseThrow(() -> new ImportBatchNotFoundException(id));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new InvalidFileFormatException("Unable to read uploaded file: " + ex.getMessage());
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record ProcessingResult(
            int totalRecords,
            int importedRecords,
            int rejectedRecords,
            int duplicateRecords,
            List<DataQualityIssueDTO> errors,
            List<DataQualityIssueDTO> warnings
    ) {
    }

    private record AccessResultDecision(AccessResult result, String refusalReason) {
    }
}
