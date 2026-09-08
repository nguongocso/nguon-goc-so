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
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.request.CreateHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.service.ShipmentHandoverService;

/**
 * Controller xử lý API cho phiếu bàn giao lô hàng.
 */
@RestController
@RequestMapping("/api/v1/shipment-handovers")
@RequiredArgsConstructor
public class ShipmentHandoverController {

    private final ShipmentHandoverService handoverService;

    @PostMapping
    public ResponseEntity<ApiResult<HandoverResponse>> create(
            @RequestBody @Valid CreateHandoverRequest request) {
        return ResponseEntity.ok(ApiResult.success(handoverService.create(request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResult<HandoverResponse>> cancel(
            @PathVariable UUID id,
            @RequestBody @Valid CancelHandoverRequest request) {
        return ResponseEntity.ok(ApiResult.success(handoverService.cancel(id, request)));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResult<HandoverResponse>> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.success(handoverService.accept(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResult<HandoverResponse>> reject(
            @PathVariable UUID id,
            @RequestBody @Valid CancelHandoverRequest request) {
        return ResponseEntity.ok(ApiResult.success(handoverService.reject(id, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<HandoverResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.success(handoverService.getById(id)));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResult<List<HandoverResponse>>> getSent() {
        return ResponseEntity.ok(ApiResult.success(handoverService.getSentHandovers()));
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResult<List<HandoverResponse>>> getReceived() {
        return ResponseEntity.ok(ApiResult.success(handoverService.getReceivedHandovers()));
    }
}
