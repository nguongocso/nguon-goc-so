package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.entity.ProductionLot;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp ProductionLotCertification đại diện cho chứng nhận gắn vào lô sản xuất
 * trong hệ thống.
 */
@Entity
@Table(name = "production_lot_certifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionLotCertification {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_lot_id", nullable = false)
    private ProductionLot productionLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private LocalDateTime attachedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attached_by", nullable = false)
    private User attachedBy;

    @Column(name = "note")
    private String note;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (attachedAt == null) {
            attachedAt = LocalDateTime.now();
        }
    }
}