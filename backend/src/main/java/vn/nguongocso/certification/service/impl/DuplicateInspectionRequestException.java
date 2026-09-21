package vn.nguongocso.certification.service.impl;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import vn.nguongocso.exception.BusinessException;

/**
 * Ngoại lệ khi tạo yêu cầu kiểm nghiệm bị trùng lặp với yêu cầu đang chờ kết quả.
 */
public class DuplicateInspectionRequestException extends BusinessException {
    /**
     * Khởi tạo ngoại lệ yêu cầu kiểm nghiệm trùng lặp.
     */
    public DuplicateInspectionRequestException(UUID duplicateRequestId) {
        super(
                HttpStatus.CONFLICT,
                "Yêu cầu kiểm nghiệm trùng lặp với yêu cầu đang chờ kết quả cho cùng bộ chỉ tiêu. " +
                        "Vui lòng xác nhận để tạo thêm yêu cầu.");
    }
}
