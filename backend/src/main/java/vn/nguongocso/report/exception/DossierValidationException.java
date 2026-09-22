package vn.nguongocso.report.exception;

import java.util.List;

import lombok.Getter;

/** Ngoại lệ kiểm tra hồ sơ không hợp lệ. */
@Getter
public class DossierValidationException extends RuntimeException {
    private final List<String> errors;

    /** Tạo ngoại lệ kiểm tra hồ sơ không hợp lệ với thông điệp và danh sách lỗi chi tiết. */
    public DossierValidationException(
            String message,
            List<String> errors
    ) {
        super(message);
        this.errors = errors;
    }
}
