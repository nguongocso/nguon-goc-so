package vn.nguongocso.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.event.dto.request.CorrectPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;

import java.util.UUID;

/**
 * Controller REST quản lý các sự kiện trong chuỗi cung ứng.
 * <p>
 * Cung cấp các API để ghi nhận và quản lý các sự kiện như:
 * <ul>
 *   <li>Thu hoạch (HARVEST)</li>
 *   <li>Đóng gói (PACKAGING)</li>
 *   <li>Sửa lỗi đóng gói (CORRECTION)</li>
 * </ul>
 * </p>
 *
 * <p>Tất cả các API đều yêu cầu xác thực và phân quyền.
 * Chỉ người dùng có vai trò VT-02 (Quản lý HTX) hoặc VT-03 (Người ghi sự kiện)
 * mới được phép thực hiện các thao tác này.</p>
 *
 * @author Team WEB !
 */

@RestController
@RequestMapping("/api/v1/chain-events")
@RequiredArgsConstructor
public class ChainEventController {

    private final ChainEventService chainEventService;

    /**
     * API ghi nhận sự kiện thu hoạch cho lô sản xuất.
     * Chỉ chấp nhận vai trò VT-02 (Quản lý HTX) và VT-03 (Người ghi sự kiện).
     */
    @PostMapping("/harvest")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<ChainEventResponse>> recordHarvest(
            @Valid @RequestBody RecordHarvestEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }
    /**
     * API ghi nhận sự kiện đóng gói cho lô sản xuất.
     */
    @PostMapping("/packaging")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<ChainEventResponse>> recordPackaging(
            @Valid @RequestBody RecordPackagingEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response = chainEventService.recordPackagingEvent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }
    
    /**
     * API ghi nhận sự kiện vận chuyển cho lô hàng.
     * Chỉ chấp nhận vai trò VT-03 (Người ghi sự kiện).
     */
    @PostMapping("/transport")
    @PreAuthorize("hasRole('VT-03')")
    public ResponseEntity<ApiResult<ChainEventResponse>> recordTransport(
            @Valid @RequestBody RecordTransportEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response =
                chainEventService.recordTransportEvent(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * API tạo sự kiện đính chính thông tin đóng gói (giữ nguyên gốc).
     */
    @PostMapping("/packaging/{id}/correct")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<ChainEventResponse>> correctPackaging(
            @PathVariable("id") UUID originalEventId,
            @Valid @RequestBody CorrectPackagingEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response = chainEventService.correctPackagingEvent(originalEventId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }
    
}

