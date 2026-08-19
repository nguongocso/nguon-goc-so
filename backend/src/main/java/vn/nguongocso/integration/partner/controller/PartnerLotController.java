package vn.nguongocso.integration.partner.controller;

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

/**
 * REST Controller cổng dữ liệu truy xuất lô sản xuất dành cho Bên thứ ba / Doanh nghiệp thu mua (NCL-12-CN-002).
 * <p>
 * Yêu cầu đối tác gửi Header {@code X-API-KEY}. Đã qua xác thực và kiểm soát hạn mức QTN-20 từ {@code ApiKeyAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/partner/production-lots")
@RequiredArgsConstructor
public class PartnerLotController {

    private static final Logger log = LoggerFactory.getLogger(PartnerLotController.class);

    private final PartnerLotService partnerLotService;

    /**
     * Lấy hồ sơ truy xuất đầy đủ của lô sản xuất (TC-01, TC-02, TC-03, TC-04).
     */
    @GetMapping("/{lotId}/dossier")
    public ResponseEntity<ApiResult<PartnerLotDossierResponse>> getLotDossier(
            @PathVariable UUID lotId,
            HttpServletRequest request) {

        PartnerApiKey partnerApiKey = (PartnerApiKey) request.getAttribute("partnerApiKey");
        if (partnerApiKey == null) {
            throw new BusinessException("Thiếu hoặc không xác thực được khóa truy cập Header X-API-KEY");
        }

        log.info("Bên thứ ba '{}' (keyId={}) yêu cầu lấy hồ sơ lô {}",
                partnerApiKey.getPartnerName(), partnerApiKey.getId(), lotId);

        PartnerLotDossierResponse response = partnerLotService.getLotDossierForPartner(lotId, partnerApiKey);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
