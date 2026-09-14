package vn.nguongocso.alert.service;

import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.alert.dto.response.ActivityLogExportDownload;
import vn.nguongocso.alert.dto.response.ActivityLogExportJobResponse;
import vn.nguongocso.alert.dto.response.ActivityLogExportResult;
import vn.nguongocso.auth.service.CustomUserDetails;
import java.util.UUID;

/**
 * Xem trước và xuất nhật ký hoạt động của tổ chức hiện tại.
 */
public interface ActivityLogExportService {
    /**
     * Đếm số bản ghi khớp bộ lọc xuất.
     */
    ActivityLogExportPreviewResponse preview(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser);

    /**
     * Tạo tệp CSV trực tiếp hoặc job nền từ snapshot bản ghi khớp bộ lọc.
     */
    ActivityLogExportResult requestExport(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser);

    /** Lấy trạng thái job thuộc chính tổ chức hiện tại. */
    ActivityLogExportJobResponse getJob(UUID exportId, CustomUserDetails currentUser);

    /** Lấy tệp job thành công sau khi kiểm tra tenant và trạng thái. */
    ActivityLogExportDownload getDownload(UUID exportId, CustomUserDetails currentUser);
}
