package vn.nguongocso.certification.service.impl;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import vn.nguongocso.exception.BusinessException;

public class DuplicateInspectionRequestException extends BusinessException {

    public DuplicateInspectionRequestException(UUID duplicateRequestId) {
        super(
                HttpStatus.CONFLICT,
                "Yêu cầu kiểm nghiệm trùng lặp với yêu cầu đang chờ kết quả cho cùng bộ chỉ tiêu. " +
                        "Vui lòng xác nhận để tạo thêm yêu cầu.");
    }
}
