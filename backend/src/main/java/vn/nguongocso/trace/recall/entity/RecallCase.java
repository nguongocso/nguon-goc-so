package vn.nguongocso.trace.recall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.auth.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vụ việc thu hồi (NCL-08-CN-012) — gom nhiều lô hàng đã bị thu hồi của một
 * lô sản xuất để theo dõi quá trình xử lý và kết thúc.
 */
@Entity
@Table(name = "recall_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecallCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    /** Mã định danh theo dõi vụ việc (VD: RC-20260910-XXXXXX). */
    @Column(name = "case_code", nullable = false, unique = true, length = 40)
    private String caseCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_lot_id", nullable = false)
    private ProductionLot productionLot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecallCaseStatus status;

    @Column(name = "organization_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Biện pháp khắc phục / phòng ngừa (bắt buộc khi đóng vụ việc). */
    @Column(name = "corrective_measures", columnDefinition = "TEXT")
    private String remediationMeasures;

    /** Danh sách ID tệp biên bản (evidence) lưu dạng chuỗi phân tách bởi dấu phẩy. */
    @Column(name = "attachments", columnDefinition = "TEXT")
    private String evidenceFileIds;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
