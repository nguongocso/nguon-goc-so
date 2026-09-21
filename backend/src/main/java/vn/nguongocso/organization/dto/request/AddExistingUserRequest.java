package vn.nguongocso.organization.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu thêm người dùng hiện có vào tổ chức.
 */
@Getter
@Setter
public class AddExistingUserRequest {

    @NotNull(message = "userId không được để trống")
    private UUID userId;

    private Integer roleId;
}
