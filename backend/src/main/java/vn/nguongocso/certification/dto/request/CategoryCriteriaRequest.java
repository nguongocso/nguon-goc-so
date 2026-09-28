package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * DTO request gán chỉ tiêu vào loại nông sản.
 */
@Getter
@Setter
public class CategoryCriteriaRequest {
    @NotNull(message = "Danh sách chỉ tiêu không được rỗng (truyền mảng rỗng nếu muốn xóa tất cả)")
    private List<Long> criterionIds;
}
