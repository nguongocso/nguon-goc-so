package vn.nguongocso.alert.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.alert.dto.response.ActivityLogResponse;
import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.alert.service.ActivityLogExportService;
import vn.nguongocso.alert.service.ActivityLogService;

/**
 * Controller xử lý các hoạt động liên quan đến lịch sử hoạt động của tổ chức.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final ActivityLogExportService activityLogExportService;

    /**
     * API lấy danh sách lịch sử hoạt động của tổ chức hiện tại.
     *
     * @param page        Trang hiện tại (mặc định là 0)
     * @param size        Số bản ghi trên 1 trang (mặc định là 10)
     * @param action      Lọc theo loại thao tác (không bắt buộc)
     * @param actorName   Lọc theo tên hoặc username người thực hiện (không bắt buộc)
     * @param startDate   Lọc từ ngày (định dạng yyyy-MM-dd, không bắt buộc)
     * @param endDate     Lọc đến ngày (định dạng yyyy-MM-dd, không bắt buộc)
     * @param currentUser Thông tin tài khoản đang đăng nhập lấy từ JWT token
     * @param request     HTTP request dùng để lấy địa chỉ IP của client
     */
    @GetMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<PageResponse<ActivityLogResponse>>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);

        log.info(
                "User {} thuộc tổ chức {} yêu cầu xem lịch sử hoạt động từ IP {}",
                currentUser.getUsername(),
                currentUser.getOrganizationCode(),
                ipAddress
        );

        PageResponse<ActivityLogResponse> response =
                activityLogService.getActivityLogs(
                        page,
                        size,
                        action,
                        actorName,
                        startDate,
                        endDate,
                        currentUser
                );

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Đếm số bản ghi nhật ký khớp bộ lọc trước khi xuất.
     */
    @PostMapping("/exports/preview")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ActivityLogExportPreviewResponse>> previewExport(
            @Valid @RequestBody ActivityLogExportFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ActivityLogExportPreviewResponse response = activityLogExportService.preview(filter, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Xuất trực tiếp snapshot nhật ký hoạt động khớp bộ lọc ra tệp CSV.
     */
    @PostMapping("/exports")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<byte[]> exportActivityLogs(
            @Valid @RequestBody ActivityLogExportFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        byte[] csvBytes = activityLogExportService.exportCsv(filter, currentUser);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("activity-logs-" + timestamp + ".csv", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvBytes);
    }

    /**
     * Lấy địa chỉ IP thực tế của client.
     *
     * Ưu tiên:
     * 1. X-Forwarded-For
     * 2. X-Real-IP
     * 3. request.getRemoteAddr()
     */
    private String getClientIpAddress(HttpServletRequest request) {

        String xForwardedFor =
                request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp =
                request.getHeader("X-Real-IP");

        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
