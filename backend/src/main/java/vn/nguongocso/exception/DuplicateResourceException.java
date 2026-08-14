package vn.nguongocso.exception;

/** Lớp ngoại lệ dùng để biểu thị lỗi khi có tài nguyên trùng lặp trong ứng dụng. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
