package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.farm.entity.ProductCategory;
import java.util.UUID;

/**
 * Join entity for the N-N relationship between ProductCategory and CultivationMilestoneCatalog,
 * with optional Standard scope.
 * Story: NCL-09-CN-011
 */
@Entity
@Table(
    name = "product_category_milestones",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_category_milestone_standard",
            columnNames = {"category_id", "milestone_id", "standard_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryMilestone {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private CultivationMilestoneCatalog milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id")
    private Standard standard;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = true;
}
