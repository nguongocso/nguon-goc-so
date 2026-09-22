package vn.nguongocso.integration.partner.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
import vn.nguongocso.integration.partner.service.PartnerLotAccessService;
import vn.nguongocso.integration.partner.service.PartnerLotService;
import vn.nguongocso.integration.partner.util.PartnerSampleDataProvider;

/**
 * Controller cổng dữ liệu truy xuất lô sản xuất dành cho bên thứ ba.
*/
@RestController
@RequestMapping("/api/v1/partner/production-lots")
@RequiredArgsConstructor
public class PartnerLotController {
    private static final Logger log = LoggerFactory.getLogger(PartnerLotController.class);

    private final PartnerLotService partnerLotService;

    private final PartnerLotAccessService partnerLotAccessService;

    /**
     * Lấy hồ sơ truy xuất đầy đủ của lô sản xuất.
     */
    @GetMapping("/{lotId}/dossier")
    public ResponseEntity<ApiResult<PartnerLotDossierResponse>> getLotDossier(
            @PathVariable String lotId,
            HttpServletRequest request) {

        PartnerApiKey partnerApiKey = (PartnerApiKey) request.getAttribute("partnerApiKey");
        if (partnerApiKey == null) {
            throw new BusinessException("Thiếu hoặc không xác thực được khóa truy cập Header X-API-KEY");
        }

        log.info("Bên thứ ba '{}' (keyId={}) yêu cầu lấy hồ sơ lô {}",
                partnerApiKey.getPartnerName(), partnerApiKey.getId(), lotId);

        boolean isTestKey = Boolean.TRUE.equals(partnerApiKey.getIsTest())
                || (partnerApiKey.getKeyPrefix() != null && partnerApiKey.getKeyPrefix().startsWith("nks_test_"));

        if (isTestKey) {
            if ("sample-lot-001".equalsIgnoreCase(lotId.trim())) {
                return ResponseEntity.ok(ApiResult.success(PartnerSampleDataProvider.getSampleLotDossier()));
            }

            log.warn("Đối tác '{}' dùng khóa thử nghiệm cố truy cập mã lô '{}' -> từ chối",
                    partnerApiKey.getPartnerName(), lotId);
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Khóa thử nghiệm chỉ được phép truy cập mã lô \"sample-lot-001\". Vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa API thật.");
        }

        UUID parsedLotId;
        try {
            parsedLotId = UUID.fromString(lotId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tham số 'lotId' có giá trị không hợp lệ (yêu cầu kiểu UUID)");
        }

        PartnerLotDossierResponse response = partnerLotService.getLotDossierForPartner(parsedLotId, partnerApiKey);

        partnerLotAccessService.recordLotAccess(partnerApiKey, null, parsedLotId);

        return ResponseEntity.ok(ApiResult.success(response));
    }
}
