package vn.nguongocso.organization.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Phản hồi khi truy vấn danh sách người dùng có sẵn để thêm vào tổ chức. */
@Data
@Builder
public class AvailableUserResponse {
    private UUID userId;

    private String username;

    private String fullName;

    private String email;

    private String phone;

    private String currentRoleCode;

    private String currentRoleName;
}
