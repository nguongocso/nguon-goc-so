package vn.nguongocso.trace.service;

import java.util.UUID;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.LockTraceCodeRequest;
import vn.nguongocso.trace.dto.response.LockTraceCodeResponse;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeDetailResponse;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeResponse;

/**
 * Service phát hiện và quản lý mã tem nghi vấn.
 */
public interface SuspectDetectionService {

    /**
     * Đánh giá mức nghi vấn cho một mã tem sau khi có lượt quét mới.
     */
    void evaluateSuspicion(UUID traceCodeId);

    /**
     * Lấy danh sách mã tem nghi vấn (phân trang).
     */
    PageResponse<SuspectTraceCodeResponse> getSuspectTraceCodes(Integer minScore, String status, int page, int size);

    /**
     * Lấy chi tiết mã tem nghi vấn.
     */
    SuspectTraceCodeDetailResponse getSuspectDetail(UUID traceCodeId);

    /**
     * Khóa mã tem nghi vấn.
     */
    LockTraceCodeResponse lockTraceCode(UUID traceCodeId, LockTraceCodeRequest request, UUID userId, String userName);

    /**
     * Mở khóa mã tem.
     */
    LockTraceCodeResponse unlockTraceCode(UUID traceCodeId, String reason, UUID userId, String userName);
}