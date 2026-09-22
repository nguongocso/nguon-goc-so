package vn.nguongocso.certification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum trạng thái của yêu cầu kiểm nghiệm
 */
@Getter
@RequiredArgsConstructor
public enum InspectionRequestStatus {
    PENDING_RESULT("Chờ kết quả"), // Yêu cầu kiểm nghiệm chưa có kết quả.

    PASSED("Đạt"), // Yêu cầu kiểm nghiệm đã có kết quả đạt.

    FAILED("Không đạt"), // Yêu cầu kiểm nghiệm đã có kết quả không đạt.

    CANCELLED("Đã hủy"); // Yêu cầu kiểm nghiệm đã bị hủy.

    private final String label;
}