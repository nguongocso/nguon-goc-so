package vn.nguongocso.organization.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.service.OrganizationMemberService;
import vn.nguongocso.permission.service.PermissionChecker;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organization/members")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
/** Quản lý thành viên trong tổ chức hiện tại. */
public class OrganizationMemberController {
    private final OrganizationMemberService permissionService;
    private final PermissionChecker permissionChecker;

    /** Lấy danh sách thành viên của tổ chức. */
    @GetMapping
    public ResponseEntity<ApiResult<List<OrganizationUserResponse>>> getMembers() {
        // permissionChecker.check("organization_user", "READ");
        return ResponseEntity.ok(ApiResult.success(
                permissionService.getMembersOfCurrentOrganization()));
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
