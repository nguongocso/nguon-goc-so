package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request cập nhật phạm vi công nhận của một đơn vị kiểm nghiệm.
 */
@Getter
@Setter
public class UpdateAccreditationScopeRequest {
    @NotNull(message = "Danh sách chỉ tiêu không được để trống.")
    private List<Long> criterionDefinitionIds;
}
