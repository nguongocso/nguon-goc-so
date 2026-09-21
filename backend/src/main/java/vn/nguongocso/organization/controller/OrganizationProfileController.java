package vn.nguongocso.organization.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.dto.response.RecipientOrganizationResponse;
import vn.nguongocso.organization.service.OrganizationService;

/**
 * Quản lý hồ sơ tổ chức hiện tại.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationProfileController {

    private final OrganizationService organizationService;

    /**
     * Lấy hồ sơ tổ chức hiện tại.
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<OrganizationProfileResponse>> getProfile() {
        log.info("Lấy hồ sơ tổ chức hiện tại");
        return ResponseEntity.ok(ApiResult.success(organizationService.getCurrentOrganizationProfile()));
    }

    /**
     * Cập nhật hồ sơ tổ chức hiện tại.
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<OrganizationProfileResponse>> updateProfile(
            @Valid @RequestBody OrganizationUpdateRequest request) {

        log.info("Cập nhật hồ sơ tổ chức hiện tại");
        return ResponseEntity.ok(ApiResult.success(organizationService.updateCurrentOrganization(request)));
    }

    /**
     * Danh sách tổ chức nhận cho dropdown phiếu bàn giao.
     * Chỉ trả các tổ chức Doanh nghiệp thu mua (VT-04, loại ENTERPRISE), ACTIVE
     * và khác tổ chức hiện tại.
     */
    @GetMapping("/recipient-organizations")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<List<RecipientOrganizationResponse>>> getRecipientOrganizations() {
        log.info("Lấy danh sách tổ chức nhận cho phiếu bàn giao");
        return ResponseEntity.ok(ApiResult.success(organizationService.getRecipientOrganizations()));
    }
}
