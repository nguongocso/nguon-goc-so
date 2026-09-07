package vn.nguongocso.farm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.response.MilestoneReminderResponse;
import vn.nguongocso.farm.dto.response.MilestoneScanResult;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.farm.service.MilestoneReminderService;
import vn.nguongocso.organization.constant.RoleCode;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý nhắc lịch ghi nhật ký theo mốc canh tác bắt buộc (NCL-03-CN-007).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/milestone-reminders")
@RequiredArgsConstructor
public class MilestoneReminderController {

    private final MilestoneReminderService milestoneReminderService;

    /**
     * Kích hoạt quét mốc quá hạn và tạo nhắc việc.
     * Cho phép Quản trị viên (VT-01) hoặc Quản lý hợp tác xã (VT-02).
     */
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<MilestoneScanResult>> triggerScan(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        MilestoneScanResult result;
        if (currentUser != null && RoleCode.ORG_MANAGER.equals(currentUser.getRoleCode())) {
            result = milestoneReminderService.scanOverdueMilestonesForOrganization(currentUser.getOrganizationId());
        } else {
            result = milestoneReminderService.scanOverdueMilestones();
        }

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * Lấy danh sách nhắc việc có phân trang.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<PageResponse<MilestoneReminderResponse>>> getReminders(
            @RequestParam(required = false) MilestoneReminderStatus status,
            @RequestParam(required = false) UUID lotId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PageResponse<MilestoneReminderResponse> response = milestoneReminderService.getReminders(
                status, lotId, pageable, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách các nhắc việc đang mở (OPEN) của người dùng hiện tại (dành cho Mobile & Dashboard).
     */
    @GetMapping("/my-active")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<List<MilestoneReminderResponse>>> getMyActiveReminders(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<MilestoneReminderResponse> response = milestoneReminderService.getMyActiveReminders(currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
