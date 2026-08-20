package vn.nguongocso.event.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.dto.response.FailedEventLogResponse;
import vn.nguongocso.event.dto.response.LotValidationResponse;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.UUID;
/**
 * Controller xác thực sự kiện chuỗi cung ứng.
 *
 * @author Triệu Văn Đại
 */
@RestController
@RequestMapping("/api/v1/chain-events")
@RequiredArgsConstructor
public class EventValidationController {
    private final EventValidationService eventValidationService;
    private final PermissionChecker permissionChecker;

    /**
     * API 1: Kiểm tra tính hợp lệ của Lô/Lô hàng trước khi tạo sự kiện.
     */
    @GetMapping("/validate-lot")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03', 'VT-04')")
    public ResponseEntity<ApiResult<LotValidationResponse>> validateLot(
            @RequestParam @NotNull UUID lotId,
            @RequestParam @NotNull ChainEventType eventType,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        LotValidationResponse response = eventValidationService.validateLot(lotId, eventType, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * API 2: Hủy bỏ bản nháp sự kiện / lô hàng sai lô.
     */
    @DeleteMapping("/drafts/{id}")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<Void>> deleteDraft(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        eventValidationService.deleteDraft(id, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), null));
    }

    /**
     * API 3: Truy vấn nhật ký các lần ghi sự kiện bị chặn (Sai lô).
     */
    @GetMapping("/failed-logs")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<PageResponse<FailedEventLogResponse>>> getFailedLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<FailedEventLogResponse> response = eventValidationService.getFailedLogs(pageable);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
