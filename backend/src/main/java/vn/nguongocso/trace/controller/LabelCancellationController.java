package vn.nguongocso.trace.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.trace.dto.request.CancelTraceCodesRequest;
import vn.nguongocso.trace.dto.response.CancelTraceCodesResponse;
import vn.nguongocso.trace.dto.response.LabelCancellationHistoryResponse;
import vn.nguongocso.trace.service.LabelCancellationService;

/** Controller xử lý hủy tem và xem lịch sử hủy tem. */
@RestController
@RequestMapping("/api/v1/trace/shipments")
@RequiredArgsConstructor
public class LabelCancellationController {
    private final LabelCancellationService labelCancellationService;

    /** Hủy tem in hỏng và hoàn lại hạn mức dải mã. */
    @PostMapping("/{shipmentId}/cancel-labels")
    public ApiResult<CancelTraceCodesResponse> cancelLabels(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody CancelTraceCodesRequest request) {
        CancelTraceCodesResponse response = labelCancellationService.cancelTraceCodes(shipmentId, request);
        return ApiResult.success(response);
    }

    /** Lấy danh sách lịch sử các đợt hủy tem của lô hàng. */
    @GetMapping("/{shipmentId}/cancellation-history")
    public ApiResult<List<LabelCancellationHistoryResponse>> getCancellationHistory(
            @PathVariable UUID shipmentId) {
        List<LabelCancellationHistoryResponse> history = labelCancellationService.getCancellationHistory(shipmentId);
        return ApiResult.success(history);
    }
}
