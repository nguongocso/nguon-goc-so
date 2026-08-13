package vn.nguongocso.report.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.dto.response.GS1DossierExportResponse;

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

    /**
     * Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1 (NCL-12-CN-003).
     *
     * @param shipmentId     ID của lô hàng
     * @param currentUser    Người dùng hiện tại (VT-02 / VT-04)
     * @param includeMapping Có bao gồm bảng ánh xạ schema hay không
     * @return DTO hồ sơ GS1 mô phỏng (JSON)
     */
    GS1DossierExportResponse exportGs1Dossier(UUID shipmentId, CustomUserDetails currentUser,
            boolean includeMapping);

    /**
     * Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1 dạng XML (NCL-12-CN-003).
     *
     * @param shipmentId     ID của lô hàng
     * @param currentUser    Người dùng hiện tại (VT-02 / VT-04)
     * @param includeMapping Có bao gồm bảng ánh xạ schema hay không
     * @return Chuỗi XML biểu diễn cùng tập dữ liệu với JSON
     */
    String exportGs1DossierXml(UUID shipmentId, CustomUserDetails currentUser, boolean includeMapping);
}