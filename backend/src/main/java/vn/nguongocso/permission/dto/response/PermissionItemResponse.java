package vn.nguongocso.permission.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO cho một quyền.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionItemResponse {

    private Integer permissionId;

    private String action;

    private String description;

    private Boolean isEnabled;

    private Boolean isDefault;
}
