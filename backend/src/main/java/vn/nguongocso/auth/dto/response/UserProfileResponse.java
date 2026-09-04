package vn.nguongocso.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.organization.enums.OrganizationType;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Response thông tin hồ sơ người dùng (NCL-01-CN-010).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;

    private UUID userId;

    private String username;

    private String fullName;

    private String phone;

    private String email;

    private String avatarUrl;

    private String roleCode;

    private String roleName;

    private UUID organizationId;

    private String organizationCode;

    private String organizationName;

    private OrganizationType organizationType;

    private List<String> permissions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

