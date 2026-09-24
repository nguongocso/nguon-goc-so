package vn.nguongocso.integration.partner.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO hồ sơ truy xuất lô sản xuất dành cho bên thứ ba.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerLotDossierResponse {
    private PartnerLotInfoResponse lotInfo;

    private PartnerOrgInfoResponse organizationInfo;

    private PartnerFarmAreaResponse farmAreaInfo;

    private List<PartnerCertificationResponse> certifications;

    private PartnerFarmLogSummaryResponse farmLogSummary;

    @JsonProperty("is_test")
    private Boolean isTest;

    @JsonProperty("isTest")
    public Boolean getIsTestCamel() {
        return isTest;
    }

    @JsonProperty("test_notice")
    private String testNotice;

    @JsonProperty("testNotice")
    public String getTestNoticeCamel() {
        return testNotice;
    }
}
