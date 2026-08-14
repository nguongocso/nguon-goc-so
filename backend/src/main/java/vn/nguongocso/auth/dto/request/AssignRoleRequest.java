package vn.nguongocso.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Yêu cầu gán vai trò cho người dùng.
 */
@Data
public class AssignRoleRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role ID is required")
    private Integer roleId;
}
