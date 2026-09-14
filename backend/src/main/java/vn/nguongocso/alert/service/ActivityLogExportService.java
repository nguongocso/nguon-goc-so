package vn.nguongocso.alert.service;

import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.auth.service.CustomUserDetails;

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
     * Tạo tệp CSV trực tiếp từ snapshot bản ghi khớp bộ lọc.
     */
    byte[] exportCsv(
            ActivityLogExportFilterRequest request,
            CustomUserDetails currentUser);
}
