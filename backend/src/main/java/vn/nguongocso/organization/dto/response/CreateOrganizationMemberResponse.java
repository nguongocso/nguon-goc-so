package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.enums.UserStatus;

/**
 * Phản hồi khi tạo thành viên tổ chức.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationMemberResponse {
    private UUID id;

    private String username;

    private String fullName;

    private String email;

    private String phone;

    private String roleCode;

    private String roleName;

    private UserStatus status;

    private LocalDateTime joinedAt;
}
