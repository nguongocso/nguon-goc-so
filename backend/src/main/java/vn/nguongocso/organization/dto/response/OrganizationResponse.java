package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;

/** Phản hồi khi truy vấn thông tin tổ chức. */
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
