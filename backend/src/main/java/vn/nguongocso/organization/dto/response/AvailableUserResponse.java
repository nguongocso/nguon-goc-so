package vn.nguongocso.organization.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phản hồi khi truy vấn danh sách người dùng có sẵn để thêm vào tổ chức.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableUserResponse {

    private UUID userId;

    private String username;

    private String fullName;

    private String email;

    private String phone;

    private String currentRoleCode;

    private String currentRoleName;
}
