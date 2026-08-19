package vn.nguongocso.publicapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.PublicTraceService;

/**
 * Controller truy xuất nguồn gốc dành cho Đối tác bên thứ ba (NCL-12-CN-001 / QTN-20).
 * <p>
 * Yêu cầu đối tác gửi Header {@code X-API-KEY}. Đã qua xác thực và đếm hạn mức từ {@code ApiKeyAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/partner/trace")
@RequiredArgsConstructor
public class PartnerTraceController {

    private static final Logger log = LoggerFactory.getLogger(PartnerTraceController.class);

    private final PublicTraceService publicTraceService;

    /**
     * Lấy dữ liệu truy xuất công khai cho bên thứ ba.
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
                    partnerApiKey.getOrganization().getOrganizationId(),
                    codeValue);
        }

        PublicTraceResponse response = publicTraceService.getPublicTrace(
                codeValue,
                latitude,
                longitude,
                getClientIp(request),
                request.getHeader("User-Agent"));

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
