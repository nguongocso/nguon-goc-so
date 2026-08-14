package vn.nguongocso.organization.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.request.AddExistingUserRequest;
import vn.nguongocso.organization.dto.request.CreateOrganizationRequest;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.AvailableUserResponse;
import vn.nguongocso.organization.dto.response.CreateOrganizationMemberResponse;
import vn.nguongocso.organization.dto.response.OrganizationDetailResponse;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.dto.response.OrganizationResponse;
import vn.nguongocso.organization.service.OrganizationService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cung cấp các API quản lý tổ chức.
 */
@RestController
@RequestMapping("/api/v1/admin/organizations")
public class OrganizationController {
    private static final Logger log = LoggerFactory.getLogger(OrganizationController.class);

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * Lấy toàn bộ danh sách tổ chức.
     * Chỉ tài khoản VT-01 được phép truy cập.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-01')")
    public ResponseEntity<ApiResult<List<OrganizationResponse>>> getAllOrganizations() {
        log.info("Nhận yêu cầu lấy danh sách organization");
        List<OrganizationResponse> organizations = organizationService.getAllOrganizations();
        log.info("Lấy danh sách organization thành công, số lượng={}", organizations.size());
        return ResponseEntity.ok(ApiResult.success(organizations));
    }

    /**
     * Tạo mới một tổ chức cùng tài khoản quản lý mặc định.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VT-01')")
    public ResponseEntity<ApiResult<OrganizationResponse>> create(
            @Valid @RequestBody CreateOrganizationRequest request) {
        log.info("Nhận yêu cầu tạo organization với code={}", request.getOrganizationCode());
        OrganizationResponse response = organizationService.createOrganization(request);
        log.info("Tạo organization thành công với id={}", response.getOrganizationID());
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Admin cập nhật hồ sơ một tổ chức theo ID.
     */
    @PutMapping("/profile/{id}")
    @PreAuthorize("hasAnyRole('VT-01')")
    public ResponseEntity<ApiResult<OrganizationProfileResponse>> updateProfileByAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        log.info("Admin cập nhật hồ sơ organization id={}", id);
        OrganizationProfileResponse response = organizationService.updateOrganizationById(id, request);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy chi tiết tổ chức (kèm danh sách thành viên).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<OrganizationDetailResponse>> getOrganizationDetail(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResult.success(organizationService.getOrganizationDetail(id)));
    }

    /**
     * Thêm tài khoản mới vào tổ chức (tạo user mới).
     */
    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<CreateOrganizationMemberResponse>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {
        log.info("Nhận yêu cầu thêm thành viên mới vào organization={}, username={}", id, request.getUsername());
        CreateOrganizationMemberResponse response = organizationService.addMember(id, request);
        log.info("Thêm thành viên mới thành công. organization={}, username={}", id, response.getUsername());
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách user có sẵn để thêm vào tổ chức (cùng loại, chưa có trong tổ
     * chức).
     */
    @GetMapping("/{organizationId}/available-users")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<List<AvailableUserResponse>>> getAvailableUsers(
            @PathVariable UUID organizationId) {
        log.info("Lấy danh sách user có sẵn để thêm vào tổ chức {}", organizationId);
        List<AvailableUserResponse> users = organizationService.getAvailableUsersForOrganization(organizationId);
        return ResponseEntity.ok(ApiResult.success(users));
    }

    /**
     * Thêm user đã tồn tại vào tổ chức (giữ nguyên vai trò hiện tại hoặc chọn role
     * mới).
     */
    @PostMapping("/{organizationId}/add-existing-user")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> addExistingUser(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AddExistingUserRequest request) {
        log.info("Thêm user {} vào tổ chức {}", request.getUserId(), organizationId);
        OrganizationUserResponse response = organizationService.addExistingUserToOrganization(
                organizationId, request.getUserId(), request.getRoleId());
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Gán vai trò cho thành viên trong tổ chức (dùng cho tổ chức hiện tại từ
     * context).
     * Nếu cần gán cho tổ chức khác, có thể mở rộng thêm.
     */
    @PutMapping("/current/members/role")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<OrganizationUserResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request) {
        log.info("Gán vai trò cho user {} trong tổ chức hiện tại", request.getUserId());
        OrganizationUserResponse response = organizationService.assignRole(request);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}