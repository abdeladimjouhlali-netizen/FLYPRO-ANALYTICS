package com.flypro.analytics.importbatch.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.importbatch.dto.ImportRequestDTO;
import com.flypro.analytics.importbatch.dto.ImportResponseDTO;
import com.flypro.analytics.importbatch.dto.ImportSearchCriteria;
import com.flypro.analytics.importbatch.dto.ImportSummaryDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Application service contract for the Import Management module.
 */
public interface ImportService {

    /**
     * Upload and stage a file; creates a PENDING import batch.
     */
    ImportResponseDTO upload(MultipartFile file);

    /**
     * Validate a file without persisting access events.
     */
    ImportSummaryDTO preview(MultipartFile file);

    /**
     * Validate a previously uploaded batch without persisting access events.
     */
    ImportSummaryDTO preview(Long batchId);

    /**
     * Execute import for a staged batch.
     */
    ImportResponseDTO execute(ImportRequestDTO request);

    /**
     * Upload and execute in a single step.
     */
    ImportResponseDTO execute(MultipartFile file);

    PageResponse<ImportResponseDTO> search(ImportSearchCriteria criteria, Pageable pageable);

    ImportResponseDTO getById(Long id);

    ImportSummaryDTO getReport(Long id);
}
