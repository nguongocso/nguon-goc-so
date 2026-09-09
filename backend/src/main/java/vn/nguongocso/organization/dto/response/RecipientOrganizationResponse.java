package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;

/**
 * Thông tin tổ chức nhận cho dropdown phiếu bàn giao.
 * Chỉ gồm các tổ chức ACTIVE và khác tổ chức hiện tại.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipientOrganizationResponse {
    private UUID id;

    private String name;

    private String code;

    private OrganizationType type;

    private OrganizationStatus status;

    private LocalDateTime createdAt;
}
