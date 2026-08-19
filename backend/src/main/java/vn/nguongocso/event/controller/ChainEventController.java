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
import vn.nguongocso.event.dto.request.*;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.OfflineEventSyncResponse;
import vn.nguongocso.event.dto.response.ScanLookupResponse;
import vn.nguongocso.event.dto.response.StorageConditionResponse;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.OfflineSyncService;
import vn.nguongocso.permission.service.PermissionChecker;

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
    private final OfflineSyncService offlineSyncService;
    private final ChainEventService chainEventService;
    private final PermissionChecker permissionChecker;

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
     * API ghi nhận sự kiện sơ chế và phân loại cho lô sản xuất.
     * Chỉ chấp nhận vai trò VT-02 (Quản lý HTX) và VT-03 (Người ghi sự kiện).
     */
    @PostMapping("/preprocessing")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<ChainEventResponse>> recordPreprocessing(
            @Valid @RequestBody RecordPreprocessingEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response = chainEventService.recordPreprocessingEvent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * API tạo sự kiện đính chính thông tin sơ chế và phân loại (giữ nguyên gốc).
     */
    @PostMapping("/preprocessing/{id}/correct")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<ChainEventResponse>> correctPreprocessing(
            @PathVariable("id") UUID originalEventId,
            @Valid @RequestBody CorrectPreprocessingEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response = chainEventService.correctPreprocessingEvent(originalEventId, request, currentUser);
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

        ChainEventResponse response = chainEventService.recordTransportEvent(request, currentUser);
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

    /**
     * API ghi nhận sự kiện ngoài đồng từ thiết bị di động.
     * Chỉ chấp nhận vai trò VT-02 (Quản lý HTX) và VT-03 (Người ghi sự kiện).
     */
    @PostMapping("/mobile")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<ChainEventResponse>> recordMobileEvent(
            @Valid @RequestBody RecordMobileEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response = chainEventService.recordMobileEvent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * API đồng bộ sự kiện ngoại tuyến.
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<OfflineEventSyncResponse>> syncOfflineEvents(
            @Valid @RequestBody OfflineEventSyncRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        OfflineEventSyncResponse response = offlineSyncService.syncOfflineEvents(request, currentUser);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Tra cứu mã truy xuất trước khi mở biểu mẫu ghi sự kiện.
     *
     * Chức năng:
     * - Kiểm tra quyền sử dụng chức năng quét mã.
     * - Kiểm tra mã truy xuất có tồn tại.
     * - Kiểm tra mã đã gắn lô hàng.
     * - Kiểm tra quyền theo tổ chức.
     * - Kiểm tra trạng thái lô hàng.
     * - Trả về thông tin cần thiết để mở biểu mẫu ghi sự kiện.
     */
    @GetMapping("/scan-lookup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<ScanLookupResponse>> scanLookup(
            @RequestParam String codeValue,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // Có thể kiểm tra quyền READ nếu cần
        ScanLookupResponse response = chainEventService.scanLookup(codeValue, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * API ghi nhận mốc điều kiện bảo quản khi vận chuyển.
     * Chỉ chấp nhận vai trò VT-03 (Người ghi sự kiện) và VT-04 (Doanh nghiệp thu mua).
     */
    @PostMapping("/storage-condition")
    @PreAuthorize("hasAnyRole('VT-03', 'VT-04')")
    public ResponseEntity<ApiResult<StorageConditionResponse>> recordStorageCondition(
            @Valid @RequestBody StorageConditionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        StorageConditionResponse response = chainEventService.recordStorageCondition(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

}
