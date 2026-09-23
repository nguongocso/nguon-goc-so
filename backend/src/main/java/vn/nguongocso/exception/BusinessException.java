package vn.nguongocso.exception;

import org.springframework.http.HttpStatus;

/** Ngoại lệ dùng cho các vi phạm quy tắc nghiệp vụ trong hệ thống. */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final Object details;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message, null);
    }

    public BusinessException(String message, Object details) {
        this(HttpStatus.BAD_REQUEST, message, details);
    }

    public BusinessException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public BusinessException(HttpStatus status, String message, Object details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getDetails() {
        return details;
    }
}
