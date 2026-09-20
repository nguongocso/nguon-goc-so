package vn.nguongocso.publicapi.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.dto.response.PartnerLotDossierResponse;
import vn.nguongocso.integration.partner.service.PartnerLotService;
import vn.nguongocso.integration.partner.util.PartnerSampleDataProvider;

/**
 * REST Controller cổng dữ liệu công khai dành cho đối tác tích hợp
 * (NCL-12-CN-004).
 * <p>
 * Phục vụ endpoint {@code /api/publicapi/v1/lots/{lotId}} hỗ trợ cả môi trường
 * thử nghiệm (Sandbox) với khóa thử nghiệm và môi trường thực tế.
 */
@RestController
@RequestMapping("/api/publicapi/v1/lots")
@RequiredArgsConstructor
public class PublicLotApiController {

    private static final Logger log = LoggerFactory.getLogger(PublicLotApiController.class);

    private final PartnerLotService partnerLotService;

    /**
     * Lấy dữ liệu hồ sơ lô hàng qua cổng publicapi.
     *
     * @param lotId   Mã định danh lô (UUID hoặc mã lô mẫu)
     * @param request HttpServletRequest chứa thông tin partnerApiKey đã xác thực
     * @return Dữ liệu hồ sơ lô (mẫu nếu là khóa thử nghiệm, thực tế nếu là khóa
     *         thật)
     */
    @GetMapping({ "/{lotId}", "/{lotId}/dossier" })
    public ResponseEntity<ApiResult<PartnerLotDossierResponse>> getLotDossier(
            @PathVariable String lotId,
            HttpServletRequest request) {

        PartnerApiKey partnerApiKey = (PartnerApiKey) request.getAttribute("partnerApiKey");
        if (partnerApiKey == null) {
            throw new BusinessException("Thiếu hoặc không xác thực được khóa truy cập Header X-API-KEY");
        }

        // Khóa thử nghiệm: chỉ cho phép truy cập với Lot ID là 'sample-lot-001' (TC-01, TC-02)
        boolean isTestKey = Boolean.TRUE.equals(partnerApiKey.getIsTest())
                || (partnerApiKey.getKeyPrefix() != null && partnerApiKey.getKeyPrefix().startsWith("nks_test_"));

        if (isTestKey) {
            if ("sample-lot-001".equalsIgnoreCase(lotId.trim())) {
                log.info("Đối tác '{}' sử dụng khóa thử nghiệm truy vấn lô 'sample-lot-001' -> trả dữ liệu mẫu Sandbox",
                        partnerApiKey.getPartnerName());
                return ResponseEntity.ok(ApiResult.success(PartnerSampleDataProvider.getSampleLotDossier()));
            }

            log.warn("Đối tác '{}' dùng khóa thử nghiệm cố truy cập mã lô '{}' -> từ chối",
                    partnerApiKey.getPartnerName(), lotId);
            throw new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Khóa thử nghiệm chỉ được phép truy cập mã lô \"sample-lot-001\". Vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa API thật.");
        }

        // Khóa thực tế: yêu cầu lotId là UUID hợp lệ của lô thuộc tổ chức
        UUID parsedLotId;
        try {
            parsedLotId = UUID.fromString(lotId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Mã lô không hợp lệ: " + lotId);
        }

        PartnerLotDossierResponse response = partnerLotService.getLotDossierForPartner(parsedLotId, partnerApiKey);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
