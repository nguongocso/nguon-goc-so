package vn.nguongocso.integration.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO thông tin vùng canh tác trong hồ sơ truy xuất của đối tác.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerFarmAreaResponse {
    private String farmAreaId;

    private String farmAreaName;

    private Double area;

    private String areaUnit;
}
