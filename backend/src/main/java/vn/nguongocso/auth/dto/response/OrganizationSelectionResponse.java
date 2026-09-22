package vn.nguongocso.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.nguongocso.organization.enums.OrganizationType;

/**
 * Response chứa thông tin organization mà user có thể lựa chọn sau khi đăng nhập.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSelectionResponse {
    private String organizationId;

    private String organizationCode;

    private String organizationName;

    private OrganizationType organizationType;

    private String roleCode;

    private String roleName;
}