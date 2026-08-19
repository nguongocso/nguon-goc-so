package vn.nguongocso.report.exception;

import lombok.Getter;
import java.util.List;

/**
 * Ngoại lệ kiểm tra hồ sơ không hợp lệ.
 *
 * @author Triệu Văn Đại
 */
@Getter
public class DossierValidationException extends RuntimeException {

    // Danh sách lỗi chi tiết
    private final List<String> errors;

    /**
     * Tạo ngoại lệ kiểm tra hồ sơ không hợp lệ với thông điệp và danh sách lỗi chi
     * tiết.
     *
     * @param message Thông điệp ngoại lệ
     * @param errors  Danh sách lỗi chi tiết
     */
    public DossierValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}