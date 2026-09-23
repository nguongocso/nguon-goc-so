package vn.nguongocso.integration.partner.entity;

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

import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Thực thể lưu trữ nhật ký đối tác bên thứ ba truy xuất dữ liệu của lô.
*/
@Entity
@Table(name = "partner_lot_access_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerLotAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_api_key_id", nullable = false)
    private PartnerApiKey partnerApiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_lot_id")
    private ProductionLot productionLot;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    /**
     * Thiết lập thời điểm truy xuất trước khi lưu mới.
     */
    @PrePersist
    protected void onCreate() {
        if (accessedAt == null) {
            accessedAt = LocalDateTime.now();
        }
    }
}
