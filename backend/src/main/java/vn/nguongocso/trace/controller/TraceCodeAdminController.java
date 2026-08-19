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
import vn.nguongocso.trace.dto.request.LockTraceCodeRequest;
import vn.nguongocso.trace.dto.response.LockTraceCodeResponse;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeDetailResponse;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeResponse;
import vn.nguongocso.trace.service.SuspectDetectionService;

/**
 * Controller quản lý mã tem nghi vấn dành cho Quản trị viên nền tảng (VT-01).
 */
@RestController
@RequestMapping("/api/v1/admin/trace-codes")
@RequiredArgsConstructor
public class TraceCodeAdminController {

    private final SuspectDetectionService suspectDetectionService;

    /**
     * Lấy danh sách mã tem nghi vấn.
     */
    @GetMapping("/suspect")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<PageResponse<SuspectTraceCodeResponse>>> getSuspectTraceCodes(
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<SuspectTraceCodeResponse> response = suspectDetectionService
                .getSuspectTraceCodes(minScore, status, page, size);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy chi tiết mã tem nghi vấn.
     */
    @GetMapping("/{traceCodeId}/suspect-detail")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<SuspectTraceCodeDetailResponse>> getSuspectDetail(
            @PathVariable UUID traceCodeId) {

        SuspectTraceCodeDetailResponse response = suspectDetectionService
                .getSuspectDetail(traceCodeId);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Khóa mã tem nghi vấn.
     */
    @PostMapping("/{traceCodeId}/lock")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<LockTraceCodeResponse>> lockTraceCode(
            @PathVariable UUID traceCodeId,
            @Valid @RequestBody LockTraceCodeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        LockTraceCodeResponse response = suspectDetectionService.lockTraceCode(
                traceCodeId,
                request,
                currentUser.getUserId(),
                currentUser.getFullName());

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Mở khóa mã tem.
     */
    @PostMapping("/{traceCodeId}/unlock")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<LockTraceCodeResponse>> unlockTraceCode(
            @PathVariable UUID traceCodeId,
            @Valid @RequestBody LockTraceCodeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        LockTraceCodeResponse response = suspectDetectionService.unlockTraceCode(
                traceCodeId,
                request.getReason(),
                currentUser.getUserId(),
                currentUser.getFullName());

        return ResponseEntity.ok(ApiResult.success(response));
    }
}