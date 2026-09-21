package vn.nguongocso.alert.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.alert.dto.response.ActivityLogResponse;
import vn.nguongocso.alert.dto.request.ActivityLogExportFilterRequest;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.alert.dto.response.ActivityLogExportDownload;
import vn.nguongocso.alert.dto.response.ActivityLogExportJobResponse;
import vn.nguongocso.alert.dto.response.ActivityLogExportResult;
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
            @RequestParam(required = false) String objectType,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);

        log.info(
                "User {} thuộc tổ chức {} yêu cầu xem lịch sử hoạt động từ IP {}",
                currentUser.getUsername(),
                currentUser.getOrganizationCode(),
                ipAddress);

        PageResponse<ActivityLogResponse> response = activityLogService.getActivityLogs(
                page,
                size,
                action,
                actorName,
                startDate,
                endDate,
                objectType,
                currentUser);

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
     * Tạo export trực tiếp hoặc job nền cho snapshot nhật ký khớp bộ lọc.
     */
    @PostMapping("/exports")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<?> exportActivityLogs(
            @Valid @RequestBody ActivityLogExportFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ActivityLogExportResult result = activityLogExportService.requestExport(filter, currentUser);
        if ("ASYNC".equals(result.getMode())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResult.success(HttpStatus.ACCEPTED.value(), result.getJob()));
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("activity-logs-" + timestamp + ".csv", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(result.getCsvBytes());
    }

    /** Lấy trạng thái yêu cầu export nền thuộc tổ chức hiện tại. */
    @GetMapping("/exports/{exportId}")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ActivityLogExportJobResponse>> getExportJob(
            @PathVariable UUID exportId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResult.success(activityLogExportService.getJob(exportId, currentUser)));
    }

    /** Tải tệp CSV của export job đã hoàn tất. */
    @GetMapping("/exports/{exportId}/download")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<Resource> downloadExportJob(
            @PathVariable UUID exportId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ActivityLogExportDownload download = activityLogExportService.getDownload(exportId, currentUser);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.getFileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(download.getFileSize())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(new FileSystemResource(download.getPath()));
    }

    /**
     * Lấy địa chỉ IP thực tế của client.
     */
    private String getClientIpAddress(HttpServletRequest request) {

        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");

        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
