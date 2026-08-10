package vn.nguongocso.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request dùng để lựa chọn tổ chức sau khi
 * người dùng đã đăng nhập bằng username/password.
 */
@Getter
@Setter
@NoArgsConstructor
public class SelectOrganizationRequest {

    /**
     * ID của tổ chức mà người dùng muốn truy cập.
     */
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
}