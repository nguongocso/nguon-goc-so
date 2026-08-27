package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Request DTO for assigning criteria to a product category.
 * Story: NCL-09-CN-009
 */
@Getter
@Setter
public class CategoryCriteriaRequest {

    @NotNull(message = "Danh sách chỉ tiêu không được rỗng (truyền mảng rỗng nếu muốn xóa tất cả)")
    private List<Long> criterionIds;
}
