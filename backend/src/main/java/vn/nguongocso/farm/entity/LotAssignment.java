package vn.nguongocso.farm.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;

/**
 * Bản ghi phân công thành viên phụ trách một lô sản xuất.
 *
 * <p>
 * Chuyển giao phân công không xóa bản ghi cũ: bản ghi cũ chỉ được
 * vô hiệu hóa ({@code active = FALSE} kèm {@code releasedAt/releasedBy})
 * và tạo bản ghi mới cho người thay thế, nhằm bảo toàn lịch sử phân công
 * (QTN-32 mục 5 — dữ liệu đã ghi không bị xóa).
 * </p>
 */
@Entity
@Table(name = "lot_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotAssignment {

    @Id
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    /** Lô sản xuất được phân công. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ProductionLot productionLot;

    /** Thành viên được phân công phụ trách lô. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tổ chức của phân công (denormalize để truy vấn scope nhanh). */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Phân công còn hiệu lực. {@code FALSE} khi thành viên rời lô
     * (chuyển giao, hủy phân công) — bản ghi vẫn giữ lại làm lịch sử.
     */
    @Column(nullable = false)
    private Boolean active;

    /** Người thao tác gán/chuyển giao (nullable). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    /** Thời điểm gán. */
    @Column(nullable = false, name = "assigned_at")
    private LocalDateTime assignedAt;

    /** Thời điểm kết thúc phân công (nullable — chỉ set khi release). */
    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    /** Người thao tác kết thúc phân công (nullable). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private User releasedBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = Boolean.TRUE;
        }
    }
}
