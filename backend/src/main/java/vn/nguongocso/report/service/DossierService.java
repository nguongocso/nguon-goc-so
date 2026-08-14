package vn.nguongocso.report.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.DossierCheckResponse;

import java.util.UUID;

/**
 * Service xử lý nghiệp vụ hồ sơ.
 *
 * @author Triệu Văn Đại
 */
public interface DossierService {
    // Kiểm tra điều kiện xuất hồ sơ.
    DossierCheckResponse checkEligibility(UUID shipmentId, CustomUserDetails currentUser);

    // Xuất hồ sơ dạng PDF.
    byte[] exportDossierPdf(UUID shipmentId, CustomUserDetails currentUser, String ipAddress);
}