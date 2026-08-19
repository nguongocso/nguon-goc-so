package vn.nguongocso.permission.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO cho một nhóm quyền của vai trò.
 */
@Getter
@Builder
public class RolePermissionGroupResponse {
    private String resource;

    private String resourceLabel;

    private List<PermissionItemResponse> permissions;
}