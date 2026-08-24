package vn.nguongocso.event.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.dto.request.WarehouseReceiptRequest;
import vn.nguongocso.event.dto.response.WarehouseReceiptResponse;
import vn.nguongocso.event.service.WarehouseReceiptService;

/**
 * Controller quản lý nhập kho và đối chiếu số lượng.
 * Chỉ dành cho Doanh nghiệp thu mua (VT-04).
 *
 * @author Team
 */
@RestController
@RequestMapping("/api/v1/chain-events")
@RequiredArgsConstructor
public class WarehouseReceiptController {

    private final WarehouseReceiptService warehouseReceiptService;

    /**
     * API ghi nhận nhập kho và đối chiếu số lượng.
     * Chỉ dành cho Doanh nghiệp thu mua (VT-04).
     */
    @PostMapping("/warehouse-receipt")
    @PreAuthorize("hasRole('VT-04')")
    public ResponseEntity<ApiResult<WarehouseReceiptResponse>> recordWarehouseReceipt(
            @Valid @RequestBody WarehouseReceiptRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        WarehouseReceiptResponse response = warehouseReceiptService.recordWarehouseReceipt(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * API lấy danh sách sự kiện nhập kho của doanh nghiệp thu mua hiện tại.
     * Chỉ dành cho Doanh nghiệp thu mua (VT-04).
     */
    @GetMapping("/warehouse-receipts")
    @PreAuthorize("hasRole('VT-04')")
    public ResponseEntity<ApiResult<PageResponse<WarehouseReceiptResponse>>> getWarehouseReceipts(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 10, sort = "recordedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<WarehouseReceiptResponse> response = warehouseReceiptService.getWarehouseReceipts(currentUser, pageable);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * API lấy chi tiết một sự kiện nhập kho.
     * Chỉ dành cho Doanh nghiệp thu mua (VT-04).
     */
    @GetMapping("/warehouse-receipts/{eventId}")
    @PreAuthorize("hasRole('VT-04')")
    public ResponseEntity<ApiResult<WarehouseReceiptResponse>> getWarehouseReceiptDetail(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        WarehouseReceiptResponse response = warehouseReceiptService.getWarehouseReceiptDetail(eventId, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
