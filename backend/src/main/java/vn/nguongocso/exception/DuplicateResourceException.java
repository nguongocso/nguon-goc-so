package vn.nguongocso.exception;

import org.springframework.http.HttpStatus;

/** Ngoại lệ dùng khi phát hiện tài nguyên bị trùng lặp trong hệ thống. */
public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
