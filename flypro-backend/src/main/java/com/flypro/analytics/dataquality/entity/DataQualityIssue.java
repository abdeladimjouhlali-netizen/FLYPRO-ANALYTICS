package com.flypro.analytics.dataquality.entity;

import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Data quality issue detected during an import batch.
 */
@Entity
@Table(
        name = "data_quality_issues",
        indexes = {
                @Index(name = "idx_data_quality_issues_import_batch_id", columnList = "import_batch_id"),
                @Index(name = "idx_data_quality_issues_severity", columnList = "severity")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DataQualityIssue extends BaseAuditableEntity {

    @Column(name = "line_number")
    private Integer lineNumber;

    @Size(max = 100)
    @Column(name = "field_name", length = 100)
    private String fieldName;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private AlertSeverity severity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_batch_id", nullable = false)
    private ImportBatch importBatch;
}
