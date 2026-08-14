package vn.nguongocso.exception;

import org.springframework.http.HttpStatus;

/** Lớp ngoại lệ dùng để biểu thị các lỗi nghiệp vụ trong ứng dụng. */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final Object details;

    /**
     * Khởi tạo ngoại lệ với thông điệp lỗi và trạng thái HTTP mặc định là
     * BAD_REQUEST.
     */
    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message, null);
    }

    /** Khởi tạo ngoại lệ với thông điệp lỗi và dữ liệu chi tiết lỗi. */
    public BusinessException(String message, Object details) {
        this(HttpStatus.BAD_REQUEST, message, details);
    }

    /** Khởi tạo ngoại lệ với thông điệp lỗi và trạng thái HTTP cụ thể. */
    public BusinessException(HttpStatus status, String message) {
        this(status, message, null);
    }

    /** Khởi tạo ngoại lệ đầy đủ tham số. */
    public BusinessException(HttpStatus status, String message, Object details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    /** Lấy trạng thái HTTP liên quan đến ngoại lệ. */
    public HttpStatus getStatus() {
        return status;
    }

    /** Lấy thông tin chi tiết đính kèm. */
    public Object getDetails() {
        return details;
    }
}