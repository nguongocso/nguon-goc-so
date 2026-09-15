package vn.nguongocso.export.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.export.dto.request.CreateProfileTemplateRequest;
import vn.nguongocso.export.dto.request.UpdateProfileTemplateRequest;
import vn.nguongocso.export.dto.response.FieldGroupDefinition;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;
import vn.nguongocso.export.service.ProfileTemplateService;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý cấu hình mẫu hồ sơ truy xuất nguồn gốc theo yêu cầu đối tác.
 * User Story: NCL-07-CN-007.
 * Phân quyền: Dành riêng cho Quản lý hợp tác xã (VT-02) trong phạm vi tổ chức của mình (QTN-01).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations/{orgId}/profile-templates")
@RequiredArgsConstructor
public class ProfileTemplateController {

    private final ProfileTemplateService profileTemplateService;

    /**
     * Lấy danh mục tất cả các trường dữ liệu hệ thống hỗ trợ cấu hình.
     * Hỗ trợ cả /catalog và /available-fields để đồng bộ với các phiên bản frontend.
     */
    @GetMapping({"/catalog", "/available-fields"})
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<List<FieldGroupDefinition>>> getCatalog(
            @PathVariable UUID orgId) {
        log.info("Nhận yêu cầu lấy danh mục trường dữ liệu mẫu hồ sơ: orgId={}", orgId);
        List<FieldGroupDefinition> catalog = profileTemplateService.getAllAvailableFields();
        log.info("Lấy danh mục trường thành công: orgId={}, tổng số nhóm={}", orgId, catalog.size());
        return ResponseEntity.ok(ApiResult.success(catalog));
    }

    /**
     * Lấy danh sách mẫu hồ sơ thuộc tổ chức của người dùng (TC-04).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<List<ProfileTemplateResponse>>> listTemplates(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu lấy danh sách mẫu hồ sơ: orgId={}, user={}", orgId, currentUser != null ? currentUser.getUsername() : "anonymous");
        List<ProfileTemplateResponse> templates = profileTemplateService.listTemplates(orgId, currentUser);
        log.info("Lấy danh sách mẫu hồ sơ thành công: orgId={}, số lượng={}", orgId, templates.size());
        return ResponseEntity.ok(ApiResult.success(templates));
    }

    /**
     * Tạo mới mẫu hồ sơ truy xuất (TC-01, TC-02).
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ProfileTemplateResponse>> createTemplate(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateProfileTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu tạo mẫu hồ sơ: orgId={}, name={}, partnerName={}, isDefault={}, user={}",
                orgId, request.getName(), request.getPartnerName(), request.getIsDefault(), currentUser != null ? currentUser.getUsername() : "anonymous");
        ProfileTemplateResponse response = profileTemplateService.createTemplate(orgId, request, currentUser);
        log.info("Tạo mẫu hồ sơ thành công: orgId={}, templateId={}, isDefault={}", orgId, response.getId(), response.isDefault());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Lấy mẫu hồ sơ mặc định của tổ chức (TC-03).
     */
    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<ProfileTemplateResponse>> getDefaultTemplate(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu lấy mẫu hồ sơ mặc định: orgId={}, user={}", orgId, currentUser != null ? currentUser.getUsername() : "anonymous");
        ProfileTemplateResponse response = profileTemplateService.getDefaultTemplateResponse(orgId, currentUser);
        log.info("Lấy mẫu mặc định thành công: orgId={}, templateId={}", orgId, response.getId());
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy thông tin chi tiết mẫu hồ sơ theo ID (TC-04).
     */
    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<ProfileTemplateResponse>> getTemplate(
            @PathVariable UUID orgId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu lấy chi tiết mẫu hồ sơ: orgId={}, templateId={}, user={}",
                orgId, templateId, currentUser != null ? currentUser.getUsername() : "anonymous");
        ProfileTemplateResponse response = profileTemplateService.getTemplate(orgId, templateId, currentUser);
        log.info("Lấy chi tiết mẫu hồ sơ thành công: orgId={}, templateId={}, name={}, isDefault={}",
                orgId, templateId, response.getName(), response.isDefault());
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Cập nhật mẫu hồ sơ (TC-02).
     */
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ProfileTemplateResponse>> updateTemplate(
            @PathVariable UUID orgId,
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateProfileTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu cập nhật mẫu hồ sơ: orgId={}, templateId={}, isDefault={}, user={}",
                orgId, templateId, request.getIsDefault(), currentUser != null ? currentUser.getUsername() : "anonymous");
        ProfileTemplateResponse response = profileTemplateService.updateTemplate(orgId, templateId, request, currentUser);
        log.info("Cập nhật mẫu hồ sơ thành công: orgId={}, templateId={}, isDefault={}", orgId, templateId, response.isDefault());
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Xóa mẫu hồ sơ.
     */
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<Void>> deleteTemplate(
            @PathVariable UUID orgId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu xóa mẫu hồ sơ: orgId={}, templateId={}, user={}",
                orgId, templateId, currentUser != null ? currentUser.getUsername() : "anonymous");
        profileTemplateService.deleteTemplate(orgId, templateId, currentUser);
        log.info("Xóa mẫu hồ sơ thành công: orgId={}, templateId={}", orgId, templateId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}
