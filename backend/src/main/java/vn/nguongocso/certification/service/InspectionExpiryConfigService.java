package vn.nguongocso.certification.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionExpiryThresholdRequest;
import vn.nguongocso.certification.dto.response.InspectionExpiryThresholdResponse;

/**
 * Dịch vụ quản lý cấu hình ngưỡng cảnh báo hết hiệu lực kiểm nghiệm (NCL-11-CN-004).
 */
public interface InspectionExpiryConfigService {

    /**
     * Lấy số ngày ngưỡng cảnh báo hết hiệu lực (ưu tiên từ DB, fallback về cấu hình mặc định).
     *
     * @return số ngày cảnh báo (>= 1)
     */
    int getWarningThresholdDays();

    /**
     * Lấy thông tin cấu hình ngưỡng cảnh báo hiện tại.
     *
     * @return DTO chứa thông tin ngưỡng và thời gian cập nhật
     */
    InspectionExpiryThresholdResponse getThresholdConfig();

    /**
     * Cập nhật ngưỡng cảnh báo hết hiệu lực kiểm nghiệm (chỉ dành cho Quản trị viên).
     *
     * @param request     yêu cầu cập nhật ngưỡng
     * @param currentUser thông tin người dùng thực hiện
     * @return DTO thông tin cấu hình sau khi cập nhật
     */
    InspectionExpiryThresholdResponse updateThresholdConfig(
            InspectionExpiryThresholdRequest request,
            CustomUserDetails currentUser);
}
