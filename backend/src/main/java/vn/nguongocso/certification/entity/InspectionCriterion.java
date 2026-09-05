package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Thực thể chỉ tiêu kiểm nghiệm.
 * Một yêu cầu kiểm nghiệm có thể có nhiều chỉ tiêu.
 */
@Entity
@Table(
    name = "inspection_criteria",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inspection_request_criterion",
            columnNames = {"inspection_request_id", "criterion_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionCriterion {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_request_id", nullable = false)
    private InspectionRequest inspectionRequest;

    /**
     * Mã chỉ tiêu.
     * Ví dụ: PESTICIDE_RESIDUE, HEAVY_METAL...
     */
    @Column(name = "criterion_code", nullable = false, length = 100)
    private String criterionCode;

    /**
     * Tên chỉ tiêu hiển thị.
     */
    @Column(name = "criterion_name", nullable = false, length = 255)
    private String criterionName;

    /**
     * Tiêu chuẩn làm căn cứ cho chỉ tiêu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id")
    private Standard standard;

    /**
     * Tham chiếu đến chỉ tiêu gốc trong danh mục (nullable để không hồi tố dữ liệu cũ).
     * Story: NCL-09-CN-009 BR-5, BR-7.
     */
    @Column(name = "criterion_id")
    private Long criterionId;
}