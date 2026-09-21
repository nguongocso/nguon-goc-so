package vn.nguongocso.exception;

import org.springframework.http.HttpStatus;

/** Lớp ngoại lệ dùng để biểu thị lỗi khi có tài nguyên trùng lặp trong ứng dụng. */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

