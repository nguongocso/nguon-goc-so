package vn.nguongocso.organization.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.request.DeactivateMemberRequest;
import vn.nguongocso.auth.dto.request.ReactivateMemberRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.auth.dto.response.ReplacementCandidateResponse;
import vn.nguongocso.auth.dto.response.UnfinishedLotsResponse;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.service.OrganizationMemberService;
import vn.nguongocso.permission.service.PermissionChecker;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organization/members")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
/** Quản lý thành viên trong tổ chức hiện tại. */
public class OrganizationMemberController {
    private final OrganizationMemberService permissionService;
    private final PermissionChecker permissionChecker;

    /** Lấy danh sách thành viên của tổ chức (lọc theo trạng thái membership). */
    @GetMapping
    public ResponseEntity<ApiResult<List<OrganizationUserResponse>>> getMembers(
            @RequestParam(required = false) String status) {
        // permissionChecker.check("organization_user", "READ");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.getMembersOfCurrentOrganization(status)));
    }

    /**
     * Precheck các lô chưa hoàn thành đang phân công cho thành viên
     * trước khi vô hiệu hóa (QTN-32).
     */
    @GetMapping("/{userId}/unfinished-lots")
    public ResponseEntity<ApiResult<UnfinishedLotsResponse>> getUnfinishedLots(
            @PathVariable UUID userId) {
        // permissionChecker.check("organization_user", "READ");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.getUnfinishedLotsOfMember(userId)));
    }

    /** Danh sách thành viên đủ điều kiện thay thế thành viên sắp vô hiệu hóa. */
    @GetMapping("/{userId}/replacement-candidates")
    public ResponseEntity<ApiResult<List<ReplacementCandidateResponse>>> getReplacementCandidates(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID lotId,
            @RequestParam(required = false) String keyword) {
        // permissionChecker.check("organization_user", "READ");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.getReplacementCandidates(userId, lotId, keyword)));
    }

    /** Vô hiệu hóa thành viên: thu hồi quyền, chấm dứt phiên, ghi audit log (QTN-32). */
    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> deactivateMember(
            @PathVariable UUID userId,
            @Valid @RequestBody DeactivateMemberRequest request) {
        // permissionChecker.check("organization_user", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.deactivateMember(userId, request)));
    }

    /** Kích hoạt lại thành viên đã ngừng hoạt động (bắt buộc lý do). */
    @PatchMapping("/{userId}/reactivate")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> reactivateMember(
            @PathVariable UUID userId,
            @Valid @RequestBody ReactivateMemberRequest request) {
        // permissionChecker.check("organization_user", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.reactivateMember(userId, request)));
    }

    /** Gán vai trò cho thành viên. */
    @PutMapping("/roles")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request) {
        // permissionChecker.check("organization_user", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.assignRole(request)));
    }

    /** Thêm thành viên mới vào tổ chức. */
    @PostMapping
    public ResponseEntity<ApiResult<OrganizationUserResponse>> addMember(
            @Valid @RequestBody AddMemberRequest request) {
        // permissionChecker.check("organization_user", "CREATE");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.addMember(request)));
    }
}
