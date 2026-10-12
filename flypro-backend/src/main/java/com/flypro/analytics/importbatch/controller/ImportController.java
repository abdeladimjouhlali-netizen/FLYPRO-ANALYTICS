package com.flypro.analytics.importbatch.controller;

import com.flypro.analytics.common.enums.ImportStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.importbatch.dto.ImportRequestDTO;
import com.flypro.analytics.importbatch.dto.ImportResponseDTO;
import com.flypro.analytics.importbatch.dto.ImportSearchCriteria;
import com.flypro.analytics.importbatch.dto.ImportSummaryDTO;
import com.flypro.analytics.importbatch.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/**
 * REST API for access-event file import (CSV / Excel / JSON).
 */
@RestController
@RequestMapping("/api/v1/imports")
@Tag(name = "Import Management", description = "Import access events from CSV, Excel (.xlsx) or JSON files")
@SecurityRequirement(name = "bearerAuth")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Upload import file",
            description = "Stages the file and creates a PENDING import batch. Supported: .csv, .xlsx, .json"
    )
    public ResponseEntity<ApiResponse<ImportResponseDTO>> upload(
            @Parameter(description = "Import file", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        ImportResponseDTO response = importService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Import file uploaded successfully", response));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Preview import",
            description = "Validates the file and returns a summary without creating access events"
    )
    public ResponseEntity<ApiResponse<ImportSummaryDTO>> previewFile(
            @Parameter(description = "Import file", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Import preview generated successfully",
                importService.preview(file)
        ));
    }

    @PostMapping("/preview/{batchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Preview staged import batch")
    public ResponseEntity<ApiResponse<ImportSummaryDTO>> previewBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Import preview generated successfully",
                importService.preview(batchId)
        ));
    }

    @PostMapping(value = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Execute staged import")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ImportRequestDTO.class),
                    examples = @ExampleObject(name = "Execute batch", value = "{\"batchId\": 1}")
            )
    )
    public ResponseEntity<ApiResponse<ImportResponseDTO>> execute(
            @Valid @RequestBody ImportRequestDTO request
    ) {
        ImportResponseDTO response = importService.execute(request);
        return ResponseEntity.ok(ApiResponse.ok("Import executed successfully", response));
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Upload and execute import",
            description = "Creates a batch, validates and imports access events in one step"
    )
    public ResponseEntity<ApiResponse<ImportResponseDTO>> executeFile(
            @Parameter(description = "Import file", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        ImportResponseDTO response = importService.execute(file);
        return ResponseEntity.ok(ApiResponse.ok("Import executed successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Search import history", description = "Paginated list with filters on status, file name, date and user")
    public ResponseEntity<ApiResponse<PageResponse<ImportResponseDTO>>> search(
            @RequestParam(required = false) ImportStatus status,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant importDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant importDateTo,
            @Parameter(description = "Filter by importing user (createdBy)")
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ImportSearchCriteria criteria = new ImportSearchCriteria(
                status, fileName, importDateFrom, importDateTo, user, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Import batches retrieved successfully",
                importService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Get import batch details")
    public ResponseEntity<ApiResponse<ImportResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(importService.getById(id)));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ANALYST')")
    @Operation(summary = "Download validation report", description = "Returns import statistics, errors and warnings")
    public ResponseEntity<ApiResponse<ImportSummaryDTO>> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Import report retrieved successfully",
                importService.getReport(id)
        ));
    }
}
