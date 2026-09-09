package vn.nguongocso.recall.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Thực thể đại diện cho chi tiết lô hàng trong yêu cầu thu hồi hàng loạt (NCL-08-CN-011).
 *
 * <p>
 * Mỗi bản ghi ghi nhận:
 * <ul>
 *   <li>Lô hàng thuộc yêu cầu thu hồi</li>
 *   <li>Có được bao gồm trong phạm vi thu hồi hay không</li>
 *   <li>Lý do loại bỏ (nếu có)</li>
 * </ul>
 */
@Entity
@Table(name = "bulk_recall_shipments")
@Getter
@Setter
@NoArgsConstructor
public class BulkRecallShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bulk_recall_request_id", nullable = false)
    private BulkRecallRequest bulkRecallRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    /**
     * Đánh dấu lô hàng có được bao gồm trong phạm vi thu hồi hay không.
     * true = included (sẽ bị thu hồi khi yêu cầu được duyệt)
     * false = excluded (bị loại khỏi phạm vi, phải có exclusionReason)
     */
    @Column(name = "included", nullable = false)
    private boolean included;

    /**
     * Lý do loại bỏ lô hàng khỏi phạm vi thu hồi.
     * Bắt buộc khi included = false.
     */
    @Column(name = "exclusion_reason", columnDefinition = "TEXT")
    private String exclusionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
