package vn.nguongocso.exception;

/**
 * Lớp ngoại lệ dùng để biểu thị lỗi khi không tìm thấy tài nguyên trong ứng
 * dụng.
 */
public class ResourceNotFoundException extends RuntimeException {
    /** Tạo một ngoại lệ mới với thông báo lỗi. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
