package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;

/**
 * Phản hồi khi truy vấn thông tin hồ sơ tổ chức.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationProfileResponse {

    private UUID organizationId;

    private String name;

    private String code;

    private OrganizationType type;

    private OrganizationStatus status;

    private String address;

    /** ID đơn vị hành chính cấp tỉnh/thành phố. */
    private UUID provinceId;

    /** Tên đơn vị hành chính cấp tỉnh/thành phố. */
    private String provinceName;

    /** ID đơn vị hành chính cấp xã/phường. */
    private UUID communeId;

    /** Tên đơn vị hành chính cấp xã/phường. */
    private String communeName;

    private String phone;

    private String email;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
