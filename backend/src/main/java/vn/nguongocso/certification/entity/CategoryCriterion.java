package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.farm.entity.ProductCategory;
import java.util.UUID;

/**
 * Join entity for the N-N relationship between ProductCategory and InspectionCriterionCatalog.
 * Story: NCL-09-CN-009
 */
@Entity
@Table(
    name = "category_criteria",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_category_criteria",
            columnNames = {"category_id", "criterion_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCriterion {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private InspectionCriterionCatalog criterion;
}
