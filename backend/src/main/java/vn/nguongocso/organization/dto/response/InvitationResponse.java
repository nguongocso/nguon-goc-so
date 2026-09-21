package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phản hồi khi truy vấn thông tin lời mời tham gia tổ chức.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

    private UUID id;

    private String email;

    private UUID organizationId;

    private String organizationName;

    private Integer roleId;

    private String roleName;

    private String status;

    private String token;

    private String joinUrl;

    private LocalDateTime expiryDate;

    private UUID createdBy;

    private LocalDateTime createdAt;
}
