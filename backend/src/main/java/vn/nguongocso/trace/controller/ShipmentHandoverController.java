package vn.nguongocso.trace.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.request.CreateHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverAttachmentUploadResponse;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.service.ShipmentHandoverService;

/**
 * Controller xử lý phiếu bàn giao lô hàng.
 */
@RestController
@RequestMapping("/api/v1/shipment-handovers")
@RequiredArgsConstructor
public class ShipmentHandoverController {

    private final ShipmentHandoverService handoverService;

    /**
     * Tạo phiếu bàn giao lô hàng.
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<HandoverResponse>> create(
            @RequestBody @Valid CreateHandoverRequest request) {
        return ResponseEntity.ok(ApiResult.success(handoverService.create(request)));
    }

    /**
     * Tải lên chứng từ giao hàng trước khi tạo phiếu bàn giao.
     */
    @PostMapping(value = "/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<HandoverAttachmentUploadResponse>> uploadAttachment(
            @RequestParam("file") MultipartFile file) {

        String filePath = handoverService.uploadAttachment(file);

        return ResponseEntity.ok(
                ApiResult.success(
                        HandoverAttachmentUploadResponse.builder()
                                .filePath(filePath)
                                .build()));
    }

    /**
     * Hủy phiếu bàn giao đang chờ xác nhận.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<HandoverResponse>> cancel(
            @PathVariable UUID id,
            @RequestBody @Valid CancelHandoverRequest request) {
        return ResponseEntity.ok(ApiResult.success(handoverService.cancel(id, request)));
    }

    /**
     * Xác nhận nhận bàn giao lô hàng.
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<HandoverResponse>> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.success(handoverService.accept(id)));
    }

    /**
     * Từ chối nhận bàn giao lô hàng.
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<HandoverResponse>> reject(
            @PathVariable UUID id,
            @RequestBody @Valid CancelHandoverRequest request) {
        return ResponseEntity.ok(ApiResult.success(handoverService.reject(id, request)));
    }

    /**
     * Lấy chi tiết phiếu bàn giao.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03', 'VT-04')")
    public ResponseEntity<ApiResult<HandoverResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.success(handoverService.getById(id)));
    }

    /**
     * Lấy danh sách phiếu bàn giao đã gửi.
     */
    @GetMapping("/sent")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<List<HandoverResponse>>> getSent() {
        return ResponseEntity.ok(ApiResult.success(handoverService.getSentHandovers()));
    }

    /**
     * Lấy danh sách phiếu bàn giao đã nhận.
     */
    @GetMapping("/received")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<List<HandoverResponse>>> getReceived() {
        return ResponseEntity.ok(ApiResult.success(handoverService.getReceivedHandovers()));
    }
}
