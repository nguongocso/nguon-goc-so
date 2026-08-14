package vn.nguongocso.organization.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phản hồi khi truy vấn thông tin hồ sơ tổ chức.
 */
@Data
@Builder
public class OrganizationProfileResponse {
    private UUID organizationId;

    private String name;

    private String code;

    private OrganizationType type;

    private OrganizationStatus status;

    private String address;

    private String phone;

    private String email;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
