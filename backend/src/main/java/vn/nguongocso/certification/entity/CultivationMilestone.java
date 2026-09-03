package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.farm.entity.ProductCategory;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Khai báo mốc canh tác (bảng hợp nhất).
 * - productCategoryId NULL = áp dụng cho toàn bộ loại nông sản
 * - standardId NULL        = áp dụng cho mọi tiêu chuẩn
 * - isMandatory            = mốc bắt buộc (thay "ngừng sử dụng")
 * Story: NCL-09-CN-011
 */
@Entity
@Table(
    name = "cultivation_milestone",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_milestone_name_cat_std",
            columnNames = {"product_category_id", "standard_id", "name_key"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CultivationMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;

    @Column(name = "expected_days_from_planting")
    private Integer expectedDaysFromPlanting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id")
    private ProductCategory productCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id")
    private Standard standard;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
