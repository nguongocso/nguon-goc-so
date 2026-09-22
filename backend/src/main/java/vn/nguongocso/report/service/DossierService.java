package vn.nguongocso.report.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.request.BatchDossierCheckRequest;
import vn.nguongocso.report.dto.request.BatchDossierExportRequest;
import vn.nguongocso.report.dto.response.BatchDossierCheckResponse;
import vn.nguongocso.report.dto.response.BatchDossierHistoryDto;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;

/** Service xử lý nghiệp vụ hồ sơ truy xuất. */
public interface DossierService {

    /**
     * Kiểm tra điều kiện xuất hồ sơ của lô hàng.
     */
    DossierCheckResponse checkEligibility(
        UUID shipmentId,
        CustomUserDetails currentUser
    );

    /**
     * Xuất hồ sơ truy xuất dạng PDF mặc định.
     */
    byte[] exportDossierPdf(
        UUID shipmentId,
        CustomUserDetails currentUser,
        String ipAddress
    );

    /**
     * Xuất hồ sơ truy xuất dạng PDF áp dụng mẫu cấu hình trường đối tác.
     */
    byte[] exportDossierPdf(
        UUID shipmentId,
        UUID templateId,
        CustomUserDetails currentUser,
        String ipAddress
    );

    /**
     * Xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng.
     */
    Gs1DossierExportResponse exportGs1Dossier(
        UUID shipmentId,
        String format,
        boolean includeMapping,
        CustomUserDetails currentUser,
        String ipAddress
    );

    /**
     * Kiểm tra điều kiện xuất hồ sơ cho nhiều lô hàng.
     */
    BatchDossierCheckResponse checkBatchEligibility(
        BatchDossierCheckRequest request,
        CustomUserDetails currentUser
    );

    /**
     * Xuất bộ hồ sơ truy xuất nhiều lô dạng PDF.
     */
    byte[] exportBatchDossierPdf(
        BatchDossierExportRequest request,
        CustomUserDetails currentUser,
        String ipAddress
    );

    /**
     * Lấy lịch sử xuất bộ hồ sơ hàng loạt.
     */
    List<BatchDossierHistoryDto> getBatchExportHistory(
        CustomUserDetails currentUser
    );
}
