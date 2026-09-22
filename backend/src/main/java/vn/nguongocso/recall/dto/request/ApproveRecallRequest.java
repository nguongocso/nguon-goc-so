package vn.nguongocso.recall.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Yêu cầu duyệt thu hồi lô sản xuất. */
@Getter
@Setter
@NoArgsConstructor
public class ApproveRecallRequest {

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự.")
    private String remarks;
}