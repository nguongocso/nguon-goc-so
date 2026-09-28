package vn.nguongocso.integration.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO thông tin tổ chức trong hồ sơ truy xuất của đối tác.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerOrgInfoResponse {
    private String organizationId;

    private String organizationName;

    private String organizationCode;

    private String address;

    private String phone;

    private String email;
}
