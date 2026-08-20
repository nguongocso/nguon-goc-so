package vn.nguongocso.trace.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.RecallRequest;
import vn.nguongocso.trace.dto.response.RecallInfoResponse;
import vn.nguongocso.trace.dto.response.RecallResponse;
import vn.nguongocso.trace.service.ShipmentRecallService;

/**
 * Controller quản lý thu hồi lô hàng.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Validated
public class ShipmentRecallController {

    private final ShipmentRecallService shipmentRecallService;
    private final PermissionChecker permissionChecker;

    /**
     * Thu hồi một lô hàng.
     *
     * POST /api/v1/shipments/{shipmentId}/recall
     */
    @PostMapping("/{shipmentId}/recall")
    public ResponseEntity<ApiResult<RecallResponse>> recallShipment(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody RecallRequest request,
            HttpServletRequest httpRequest) {

        // Kiểm tra quyền thực hiện thao tác
        permissionChecker.check("SHIPMENT", "CREATE");

        // Lấy IP của client
        String ipAddress = getClientIpAddress(httpRequest);

        // Truyền IP xuống service để ghi vào lịch sử hoạt động
        RecallResponse response =
                shipmentRecallService.recallShipment(
                        shipmentId,
                        request,
                        ipAddress);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(
                        HttpStatus.CREATED.value(),
                        response));
    }

    /**
     * Lấy thông tin thu hồi của lô hàng.
     *
     * GET /api/v1/shipments/{shipmentId}/recall
     */
    @GetMapping("/{shipmentId}/recall")
    public ResponseEntity<ApiResult<RecallInfoResponse>> getRecallInfo(
            @PathVariable UUID shipmentId) {

        permissionChecker.check("SHIPMENT", "READ");

        RecallInfoResponse response =
                shipmentRecallService.getRecallInfo(shipmentId);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        response));
    }

    /**
     * Lấy địa chỉ IP thực tế của client.
     *
     * Ưu tiên:
     * 1. X-Forwarded-For
     * 2. X-Real-IP
     * 3. request.getRemoteAddr()
     */
    private String getClientIpAddress(HttpServletRequest request) {

        String xForwardedFor =
                request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null
                && !xForwardedFor.isBlank()) {

            return xForwardedFor
                    .split(",")[0]
                    .trim();
        }

        String xRealIp =
                request.getHeader("X-Real-IP");

        if (xRealIp != null
                && !xRealIp.isBlank()) {

            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
