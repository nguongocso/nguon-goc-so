package vn.nguongocso.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.auth.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response thông tin người dùng trong tổ chức.
 */
@Data
@Builder
public class OrganizationUserResponse {
    private UUID id;

    private UUID organizationId;

    private UUID userId;

    private String username;

    private String fullName;

    private String email;

    private String phone;

    private Integer roleId;

    private String roleCode;

    private String roleName;

    private UserStatus status;

    private LocalDateTime joinedAt;
}
