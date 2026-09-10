package vn.nguongocso.trace.recall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.recall.enums.LotResolution;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kết quả xử lý thực tế của một lô hàng trong vụ việc thu hồi (NCL-08-CN-012).
 *
 * <p>Một bản ghi duy nhất cho (case, shipment) — không cho phép ghi đè lịch sử;
 * ràng buộc UNIQUE(recall_case_id, shipment_id) bảo vệ ở tầng DB.</p>
 */
@Entity
@Table(name = "recall_lot_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecallLotResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recall_case_id", nullable = false)
    private RecallCase recallCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LotResolution resolution;

    /** Số lượng thực tế thu hồi được (số thực, đơn vị lấy từ lô sản xuất). */
    @Column(name = "recovered_quantity", precision = 18, scale = 3)
    private BigDecimal recoveredQuantity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
