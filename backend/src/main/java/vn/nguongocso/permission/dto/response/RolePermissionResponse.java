package vn.nguongocso.permission.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response cấu hình quyền của một vai trò trong một tổ chức.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionResponse {

    private UUID organizationId;

    private Integer roleId;

    private String roleCode;

    private String roleName;

    private List<RolePermissionGroupResponse> groups;
}
