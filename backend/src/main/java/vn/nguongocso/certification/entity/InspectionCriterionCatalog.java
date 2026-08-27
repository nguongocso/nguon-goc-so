package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Master catalog of inspection criteria.
 * Story: NCL-09-CN-009
 */
@Entity
@Table(
    name = "inspection_criterion_catalog",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_criterion_name_standard",
            columnNames = {"name", "reference_standard"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionCriterionCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "max_threshold", nullable = false, precision = 12, scale = 4)
    private BigDecimal maxThreshold;

    @Column(name = "reference_standard", length = 150)
    private String referenceStandard;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
