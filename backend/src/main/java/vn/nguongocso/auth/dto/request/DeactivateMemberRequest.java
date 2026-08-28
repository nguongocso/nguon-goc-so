package vn.nguongocso.auth.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu vô hiệu hóa thành viên của tổ chức hiện tại (QTN-32).
 *
 * <p>
 * Hiện chỉ yêu cầu {@code reason}; luồng chuyển giao lô
 * (replacementUserId) đã tạm gỡ bỏ vì hệ thống chưa có phân quyền
 * ghi sự kiện theo lô (D-4).
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeactivateMemberRequest {

    /**
     * Lý do vô hiệu hóa (bắt buộc, tối đa 500 ký tự — khớp
     * {@code LockAccountRequest.reason}).
     */
    @NotBlank(message = "Lý do không được để trống")
    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;}
