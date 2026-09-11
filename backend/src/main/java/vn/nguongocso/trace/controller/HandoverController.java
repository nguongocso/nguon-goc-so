package vn.nguongocso.trace.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.dto.response.HandoverSummaryResponse;
import vn.nguongocso.trace.service.ShipmentHandoverService;

/**
 * Controller xử lý API danh sách và thao tác phiếu bàn giao cho tổ chức bàn giao (VT-02) và thu mua (VT-04).
 */
@RestController
@RequestMapping("/api/v1/handovers")
@RequiredArgsConstructor
public class HandoverController {

    private final ShipmentHandoverService handoverService;

    /**
     * Lấy danh sách phiếu bàn giao của tổ chức hiện tại.
     * Hỗ trợ tìm kiếm, lọc trạng thái và phân trang.
     * - VT-04: Phiếu nhận (toOrganization = currentUser.organization)
     * - VT-02: Phiếu đã gửi (fromOrganization = currentUser.organization)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<PageResponse<HandoverSummaryResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        PageResponse<HandoverSummaryResponse> response = handoverService.listForCurrentOrganization(status, search, page, size, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy chi tiết phiếu bàn giao theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03', 'VT-04')")
    public ResponseEntity<ApiResult<HandoverResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.success(handoverService.getById(id)));
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
}
