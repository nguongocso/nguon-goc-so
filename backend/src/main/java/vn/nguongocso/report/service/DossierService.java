package vn.nguongocso.report.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;

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
     * Xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng.
     *
     * <p>
     * Chỉ dành cho VT-02 (Quản lý HTX) và VT-04 (Doanh nghiệp thu mua). Hồ sơ
     * được ánh xạ theo bốn chiều {@code who / when / where / why} kèm lịch sử
     * kiểm nghiệm của lô sản xuất tương ứng và không làm thay đổi bất kỳ dữ
     * liệu nghiệp vụ nào.
     * </p>
     *
     * @param shipmentId       ID lô hàng cần xuất hồ sơ
     * @param format           định dạng xuất: {@code json} hoặc {@code xml}
     * @param includeMapping   có kèm bảng ánh xạ schema trong hồ sơ hay không
     * @param currentUser      thông tin người dùng hiện tại
     * @param ipAddress        địa chỉ IP của client
     * @return hồ sơ GS1 mô phỏng
     */
    Gs1DossierExportResponse exportGs1Dossier(UUID shipmentId,
                                              String format,
                                              boolean includeMapping,
                                              CustomUserDetails currentUser,
                                              String ipAddress);

    // NCL-07-CN-005: Kiểm tra điều kiện xuất hồ sơ cho nhiều lô
    vn.nguongocso.report.dto.response.BatchDossierCheckResponse checkBatchEligibility(
            vn.nguongocso.report.dto.request.BatchDossierCheckRequest request,
            CustomUserDetails currentUser);

    // NCL-07-CN-005: Xuất bộ hồ sơ truy xuất nhiều lô (PDF)
    byte[] exportBatchDossierPdf(
            vn.nguongocso.report.dto.request.BatchDossierExportRequest request,
            CustomUserDetails currentUser,
            String ipAddress);

    // NCL-07-CN-005: Lịch sử xuất bộ hồ sơ hàng loạt
    java.util.List<vn.nguongocso.report.dto.response.BatchDossierHistoryDto> getBatchExportHistory(
            CustomUserDetails currentUser);
}
