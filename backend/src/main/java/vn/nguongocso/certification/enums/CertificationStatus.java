package vn.nguongocso.certification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái hiệu lực của chứng nhận khi hiển thị công khai.
 */
@Getter
@RequiredArgsConstructor
public enum CertificationStatus {
    VALID("Còn hiệu lực"), // Chứng nhận còn hiệu lực
    
    EXPIRED("Đã hết hạn"); // Chứng nhận đã hết hạn

    private final String label; // Nhãn hiển thị cho người dùng
}