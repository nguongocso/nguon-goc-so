package vn.nguongocso.certification.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Thực thể kết quả kiểm nghiệm cho từng chỉ tiêu.
 * Lưu trữ kết quả (đạt/không đạt), ngày cấp, ngày hết hiệu lực.
 */
@Entity
@Table(
    name = "inspection_criterion_results",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inspection_criterion_result",
            columnNames = "inspection_criterion_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionCriterionResult {

    /**
     * ID duy nhất của kết quả kiểm nghiệm.
     */
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private java.util.UUID id = java.util.UUID.randomUUID();

    /**
     * Chỉ tiêu kiểm nghiệm tương ứng với kết quả này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_criterion_id", nullable = false)
    private InspectionCriterion inspectionCriterion;

        /**
     * Ngày cấp kết quả kiểm nghiệm.
     * Có thể null khi chỉ tiêu không đạt (passed = false).
     */
    @Column(name = "result_date")
    private LocalDate resultDate;

    /**
     * Ngày hết hiệu lực của kết quả kiểm nghiệm.
     * Có thể null khi chỉ tiêu không đạt (passed = false).
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /**
     * Kết quả kiểm nghiệm: true = đạt, false = không đạt.
     */
    @Column(name = "passed", nullable = false)
    private Boolean passed;

    /**
     * Đường dẫn tập tin phiếu kết quả kiểm nghiệm.
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * Người nhập/tạo kết quả kiểm nghiệm.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
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
