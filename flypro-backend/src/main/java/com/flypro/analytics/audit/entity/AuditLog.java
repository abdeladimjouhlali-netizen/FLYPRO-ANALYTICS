package com.flypro.analytics.audit.entity;

import com.flypro.analytics.common.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Immutable-style audit trail entry for sensitive actions.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_username", columnList = "username"),
                @Index(name = "idx_audit_logs_entity", columnList = "entity_name, entity_id"),
                @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuditLog extends BaseAuditableEntity {

    @NotBlank
    @Size(max = 100)
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @NotBlank
    @Size(max = 150)
    @Column(name = "entity_name", nullable = false, length = 150)
    private String entityName;

    @Size(max = 100)
    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Size(max = 100)
    @Column(name = "username", length = 100)
    private String username;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}
