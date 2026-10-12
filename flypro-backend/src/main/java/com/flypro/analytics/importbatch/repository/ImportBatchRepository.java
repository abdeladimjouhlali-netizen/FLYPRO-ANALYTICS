package com.flypro.analytics.importbatch.repository;

import com.flypro.analytics.importbatch.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence port for {@link ImportBatch}.
 */
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long>,
        JpaSpecificationExecutor<ImportBatch> {
}
