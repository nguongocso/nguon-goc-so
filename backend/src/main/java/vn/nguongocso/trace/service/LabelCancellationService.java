package vn.nguongocso.trace.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.trace.dto.request.CancelTraceCodesRequest;
import vn.nguongocso.trace.dto.response.CancelTraceCodesResponse;
import vn.nguongocso.trace.dto.response.LabelCancellationHistoryResponse;

/** Service xử lý hủy tem và hoàn hạn mức. */
public interface LabelCancellationService {

    /**
     * Hủy danh sách tem/mã truy xuất in hỏng và hoàn hạn mức.
     */
    CancelTraceCodesResponse cancelTraceCodes(
        UUID shipmentId,
        CancelTraceCodesRequest request
    );

    /**
     * Lấy danh sách lịch sử các đợt hủy tem của lô hàng.
     */
    List<LabelCancellationHistoryResponse> getCancellationHistory(
        UUID shipmentId
    );
}
