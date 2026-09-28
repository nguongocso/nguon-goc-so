package vn.nguongocso.certification.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.enums.InspectionResultEntrySource;

/**
 * Thực thể kết quả kiểm nghiệm cho từng chỉ tiêu.
 */
@Entity
@Table(name = "inspection_criterion_results", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inspection_criterion_result", columnNames = "inspection_criterion_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionCriterionResult {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_criterion_id", nullable = false)
    private InspectionCriterion inspectionCriterion;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_source", nullable = false, length = 32)
    @Builder.Default
    private InspectionResultEntrySource entrySource = InspectionResultEntrySource.COOPERATIVE_MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portal_link_id")
    private InspectionResultEntryLink portalLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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
