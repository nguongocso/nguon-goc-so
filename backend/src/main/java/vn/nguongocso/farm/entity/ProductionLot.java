package vn.nguongocso.farm.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.organization.entity.Organization;

/**
 * Lô sản xuất nông sản trong trang trại.
*/
@Entity
@Table(name = "production_lot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_area_id")
    private FarmArea farmArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id", nullable = false)
    private ProductCategory productCategory;

    @Column(nullable = false)
    private String name;

    @Column(name = "expected_quantity", nullable = false)
    private Double expectedQuantity;

    @Column(name = "expected_quantity_unit", length = 20)
    private String expectedQuantityUnit;

    @Column(name = "actual_quantity")
    private Double actualQuantity;

    @Column(name = "planting_date")
    private LocalDate plantingDate;

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductionLotStatus status;

    @Column(name = "approval_notes")
    private String approvalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancellation_reason", length = 100)
    private String cancellationReason;

    @Column(name = "cancellation_note", length = 1000)
    private String cancellationNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "disposal_reason", length = 100)
    private String disposalReason;

    @Column(name = "handling_measure", length = 1000)
    private String handlingMeasure;

    @Column(name = "disposal_note", length = 1000)
    private String disposalNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposed_by")
    private User disposedBy;

    @Column(name = "disposed_at")
    private LocalDateTime disposedAt;

    @OneToMany(mappedBy = "productionLot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductionLotCertification> certifications = new ArrayList<>();

    /** Khởi tạo thời điểm tạo, cập nhật và trạng thái mặc định trước khi lưu mới. */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = ProductionLotStatus.DRAFT;
        }
    }

    /** Cập nhật thời điểm sửa đổi trước khi lưu bản ghi hiện có. */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
