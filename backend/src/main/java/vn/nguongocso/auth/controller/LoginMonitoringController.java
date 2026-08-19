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
import vn.nguongocso.auth.service.AccountLockService;
import vn.nguongocso.auth.service.LoginMonitoringService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

/**
 * REST Controller quản lý giám sát đăng nhập bất thường.
 * 
 * <p>
 * Cung cấp các API để:
 * - Xem lịch sử đăng nhập của tài khoản
 * - Xem danh sách bất thường đã phát hiện
 * - Khoá/mở khoá tạm tài khoản nghi vấn
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth/security")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LoginMonitoringController {
    
    private final LoginMonitoringService loginMonitoringService;
    private final AccountLockService accountLockService;
    
    /**
     * Lấy lịch sử đăng nhập của các tài khoản.
     * 
     * <p>
     * Quyền:
     * - VT-01 (Admin): xem toàn nền tảng, có thể lọc theo organizationId
     * - VT-02 (Quản lý tổ chức): xem chỉ trong tổ chức mình, organizationId luôn lấy từ token
     * - VT-03..VT-05: không có quyền xem (403)
     * </p>
     * 
     * @param userId           lọc theo tài khoản cụ thể (optional)
     * @param result           lọc theo kết quả: SUCCESS hoặc FAILED (optional)
     * @param organizationId   lọc theo tổ chức (chỉ VT-01)
     * @param startDate        lọc từ ngày (định dạng yyyy-MM-dd)
     * @param endDate          lọc đến ngày (định dạng yyyy-MM-dd)
     * @param page             số trang (mặc định 0)
     * @param size             kích thước trang (mặc định 10)
     * @return danh sách lịch sử đăng nhập
     */
    @GetMapping("/login-history")
    public ResponseEntity<ApiResult<PageResponse<LoginHistoryResponse>>> getLoginHistory(
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) String result,
        @RequestParam(required = false) UUID organizationId,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        PageResponse<LoginHistoryResponse> response = loginMonitoringService.getLoginHistory(
            userId,
            result,
            organizationId,
            startDate,
            endDate,
            pageable
        );
        
        return ResponseEntity.ok(ApiResult.success(response));
    }
    
    /**
     * Lấy danh sách bất thường đãng nhập đã phát hiện.
     * 
     * <p>
     * Quyền:
     * - VT-01 (Admin): xem toàn nền tảng, có thể lọc theo organizationId
     * - VT-02 (Quản lý tổ chức): xem chỉ trong tổ chức mình, organizationId luôn lấy từ token
     * - VT-03..VT-05: không có quyền xem (403)
     * </p>
     * 
     * @param status        lọc theo trạng thái: OPEN, DISMISSED (optional)
     * @param reasonCode    lọc theo nguyên nhân: REPEATED_FAILED_LOGIN, UNUSUAL_COUNTRY (optional)
     * @param organizationId lọc theo tổ chức (chỉ VT-01)
     * @param page          số trang (mặc định 0)
     * @param size          kích thước trang (mặc định 10)
     * @return danh sách bất thường đăng nhập
     */
    @GetMapping("/login-anomalies")
    public ResponseEntity<ApiResult<PageResponse<LoginAnomalyResponse>>> getLoginAnomalies(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String reasonCode,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) UUID organizationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        PageResponse<LoginAnomalyResponse> response = loginMonitoringService.getLoginAnomalies(
            status,
            reasonCode,
            organizationId,
            username,
            pageable
        );
        
        return ResponseEntity.ok(ApiResult.success(response));
    }

    @GetMapping("/suspicious-cases")
    public ResponseEntity<ApiResult<PageResponse<SuspiciousCaseResponse>>> getSuspiciousCases(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) UUID organizationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<SuspiciousCaseResponse> response = loginMonitoringService.getSuspiciousCases(
            status,
            organizationId,
            username,
            pageable
        );

        return ResponseEntity.ok(ApiResult.success(response));
    }
    
    /**
     * Khoá tạm một tài khoản nghi vấn.
     * 
     * <p>
     * Quyền:
     * - VT-01: khoá bất kỳ tài khoản nào
     * - VT-02: khoá chỉ tài khoản trong tổ chức mình
     * - Các vai trò khác: 403 Forbidden
     * </p>
     * 
     * <p>
     * Khi khoá thành công:
     * - Trạng thái User được cập nhật thành LOCKED
     * - Mọi access token hiện tại bị vô hiệu hoá
     * - Thông báo được gửi cho tài khoản chủ sở hữu
     * - Bản ghi AccountLock được tạo
     * </p>
     * 
     * @param accountId ID tài khoản cần khoá
     * @param request   thông tin khoá (anomalyId, reason)
     * @return thông tin tài khoản sau khi khoá
     */
    @PatchMapping("/accounts/{accountId}/lock")
    public ResponseEntity<ApiResult<AccountLockResponse>> lockAccount(
        @PathVariable UUID accountId,
        @Valid @RequestBody LockAccountRequest request
    ) {
        AccountLockResponse response = loginMonitoringService.lockAccount(
            accountId,
            request.getAnomalyId(),
            request.getReason(),
            request.getDays() == null ? 0 : request.getDays(),
            request.getHours() == null ? 0 : request.getHours(),
            request.getMinutes() == null ? 0 : request.getMinutes(),
            Boolean.TRUE.equals(request.isPermanent())
        );
        
        return ResponseEntity.ok(ApiResult.success(response));
    }
    
    /**
     * Mở khoá một tài khoản.
     * 
     * <p>
     * Quyền:
     * - VT-01: mở khoá bất kỳ tài khoản nào
     * - VT-02: mở khoá chỉ tài khoản trong tổ chức mình
     * - Các vai trò khác: 403 Forbidden
     * </p>
     * 
     * <p>
     * Khi mở khoá thành công:
     * - Trạng thái User được cập nhật thành ACTIVE
     * - Thông báo được gửi cho tài khoản chủ sở hữu
     * - Bản ghi AccountLock được cập nhật
     * </p>
     * 
     * @param accountId ID tài khoản cần mở khoá
     * @return thông tin tài khoản sau khi mở khoá
     */
    @PatchMapping("/accounts/{accountId}/unlock")
    public ResponseEntity<ApiResult<AccountLockResponse>> unlockAccount(
        @PathVariable UUID accountId
    ) {
        AccountLockResponse response = loginMonitoringService.unlockAccount(accountId);
        
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Đánh dấu tất cả bản ghi bất thường của một tài khoản là đã giải quyết.
     */
    @PatchMapping("/accounts/{accountId}/resolve-anomalies")
    public ResponseEntity<ApiResult<Void>> resolveUserAnomalies(
        @PathVariable UUID accountId
    ) {
        loginMonitoringService.markUserAnomaliesResolved(accountId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}
