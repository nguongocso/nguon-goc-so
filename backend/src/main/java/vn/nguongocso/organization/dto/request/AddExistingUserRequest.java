package vn.nguongocso.organization.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Yêu cầu thêm người dùng hiện có vào tổ chức. */
@Data
public class AddExistingUserRequest {
    @NotNull(message = "userId không được để trống")
    private UUID userId;

    private Integer roleId;
}
