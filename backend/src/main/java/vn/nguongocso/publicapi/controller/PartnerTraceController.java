package vn.nguongocso.publicapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.service.PartnerLotAccessService;
import vn.nguongocso.integration.partner.util.PartnerSampleDataProvider;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.PublicTraceService;

/**
 * Controller truy xuất nguồn gốc dành cho Đối tác bên thứ ba (NCL-12-CN-001 / QTN-20).
 *
 * <p>
 * Yêu cầu đối tác gửi Header {@code X-API-KEY}. Đã qua xác thực và đếm hạn mức từ {@code ApiKeyAuthenticationFilter}.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/partner/trace")
@RequiredArgsConstructor
public class PartnerTraceController {

    private final PublicTraceService publicTraceService;
    private final PartnerLotAccessService partnerLotAccessService;

    /**
     * Lấy dữ liệu truy xuất công khai cho bên thứ ba.
     *
     * @param codeValue mã tem truy xuất
     * @param latitude  vĩ độ
     * @param longitude kinh độ
     * @param request   HTTP request
     * @return thông tin truy xuất công khai
     */
    @GetMapping("/{codeValue}")
    public ResponseEntity<ApiResult<PublicTraceResponse>> getPartnerTrace(
            @PathVariable String codeValue,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            HttpServletRequest request) {

        PartnerApiKey partnerApiKey = (PartnerApiKey) request.getAttribute("partnerApiKey");
        if (partnerApiKey != null) {
            log.info("Đối tác '{}' (orgId={}) gọi API truy xuất mã={}",
                    partnerApiKey.getPartnerName(),
                    partnerApiKey.getOrganization() != null
                            ? partnerApiKey.getOrganization().getOrganizationId()
                            : "N/A",
                    codeValue);

            // TC-01, TC-02: Nếu là khóa thử nghiệm -> Trả dữ liệu mẫu Sandbox chuẩn
            if (Boolean.TRUE.equals(partnerApiKey.getIsTest())) {
                log.info("Đối tác '{}' gọi tra cứu bằng khóa thử nghiệm mã={} -> Trả dữ liệu mẫu Sandbox (NCL-12-CN-004)",
                        partnerApiKey.getPartnerName(), codeValue);
                return ResponseEntity.ok(ApiResult.success(PartnerSampleDataProvider.getSampleTraceResponse()));
            }
        }

        PublicTraceResponse response = publicTraceService.getPublicTrace(
                codeValue,
                latitude,
                longitude,
                getClientIp(request),
                request.getHeader("User-Agent"));
        response.setIsTest(false);

        // Ghi nhận nhật ký truy xuất lô của đối tác (NCL-12-CN-006 / TC-03)
        if (partnerApiKey != null) {
            partnerLotAccessService.recordLotAccess(
                    partnerApiKey,
                    response.getShipmentId(),
                    response.getProductionLotId());
        }

        return ResponseEntity.ok(ApiResult.success(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
