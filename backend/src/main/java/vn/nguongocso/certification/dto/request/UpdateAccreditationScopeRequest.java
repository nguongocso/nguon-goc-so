package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request cập nhật phạm vi công nhận của một đơn vị kiểm nghiệm.
 * <p>
 * Ngữ nghĩa REPLACE-ALL: danh sách {@code criterionDefinitionIds} là
 * toàn bộ chỉ tiêu thuộc phạm vi sau khi lưu (không phải delta).
 */
@Getter
@Setter
public class UpdateAccreditationScopeRequest {

    /**
     * Tập chỉ tiêu (id trong {@code inspection_criterion_catalog})
     * thuộc phạm vi công nhận của đơn vị kiểm nghiệm.
     */
    @NotNull(message = "Danh sách chỉ tiêu không được để trống.")
    private List<Long> criterionDefinitionIds;
}
