package vn.nguongocso.organization.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Phản hồi khi người dùng chấp nhận lời mời tham gia tổ chức.
 */
@Getter
@Setter
@Builder
public class AcceptInvitationResponse {
    private UUID userId;

    private String userName;

    private String fullName;

    private UUID organizationId;

    private String organizationName;

    private String roleCode;
}
