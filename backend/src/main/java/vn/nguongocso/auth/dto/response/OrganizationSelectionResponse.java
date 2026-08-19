package vn.nguongocso.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.nguongocso.organization.enums.OrganizationType;

/**
 * Response chứa thông tin organization mà user
 * có thể lựa chọn sau khi đăng nhập.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSelectionResponse {

    /**
     * ID của organization.
     */
    private String organizationId;

    /**
     * Mã organization.
     */
    private String organizationCode;

    /**
     * Tên organization.
     */
    private String organizationName;

    /**
     * Loại organization.
     */
    private OrganizationType organizationType;

    /**
     * Role của user trong organization.
     */
    private String roleCode;

    /**
     * Tên role của user trong organization.
     */
    private String roleName;
}