package vn.nguongocso.permission.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO để cập nhật quyền cho một vai trò.
 */
@Getter
@Setter
public class UpdateRolePermissionRequest {
    @NotEmpty(message = "Danh sách quyền không được để trống.")
    @Valid
    private List<PermissionToggle> permissions;

    /**
     * Lớp con đại diện cho trạng thái bật/tắt của một quyền.
     */
    @Getter
    @Setter
    public static class PermissionToggle {

        @NotNull(message = "Permission ID không được để trống.")
        private Integer permissionId;

        @NotNull(message = "Trạng thái quyền không được để trống.")
        private Boolean isEnabled;
    }
}