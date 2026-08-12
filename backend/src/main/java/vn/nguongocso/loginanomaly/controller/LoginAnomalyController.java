package vn.nguongocso.loginanomaly.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.loginanomaly.dto.response.LockLoginAnomalyResponse;
import vn.nguongocso.loginanomaly.dto.response.LoginAnomalyResponse;
import vn.nguongocso.loginanomaly.enums.LoginAnomalySeverity;
import vn.nguongocso.loginanomaly.service.LoginAnomalyService;

/**
 * API theo dõi đăng nhập bất thường.
 *
 * <p>
 * Quản trị viên nền tảng (VT-01) và quản lý hợp tác xã (VT-02) có quyền
 * truy cập. Quản lý hợp tác xã chỉ thấy dữ liệu thuộc tổ chức của mình (TC-03).
 * </p>
 */
@RestController
@RequestMapping("/api/v1/login-anomalies")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
public class LoginAnomalyController {

    private final LoginAnomalyService loginAnomalyService;

    /**
     * Danh sách đăng nhập bất thường.
     */
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<LoginAnomalyResponse>>> getAnomalies(
            @RequestParam(required = false) LoginAnomalySeverity severity,

            @RequestParam(required = false) UserStatus accountStatus,

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(defaultValue = "0") Integer page,

            @RequestParam(defaultValue = "20") Integer size) {

        return ResponseEntity.ok(ApiResult.success(
                loginAnomalyService.getAnomalies(
                        severity,
                        accountStatus,
                        keyword,
                        fromDate,
                        toDate,
                        page,
                        size)));
    }

    /**
     * Khóa tạm tài khoản của một bản ghi bất thường.
     */
    @PostMapping("/{id}/lock")
    public ResponseEntity<ApiResult<LockLoginAnomalyResponse>> lockAnomaly(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResult.success(
                loginAnomalyService.lockAnomaly(id)));
    }
}
