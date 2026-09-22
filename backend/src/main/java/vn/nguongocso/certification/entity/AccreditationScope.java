package vn.nguongocso.certification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phạm vi công nhận của một đơn vị kiểm nghiệm (NCL-11-CN-006 Phase 2).
 */
@Entity
@Table(name = "accreditation_scopes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_accreditation_scope_unit_criterion", columnNames = { "testing_unit_id",
                "criterion_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccreditationScope {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "testing_unit_id", nullable = false)
    private TestingUnit testingUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private InspectionCriterionCatalog criterion;

    @Column(name = "criterion_code", nullable = false, length = 150)
    private String criterionCode;

    @Column(name = "criterion_name", nullable = false, length = 150)
    private String criterionName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
