package com.flypro.analytics.dataquality.repository;

import com.flypro.analytics.dataquality.entity.DataQualityIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence port for {@link DataQualityIssue}.
 */
public interface DataQualityIssueRepository extends JpaRepository<DataQualityIssue, Long> {

    List<DataQualityIssue> findByImportBatchIdOrderByLineNumberAscIdAsc(Long importBatchId);
}
