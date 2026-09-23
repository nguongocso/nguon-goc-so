package vn.nguongocso.organization.dto.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Request cập nhật mapping tổ chức và đơn vị hành chính. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationDivisionsRequest {
    private UUID provinceId;

    private UUID communeId;
}
