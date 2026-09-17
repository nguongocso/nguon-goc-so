package vn.nguongocso.certification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Nguồn ghi nhận kết quả kiểm nghiệm (QTN-14 / NCL-11-CN-007).
 */
@Getter
@RequiredArgsConstructor
public enum InspectionResultEntrySource {

    /**
     * Quản lý Hợp tác xã (VT-02) nhập kết quả thủ công trên hệ thống.
     */
    COOPERATIVE_MANUAL("Hợp tác xã nhập thủ công"),

    /**
     * Đơn vị kiểm nghiệm trực tiếp khai báo kết quả qua liên kết cổng bảo mật dùng một lần.
     */
    TESTING_UNIT_PORTAL("Đơn vị kiểm nghiệm nhập qua cổng");

    private final String description;
}
