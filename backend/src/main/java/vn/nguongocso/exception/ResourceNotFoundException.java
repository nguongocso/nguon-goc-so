package vn.nguongocso.exception;

import org.springframework.http.HttpStatus;

/**
 * Lớp ngoại lệ dùng để biểu thị lỗi khi không tìm thấy tài nguyên trong ứng
 * dụng.
 */
public class ResourceNotFoundException extends BusinessException {

    /** Tạo một ngoại lệ mới với thông báo lỗi và trạng thái HTTP 404 NOT_FOUND. */
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
