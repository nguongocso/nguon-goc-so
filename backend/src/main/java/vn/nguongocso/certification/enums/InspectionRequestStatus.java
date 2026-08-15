package vn.nguongocso.certification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum trạng thái của yêu cầu kiểm nghiệm
 */
@Getter
@RequiredArgsConstructor
public enum InspectionRequestStatus {

    PENDING_RESULT("Chờ kết quả"),

    PASSED("Đạt"),

    FAILED("Không đạt"),

    CANCELLED("Đã hủy");

    private final String label;
}