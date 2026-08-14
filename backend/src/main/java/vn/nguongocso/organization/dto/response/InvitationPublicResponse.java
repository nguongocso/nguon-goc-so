package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Phản hồi khi truy vấn thông tin lời mời tham gia tổ chức.
 */
@Getter
@Setter
@Builder
public class InvitationPublicResponse {
    private String email;

    private String organizationName;

    private String roleName;

    private String status;

    private LocalDateTime expiryDate;

    private boolean isExistingUser;
}
