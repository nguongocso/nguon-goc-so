package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Phản hồi khi truy vấn thông tin tổ chức.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    private UUID organizationID;

    private String organizationName;

    private String organizationCode;

    private OrganizationType organizationType;

    private OrganizationStatus status;

    private LocalDateTime createdAt;
}