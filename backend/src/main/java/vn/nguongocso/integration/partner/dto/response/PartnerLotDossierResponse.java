package vn.nguongocso.integration.partner.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hồ sơ truy xuất lô sản xuất dành cho bên thứ ba (NCL-12-CN-002, NCL-12-CN-004).
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

    /** Đánh dấu dữ liệu thử nghiệm (Sandbox). */
    @com.fasterxml.jackson.annotation.JsonProperty("is_test")
    private Boolean isTest;

    @com.fasterxml.jackson.annotation.JsonProperty("isTest")
    public Boolean getIsTestCamel() {
        return isTest;
    }

    /** Thông điệp thông báo dữ liệu thử nghiệm. */
    @com.fasterxml.jackson.annotation.JsonProperty("test_notice")
    private String testNotice;

    @com.fasterxml.jackson.annotation.JsonProperty("testNotice")
    public String getTestNoticeCamel() {
        return testNotice;
    }
}
