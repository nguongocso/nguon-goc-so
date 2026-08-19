package vn.nguongocso.integration.partner.dto.response;

import java.util.List;

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
public class PartnerLotDossierResponse {
    private PartnerLotInfoResponse lotInfo;
    private PartnerOrgInfoResponse organizationInfo;
    private PartnerFarmAreaResponse farmAreaInfo;
    private List<PartnerCertificationResponse> certifications;
    private PartnerFarmLogSummaryResponse farmLogSummary;
}
