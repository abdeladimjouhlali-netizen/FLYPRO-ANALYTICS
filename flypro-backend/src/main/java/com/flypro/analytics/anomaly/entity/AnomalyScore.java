package com.flypro.analytics.anomaly.entity;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Anomaly score produced by an AI / analytics algorithm for an access event.
 */
@Entity
@Table(
        name = "anomaly_scores",
        indexes = {
                @Index(name = "idx_anomaly_scores_access_event_id", columnList = "access_event_id"),
                @Index(name = "idx_anomaly_scores_algorithm", columnList = "algorithm")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AnomalyScore extends BaseAuditableEntity {

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "score", nullable = false, precision = 10, scale = 6)
    private BigDecimal score;

    @NotBlank
    @Size(max = 100)
    @Column(name = "algorithm", nullable = false, length = 100)
    private String algorithm;

    @Size(max = 100)
    @Column(name = "prediction", length = 100)
    private String prediction;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "access_event_id", nullable = false)
    private AccessEvent accessEvent;
}
