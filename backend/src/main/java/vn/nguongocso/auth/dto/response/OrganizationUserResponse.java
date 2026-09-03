package vn.nguongocso.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.organization.enums.OrganizationUserStatus;

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

    /**
     * Trạng thái membership trong tổ chức hiện tại
     * ({@code organization_users.status}: ACTIVE/INACTIVE).
     *
     * <p>
     * Khác với {@code status} (trạng thái toàn cục của tài khoản
     * {@code users.status}) — một tài khoản có thể INACTIVE membership ở
     * tổ chức này nhưng vẫn ACTIVE ở tổ chức khác.
     * </p>
     */
    private OrganizationUserStatus membershipStatus;

    private LocalDateTime joinedAt;
}
