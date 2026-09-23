package vn.nguongocso.exception;

import org.springframework.http.HttpStatus;

/** Ngoại lệ dùng khi không tìm thấy tài nguyên yêu cầu trong hệ thống. */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
