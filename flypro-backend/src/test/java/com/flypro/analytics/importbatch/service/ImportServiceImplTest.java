package com.flypro.analytics.importbatch.service;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.alert.engine.AlertRuleEngine;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.badgeassignment.repository.BadgeAssignmentRepository;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.common.enums.ImportStatus;
import com.flypro.analytics.importbatch.dto.ImportRequestDTO;
import com.flypro.analytics.importbatch.dto.ImportResponseDTO;
import com.flypro.analytics.importbatch.dto.ImportSearchCriteria;
import com.flypro.analytics.importbatch.dto.ImportSummaryDTO;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import com.flypro.analytics.importbatch.exception.DuplicateImportException;
import com.flypro.analytics.importbatch.exception.ImportBatchNotFoundException;
import com.flypro.analytics.importbatch.exception.InvalidFileFormatException;
import com.flypro.analytics.importbatch.mapper.ImportMapper;
import com.flypro.analytics.importbatch.parser.ImportFileParserRegistry;
import com.flypro.analytics.importbatch.parser.ImportRawRecord;
import com.flypro.analytics.importbatch.repository.ImportBatchRepository;
import com.flypro.analytics.importbatch.storage.ImportFileStorage;
import com.flypro.analytics.importbatch.validator.ImportValidator;
import com.flypro.analytics.zone.entity.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportServiceImplTest {

    @Mock
    private ImportBatchRepository importBatchRepository;
    @Mock
    private AccessEventRepository accessEventRepository;
    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private AccessPointRepository accessPointRepository;
    @Mock
    private BadgeAssignmentRepository badgeAssignmentRepository;
    @Mock
    private ImportFileParserRegistry parserRegistry;
    @Mock
    private ImportFileStorage fileStorage;
    @Mock
    private ImportValidator importValidator;
    @Mock
    private ImportMapper importMapper;
    @Mock
    private AlertRuleEngine alertRuleEngine;

    @InjectMocks
    private ImportServiceImpl importService;

    private Zone zone;
    private AccessPoint accessPoint;
    private Badge badge;
    private ImportBatch pendingBatch;

    @BeforeEach
    void setUp() {
        zone = Zone.builder().id(1L).name("Z1").building("B1").floor("1").sensitive(false).build();
        accessPoint = AccessPoint.builder().id(2L).code("AP-01").name("Door").zone(zone).build();
        badge = Badge.builder()
                .id(3L)
                .badgeNumber("BDG-001")
                .status(BadgeStatus.ACTIVE)
                .type(BadgeType.RFID)
                .issueDate(LocalDate.now().minusMonths(1))
                .build();

        pendingBatch = ImportBatch.builder()
                .id(10L)
                .fileName("events.csv")
                .source("CSV")
                .status(ImportStatus.PENDING)
                .totalRecords(0)
                .validRecords(0)
                .invalidRecords(0)
                .duplicateRecords(0)
                .build();

        when(importMapper.toResponse(any(ImportBatch.class))).thenAnswer(inv -> {
            ImportBatch b = inv.getArgument(0);
            return new ImportResponseDTO(
                    b.getId(), b.getFileName(), ImportFileType.CSV, b.getStatus(),
                    b.getTotalRecords(), b.getValidRecords(), b.getInvalidRecords(),
                    b.getDuplicateRecords(), b.getStartedAt(), b.getFinishedAt(),
                    b.getCreatedAt(), b.getCreatedBy(), List.of()
            );
        });
        when(importMapper.toFileType("CSV")).thenReturn(ImportFileType.CSV);
        when(importMapper.toIssueDtos(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("upload stages file and creates PENDING batch")
    void upload() {
        MockMultipartFile file = csvFile("events.csv", sampleCsv());
        when(parserRegistry.detect(file)).thenReturn(ImportFileType.CSV);
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(inv -> {
            ImportBatch b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });

        ImportResponseDTO response = importService.upload(file);

        assertThat(response.status()).isEqualTo(ImportStatus.PENDING);
        assertThat(response.fileName()).isEqualTo("events.csv");
        verify(fileStorage).store(eq(10L), any());
        verify(importValidator).validateFilePresent(file);
    }

    @Test
    @DisplayName("preview validates without persisting events")
    void preview() {
        MockMultipartFile file = csvFile("events.csv", sampleCsv());
        ImportRawRecord record = validRecord(2);

        when(parserRegistry.detect(file)).thenReturn(ImportFileType.CSV);
        when(parserRegistry.parse(eq(ImportFileType.CSV), any(), eq("events.csv")))
                .thenReturn(List.of(record));
        when(importValidator.validateRow(record)).thenReturn(List.of());
        when(importValidator.parseTimestamp(any())).thenReturn(Instant.parse("2026-07-26T10:00:00Z"));
        when(importValidator.parseDirection(any())).thenReturn(AccessDirection.ENTRY);
        when(importValidator.parseResult(any())).thenReturn(AccessResult.AUTHORIZED);
        when(badgeRepository.findByBadgeNumberIgnoreCase("BDG-001")).thenReturn(Optional.of(badge));
        when(accessPointRepository.findByCodeIgnoreCase("AP-01")).thenReturn(Optional.of(accessPoint));
        when(accessEventRepository.existsByBadgeIdAndAccessPointIdAndEventTimestampAndDirection(
                anyLong(), anyLong(), any(), any())).thenReturn(false);

        ImportSummaryDTO summary = importService.preview(file);

        assertThat(summary.totalRecords()).isEqualTo(1);
        assertThat(summary.importedRecords()).isEqualTo(1);
        assertThat(summary.rejectedRecords()).isEqualTo(0);
        verify(accessEventRepository, never()).save(any());
        verify(importValidator).validateStructure(any());
    }

    @Test
    @DisplayName("execute imports valid rows and ignores duplicates")
    void executeWithDuplicate() {
        ImportRawRecord first = validRecord(2);
        ImportRawRecord duplicate = validRecord(3);
        byte[] content = sampleCsv().getBytes(StandardCharsets.UTF_8);

        when(importBatchRepository.findById(10L)).thenReturn(Optional.of(pendingBatch));
        when(fileStorage.get(10L)).thenReturn(Optional.of(
                new ImportFileStorage.StagedImportFile("events.csv", ImportFileType.CSV, content)));
        when(parserRegistry.parse(eq(ImportFileType.CSV), any(), eq("events.csv")))
                .thenReturn(List.of(first, duplicate));
        when(importValidator.validateRow(any())).thenReturn(List.of());
        when(importValidator.parseTimestamp(any())).thenReturn(Instant.parse("2026-07-26T10:00:00Z"));
        when(importValidator.parseDirection(any())).thenReturn(AccessDirection.ENTRY);
        when(importValidator.parseResult(any())).thenReturn(AccessResult.AUTHORIZED);
        when(badgeRepository.findByBadgeNumberIgnoreCase("BDG-001")).thenReturn(Optional.of(badge));
        when(accessPointRepository.findByCodeIgnoreCase("AP-01")).thenReturn(Optional.of(accessPoint));
        when(accessEventRepository.existsByBadgeIdAndAccessPointIdAndEventTimestampAndDirection(
                anyLong(), anyLong(), any(), any())).thenReturn(false);
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(3L)).thenReturn(Optional.empty());
        when(accessEventRepository.save(any(AccessEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportResponseDTO response = importService.execute(new ImportRequestDTO(10L));

        assertThat(response.validRecords()).isEqualTo(1);
        assertThat(response.duplicateRecords()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(ImportStatus.PARTIALLY_COMPLETED);
        verify(accessEventRepository).save(any(AccessEvent.class));
        verify(fileStorage).remove(10L);
    }

    @Test
    @DisplayName("re-executing completed batch throws DuplicateImportException")
    void duplicateExecute() {
        pendingBatch.setStatus(ImportStatus.COMPLETED);
        when(importBatchRepository.findById(10L)).thenReturn(Optional.of(pendingBatch));

        assertThatThrownBy(() -> importService.execute(new ImportRequestDTO(10L)))
                .isInstanceOf(DuplicateImportException.class);
    }

    @Test
    @DisplayName("getById throws when missing")
    void notFound() {
        when(importBatchRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> importService.getById(99L))
                .isInstanceOf(ImportBatchNotFoundException.class);
    }

    @Test
    @DisplayName("search uses specification and pagination")
    void search() {
        when(importBatchRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(pendingBatch)));

        var page = importService.search(new ImportSearchCriteria(ImportStatus.PENDING, null, null, null, null, null),
                PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("invalid structure fails import")
    void invalidStructure() {
        byte[] content = "bad".getBytes(StandardCharsets.UTF_8);
        when(importBatchRepository.findById(10L)).thenReturn(Optional.of(pendingBatch));
        when(fileStorage.get(10L)).thenReturn(Optional.of(
                new ImportFileStorage.StagedImportFile("events.csv", ImportFileType.CSV, content)));
        when(parserRegistry.parse(eq(ImportFileType.CSV), any(), eq("events.csv")))
                .thenReturn(List.of(new ImportRawRecord(2, Map.of())));
        org.mockito.Mockito.doThrow(new InvalidFileFormatException("Missing mandatory columns"))
                .when(importValidator).validateStructure(any());
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> importService.execute(new ImportRequestDTO(10L)))
                .isInstanceOf(InvalidFileFormatException.class);

        ArgumentCaptor<ImportBatch> captor = ArgumentCaptor.forClass(ImportBatch.class);
        verify(importBatchRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(b -> b.getStatus() == ImportStatus.FAILED)).isTrue();
    }

    private ImportRawRecord validRecord(int line) {
        return new ImportRawRecord(line, Map.of(
                "eventTimestamp", "2026-07-26T10:00:00Z",
                "badgeNumber", "BDG-001",
                "accessPointCode", "AP-01",
                "direction", "ENTRY",
                "result", "AUTHORIZED"
        ));
    }

    private MockMultipartFile csvFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private String sampleCsv() {
        return """
                eventTimestamp,badgeNumber,accessPointCode,direction,result
                2026-07-26T10:00:00Z,BDG-001,AP-01,ENTRY,AUTHORIZED
                """;
    }
}
