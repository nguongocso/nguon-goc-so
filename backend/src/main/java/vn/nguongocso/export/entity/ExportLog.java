package vn.nguongocso.export.entity;

import java.time.LocalDateTime;
import java.util.UUID;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Thực thể ghi nhận nhật ký xuất hồ sơ (ExportLog).
 * Lưu vết shipment, mẫu hồ sơ đã áp dụng, người xuất và thời điểm xuất.
 */
@Entity
@Table(name = "export_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportLog {

    /** Khóa chính của bản ghi nhật ký */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Lô hàng được xuất hồ sơ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    /** Mẫu hồ sơ đã sử dụng (null nếu dùng mặc định không lưu template) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ProfileTemplate template;

    /** Người thực hiện thao tác xuất */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exported_by", nullable = false)
    private User exportedBy;

    /** Thời điểm xuất hồ sơ */
    @Column(name = "exported_at", nullable = false)
    private LocalDateTime exportedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.exportedAt == null) {
            this.exportedAt = LocalDateTime.now();
        }
    }
}
