package com.flypro.analytics.badge.entity;

import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Physical or virtual access badge.
 */
@Entity
@Table(
        name = "badges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_badges_badge_number", columnNames = "badge_number")
        },
        indexes = {
                @Index(name = "idx_badges_badge_number", columnList = "badge_number"),
                @Index(name = "idx_badges_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Badge extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 100)
    @Column(name = "badge_number", nullable = false, length = 100)
    private String badgeNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BadgeStatus status;

    @NotNull
    @PastOrPresent
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private BadgeType type;

    @OneToMany(mappedBy = "badge", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<BadgeAssignment> assignments = new ArrayList<>();
}
