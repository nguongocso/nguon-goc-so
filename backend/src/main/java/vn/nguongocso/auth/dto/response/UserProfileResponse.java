package vn.nguongocso.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.organization.enums.OrganizationType;

import java.util.UUID;
import java.util.List;

/**
 * Response thông tin hồ sơ người dùng.
 */
@Data
@Builder
public class UserProfileResponse {
    private UUID userId;

    private String username;

    private String fullName;

    private String phone;

    private String email;

    private String roleCode;

    private String roleName;

    private UUID organizationId;

    private String organizationCode;

    private String organizationName;

    private OrganizationType organizationType;

    private List<String> permissions;
}
