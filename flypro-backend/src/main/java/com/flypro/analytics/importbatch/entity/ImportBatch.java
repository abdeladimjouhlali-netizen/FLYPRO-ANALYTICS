package com.flypro.analytics.importbatch.entity;

import com.flypro.analytics.common.enums.ImportStatus;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.dataquality.entity.DataQualityIssue;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch import job for CSV/JSON access or master data.
 */
@Entity
@Table(name = "import_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ImportBatch extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Size(max = 100)
    @Column(name = "source", length = 100)
    private String source;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ImportStatus status;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "valid_records", nullable = false)
    private int validRecords;

    @Column(name = "invalid_records", nullable = false)
    private int invalidRecords;

    @Column(name = "duplicate_records", nullable = false)
    private int duplicateRecords;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @OneToMany(mappedBy = "importBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DataQualityIssue> dataQualityIssues = new ArrayList<>();
}
