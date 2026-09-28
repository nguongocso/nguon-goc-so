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
         */
        int getWarningThresholdDays();

        /**
         * Lấy thông tin cấu hình ngưỡng cảnh báo hiện tại.
         */
        InspectionExpiryThresholdResponse getThresholdConfig();

        /**
         * Cập nhật ngưỡng cảnh báo hết hiệu lực kiểm nghiệm (chỉ dành cho Quản trị viên).
         */
        InspectionExpiryThresholdResponse updateThresholdConfig(
                        InspectionExpiryThresholdRequest request,
                        CustomUserDetails currentUser);
}
