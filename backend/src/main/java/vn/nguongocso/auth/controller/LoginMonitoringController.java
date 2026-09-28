package vn.nguongocso.auth.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.dto.request.LockAccountRequest;
import vn.nguongocso.auth.dto.response.AccountLockResponse;
import vn.nguongocso.auth.dto.response.LoginAnomalyResponse;
import vn.nguongocso.auth.dto.response.LoginHistoryResponse;
import vn.nguongocso.auth.dto.response.SuspiciousCaseResponse;
import vn.nguongocso.auth.service.LoginMonitoringService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

/**
 * REST Controller quản lý giám sát đăng nhập bất thường.
 */
@RestController
@RequestMapping("/api/v1/auth/security")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LoginMonitoringController {
    private final LoginMonitoringService loginMonitoringService;

    /**
     * Lấy lịch sử đăng nhập của các tài khoản.
     */
    @GetMapping("/login-history")
    public ResponseEntity<ApiResult<PageResponse<LoginHistoryResponse>>> getLoginHistory(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<LoginHistoryResponse> response = loginMonitoringService.getLoginHistory(
                userId,
                result,
                organizationId,
                startDate,
                endDate,
                pageable);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách bất thường đãng nhập đã phát hiện.
     */
    @GetMapping("/login-anomalies")
    public ResponseEntity<ApiResult<PageResponse<LoginAnomalyResponse>>> getLoginAnomalies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reasonCode,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<LoginAnomalyResponse> response = loginMonitoringService.getLoginAnomalies(
                status,
                reasonCode,
                organizationId,
                username,
                pageable);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách bất thường đãng nhập đã phát hiện.
     */
    @GetMapping("/suspicious-cases")
    public ResponseEntity<ApiResult<PageResponse<SuspiciousCaseResponse>>> getSuspiciousCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<SuspiciousCaseResponse> response = loginMonitoringService.getSuspiciousCases(
                status,
                organizationId,
                username,
                pageable);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Khoá tạm một tài khoản nghi vấn
     */
    @PatchMapping("/accounts/{accountId}/lock")
    public ResponseEntity<ApiResult<AccountLockResponse>> lockAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody LockAccountRequest request) {
        AccountLockResponse response = loginMonitoringService.lockAccount(
                accountId,
                request.getAnomalyId(),
                request.getReason(),
                request.getDays() == null ? 0 : request.getDays(),
                request.getHours() == null ? 0 : request.getHours(),
                request.getMinutes() == null ? 0 : request.getMinutes(),
                Boolean.TRUE.equals(request.isPermanent()));

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Mở khoá một tài khoản.
     */
    @PatchMapping("/accounts/{accountId}/unlock")
    public ResponseEntity<ApiResult<AccountLockResponse>> unlockAccount(
            @PathVariable UUID accountId) {
        AccountLockResponse response = loginMonitoringService.unlockAccount(accountId);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Đánh dấu tất cả bản ghi bất thường của một tài khoản là đã giải quyết.
     */
    @PatchMapping("/accounts/{accountId}/resolve-anomalies")
    public ResponseEntity<ApiResult<Void>> resolveUserAnomalies(
            @PathVariable UUID accountId) {
        loginMonitoringService.markUserAnomaliesResolved(accountId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}
