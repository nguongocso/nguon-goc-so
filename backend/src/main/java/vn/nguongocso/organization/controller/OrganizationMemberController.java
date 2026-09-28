package vn.nguongocso.organization.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.request.DeactivateMemberRequest;
import vn.nguongocso.auth.dto.request.ReactivateMemberRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.service.OrganizationMemberService;
import vn.nguongocso.permission.service.PermissionChecker;

/** Quản lý thành viên trong tổ chức hiện tại. */
@RestController
@RequestMapping("/api/v1/organization/members")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
public class OrganizationMemberController {
    private final OrganizationMemberService permissionService;

    private final PermissionChecker permissionChecker;

    /** Lấy danh sách thành viên của tổ chức theo trạng thái membership. */
    @GetMapping
    public ResponseEntity<ApiResult<List<OrganizationUserResponse>>> getMembers(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResult.success(
                permissionService.getMembersOfCurrentOrganization(status)));
    }

    /** Vô hiệu hóa thành viên: thu hồi quyền, chấm dứt phiên, ghi audit log. */
    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> deactivateMember(
            @PathVariable UUID userId,
            @Valid @RequestBody DeactivateMemberRequest request) {
        return ResponseEntity.ok(ApiResult.success(
                permissionService.deactivateMember(userId, request)));
    }

    /** Kích hoạt lại thành viên đã ngừng hoạt động. */
    @PatchMapping("/{userId}/reactivate")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> reactivateMember(
            @PathVariable UUID userId,
            @Valid @RequestBody ReactivateMemberRequest request) {
        return ResponseEntity.ok(ApiResult.success(
                permissionService.reactivateMember(userId, request)));
    }

    /** Gán vai trò cho thành viên trong tổ chức. */
    @PutMapping("/roles")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(ApiResult.success(
                permissionService.assignRole(request)));
    }

    /** Thêm thành viên mới vào tổ chức. */
    @PostMapping
    public ResponseEntity<ApiResult<OrganizationUserResponse>> addMember(
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(ApiResult.success(
                permissionService.addMember(request)));
    }
}
