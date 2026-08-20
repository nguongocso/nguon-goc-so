package vn.nguongocso.recall.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload duyệt yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 * Body có thể rỗng hoặc chứa ghi chú (remarks) tùy chọn.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApproveRecallRequest {

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự.")
    private String remarks;
}