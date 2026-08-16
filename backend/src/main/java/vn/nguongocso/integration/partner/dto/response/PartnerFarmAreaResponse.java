package vn.nguongocso.integration.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
