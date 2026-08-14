package vn.nguongocso.backup.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.backup.dto.request.BackupScheduleRequest;
import vn.nguongocso.backup.dto.response.BackupHistoryResponse;
import vn.nguongocso.backup.dto.response.BackupScheduleResponse;
import vn.nguongocso.backup.enums.BackupOperationType;
import vn.nguongocso.backup.enums.BackupStatus;
import vn.nguongocso.backup.service.BackupService;
import vn.nguongocso.backup.service.RestoreService;
import vn.nguongocso.common.ApiResult;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/v1/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VT-01')")
/** Quản lý cấu hình và lịch sử sao lưu. */
public class BackupController {
    private final BackupService backupService;
    private final RestoreService restoreService;

    /** Lấy lịch sao lưu đang áp dụng. */
    @GetMapping("/schedules")
    public ResponseEntity<ApiResult<List<BackupScheduleResponse>>> getSchedules() {
        BackupScheduleResponse active = backupService.getActiveSchedule();
        List<BackupScheduleResponse> list = active != null ? List.of(active) : List.of();
        return ResponseEntity.ok(ApiResult.success(list));
    }

    /** Cấu hình lại lịch sao lưu. */
    @PostMapping("/schedules")
    public ResponseEntity<ApiResult<BackupScheduleResponse>> configureSchedule(
            @Valid @RequestBody BackupScheduleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BackupScheduleResponse response = backupService.configureSchedule(request, userDetails.getUser());
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /** Kích hoạt sao lưu thủ công. */
    @PostMapping("/trigger")
    public ResponseEntity<ApiResult<BackupHistoryResponse>> triggerBackup(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BackupHistoryResponse response = backupService.triggerManualBackup(userDetails.getUser());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResult.success(HttpStatus.ACCEPTED.value(), response));
    }

    /** Lấy lịch sử sao lưu theo phân trang. */
    @GetMapping("/history")
    public ResponseEntity<ApiResult<Page<BackupHistoryResponse>>> getHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "operationType", required = false) BackupOperationType operationType,
            @RequestParam(value = "status", required = false) BackupStatus status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BackupHistoryResponse> history = backupService.getHistory(operationType, status, pageable);
        return ResponseEntity.ok(ApiResult.success(history));
    }

    /** Tải xuống tệp sao lưu. */
    @GetMapping("/history/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadBackup(@PathVariable("id") Integer id) {
        File file = backupService.getBackupFile(id);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    /** Xóa bản sao lưu. */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResult<Void>> deleteBackup(@PathVariable("id") Integer id) {
        backupService.deleteBackup(id);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    /** Khôi phục dữ liệu từ bản sao lưu. */
    @PostMapping("/history/{id}/restore")
    public ResponseEntity<ApiResult<BackupHistoryResponse>> triggerRestore(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BackupHistoryResponse response = restoreService.triggerRestore(id, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResult.success(HttpStatus.ACCEPTED.value(), response));
    }
}
