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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.enums.ShipmentStatus;

/**
 * Thực thể đại diện cho một lô hàng.
 */
@Getter
@Setter
@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_lot_id", nullable = false)
    private ProductionLot productionLot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_range_id")
    private CodeRange codeRange;

    /**
     * Lô nguồn đã được tách để tạo ra lô hiện tại.
     *
     * <p>Giá trị {@code null} biểu thị lô gốc; Story hiện tại chỉ hỗ trợ một cấp
     * cha - con.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_shipment_id")
    private Shipment parentShipment;

    /** Tổ chức đối tác nhận lô con sau khi tách. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_organization_id")
    private Organization recipientOrganization;

    /** Thời điểm hoàn tất thao tác tách lô. */
    @Column(name = "split_at")
    private LocalDateTime splitAt;

    /** Người thực hiện thao tác tách lô. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_by")
    private User splitBy;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_quantity", nullable = false)
    private long totalQuantity;

    @Column(name = "packaging_info")
    private String packagingInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
