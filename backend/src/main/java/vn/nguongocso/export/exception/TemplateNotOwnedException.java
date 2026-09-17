package vn.nguongocso.export.exception;

import org.springframework.http.HttpStatus;
import vn.nguongocso.exception.BusinessException;

/**
 * Ngoại lệ khi người dùng cố tình truy cập hoặc sử dụng mẫu hồ sơ của tổ chức khác.
 * Quy tắc QTN-01: Cách ly dữ liệu tổ chức.
 * HTTP Status: 403 Forbidden.
 */
public class TemplateNotOwnedException extends BusinessException {

    public TemplateNotOwnedException() {
        super(HttpStatus.FORBIDDEN, "Từ chối thao tác: Bạn không có quyền truy cập dữ liệu của tổ chức khác.");
    }

    public TemplateNotOwnedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
