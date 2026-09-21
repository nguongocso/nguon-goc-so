package vn.nguongocso.permission.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO cho một nhóm quyền của vai trò.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionGroupResponse {

    private String resource;

    private String resourceLabel;

    private List<PermissionItemResponse> permissions;
}
