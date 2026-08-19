package vn.nguongocso.permission.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Response cấu hình quyền của một vai trò trong một tổ chức.
 */
@Getter
@Builder
public class RolePermissionResponse {
    private UUID organizationId;

    private Integer roleId;

    private String roleCode;

    private String roleName;

    private List<RolePermissionGroupResponse> groups;
}