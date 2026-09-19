package vn.nguongocso.integration.partner.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.util.PartnerSampleDataProvider;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;

/**
 * Controller xuất hồ sơ theo lược đồ GS1 mô phỏng dành cho Bên thứ ba (NCL-12-CN-004, NCL-12-CN-003).
 * <p>
 * Yêu cầu đối tác gửi Header {@code X-API-KEY}. Đã qua xác thực từ {@code ApiKeyAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/partner/shipments")
@RequiredArgsConstructor
public class PartnerShipmentController {

    private static final Logger log = LoggerFactory.getLogger(PartnerShipmentController.class);

    private final vn.nguongocso.integration.partner.service.PartnerLotAccessService partnerLotAccessService;

    /**
     * Xuất hồ sơ GS1 mô phỏng cho bên thứ ba (hỗ trợ định dạng JSON và XML).
     */
    @GetMapping("/{shipmentId}/dossier/gs1")
    public ResponseEntity<?> getGs1DossierForPartner(
            @PathVariable String shipmentId,
            @RequestParam(name = "format", defaultValue = "json") String format,
            @RequestParam(name = "includeMapping", defaultValue = "true") boolean includeMapping,
            HttpServletRequest request) {

        PartnerApiKey partnerApiKey = (PartnerApiKey) request.getAttribute("partnerApiKey");
        if (partnerApiKey == null) {
            throw new BusinessException("Thiếu hoặc không xác thực được khóa truy cập Header X-API-KEY");
        }

        boolean isTestKey = Boolean.TRUE.equals(partnerApiKey.getIsTest())
                || (partnerApiKey.getKeyPrefix() != null && partnerApiKey.getKeyPrefix().startsWith("nks_test_"));

        if (isTestKey) {
            String trimmedId = shipmentId.trim();
            // Cho phép cả sample-lot-001 và sample-shipment-001
            if ("sample-lot-001".equalsIgnoreCase(trimmedId) || "sample-shipment-001".equalsIgnoreCase(trimmedId)) {
                log.info("Bên thứ ba '{}' gọi xuất hồ sơ GS1 bằng khóa thử nghiệm (shipmentId={}) -> Trả dữ liệu mẫu Sandbox (NCL-12-CN-004)",
                        partnerApiKey.getPartnerName(), shipmentId);

                Gs1DossierExportResponse sampleResponse = PartnerSampleDataProvider.getSampleGs1DossierResponse();
                String normalizedFormat = format == null ? "json" : format.toLowerCase();

                if ("xml".equals(normalizedFormat)) {
                    try {
                        XmlMapper xmlMapper = new XmlMapper();
                        xmlMapper.registerModule(new JavaTimeModule());
                        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                        String xml = xmlMapper.writeValueAsString(sampleResponse);
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_XML)
                                .body(xml);
                    } catch (JsonProcessingException ex) {
                        throw new RuntimeException("Lỗi khi sinh XML hồ sơ GS1 mẫu.", ex);
                    }
                }

                return ResponseEntity.ok(ApiResult.success(sampleResponse));
            }

            log.warn("Đối tác '{}' dùng khóa thử nghiệm cố truy cập lô hàng '{}' -> từ chối",
                    partnerApiKey.getPartnerName(), shipmentId);
            throw new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Khóa thử nghiệm chỉ được phép truy cập mã lô \"sample-lot-001\". Vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa API thật.");
        }

        UUID parsedShipmentId;
        try {
            parsedShipmentId = UUID.fromString(shipmentId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tham số 'shipmentId' có giá trị không hợp lệ (yêu cầu kiểu UUID)");
        }

        // Ghi nhận nhật ký truy xuất lô hàng của đối tác (NCL-12-CN-006 / TC-03)
        partnerLotAccessService.recordLotAccess(partnerApiKey, parsedShipmentId, null);

        // Trường hợp khóa thật: trả hồ sơ GS1 mẫu cho lô hàng hoặc thông báo
        Gs1DossierExportResponse response = PartnerSampleDataProvider.getSampleGs1DossierResponse();
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
