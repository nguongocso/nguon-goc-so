package vn.nguongocso.trace.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.enums.CodeRangeSupplementStatus;

/**
 * Thực thể đại diện cho một yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 *
 * <p>
 * Quy trình:
 * <ol>
 *   <li>Quản lý hợp tác xã (VT-02) tạo yêu cầu (trạng thái {@code PENDING}),
 *       kèm số lượng đề nghị, lý do và bằng chứng sản lượng thực
 *       (danh sách ID sự kiện thu hoạch/sơ chế, lưu JSON).</li>
 *   <li>Quản trị viên nền tảng (VT-01) duyệt toàn bộ / duyệt một phần
 *       ({@code APPROVED}, tăng {@code totalLimit} của dải mã hiện có)
 *       hoặc từ chối kèm lý do ({@code REJECTED}).</li>
 * </ol>
 *
 * <p>
 * Mỗi tổ chức chỉ được có tối đa một yêu cầu {@code PENDING} tại một thời điểm.
 */
@Entity
@Table(name = "code_range_supplement_requests")
@Getter
@Setter
@NoArgsConstructor
public class CodeRangeSupplementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "requested_quantity", nullable = false)
    private Long requestedQuantity;

    @Column(name = "approved_quantity")
    private Long approvedQuantity;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /**
     * Danh sách ID sự kiện thu hoạch (HARVEST) / sơ chế (PREPROCESSING)
     * làm bằng chứng sản lượng thực, lưu dạng JSON array.
     */
    @Column(name = "evidence_event_ids", nullable = false, columnDefinition = "TEXT")
    private String evidenceEventIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CodeRangeSupplementStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_remarks", columnDefinition = "TEXT")
    private String approvalRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = CodeRangeSupplementStatus.PENDING;
        }
        if (requestedAt == null) {
            requestedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
