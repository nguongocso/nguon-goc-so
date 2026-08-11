package vn.nguongocso.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
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
}