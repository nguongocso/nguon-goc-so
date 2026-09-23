package vn.nguongocso.export.exception;

import org.springframework.http.HttpStatus;
import vn.nguongocso.exception.BusinessException;

import java.util.List;

/**
 * Ngoại lệ khi mẫu hồ sơ truy xuất vi phạm quy tắc QTN-11 do thiếu các trường
 * bắt buộc.
 * HTTP Status: 422 Unprocessable Entity.
 */
public class MandatoryFieldsViolationException extends BusinessException {
    public MandatoryFieldsViolationException(List<String> missingFields) {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "Không thể lưu mẫu hồ sơ: Thiếu các trường bắt buộc theo quy định.",
                missingFields);
    }

    /** Lấy danh sách các trường bắt buộc bị thiếu. */
    @SuppressWarnings("unchecked")
    public List<String> getMissingFields() {
        return (List<String>) getDetails();
    }
}
