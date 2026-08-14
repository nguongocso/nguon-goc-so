package vn.nguongocso.permission.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO cho một quyền.
 */
@Getter
@Builder
public class PermissionItemResponse {
    private Integer permissionId;

    private String action;

    private String description;

    private Boolean isEnabled;

    private Boolean isDefault;
}