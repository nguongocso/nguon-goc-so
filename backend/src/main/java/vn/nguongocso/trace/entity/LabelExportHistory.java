package vn.nguongocso.trace.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;

/**
 * Entity lịch sử xuất tem QR cho lô hàng (NCL-04-CN-005).
 *
 * <p>
 * Mỗi bản ghi tương ứng một lượt xuất file PDF tem QR: ai xuất, khi nào,
 * khoảng mã nào (startIndex → endIndex), số lượng và khổ tem.
 * </p>
 */
@Table(name = "label_export_history")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelExportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    /** Lô hàng được xuất tem. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    /** Người thực hiện xuất tem. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exported_by", nullable = false)
    private User exportedBy;

    /** Tổ chức của lô hàng (QTN-01). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Thời điểm xuất. */
    @Column(name = "exported_at", nullable = false)
    private LocalDateTime exportedAt;

    /** Chỉ số bắt đầu trong danh sách mã đã sinh (bắt đầu từ 0). */
    @Column(name = "start_index", nullable = false)
    private int startIndex;

    /** Chỉ số kết thúc (bao gồm). */
    @Column(name = "end_index", nullable = false)
    private int endIndex;

    /** Số tem xuất. */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Khổ tem đã chọn (ví dụ "40x30"). */
    @Column(name = "label_size", nullable = false, length = 20)
    private String labelSize;
}
