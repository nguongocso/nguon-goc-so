package vn.nguongocso.export.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;
import vn.nguongocso.export.service.ProfileTemplateService;

/** Controller tổng hợp mẫu hồ sơ từ nhiều tổ chức (dành cho VT-04 xuất batch). */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations/batch")
@RequiredArgsConstructor
public class BatchProfileTemplateController {
    private final ProfileTemplateService profileTemplateService;

    /** Lấy danh sách mẫu hồ sơ từ nhiều tổ chức (dành cho VT-04 xuất batch). */
    @GetMapping("/templates")
    @PreAuthorize("hasRole('VT-04')")
    public ResponseEntity<ApiResult<List<ProfileTemplateResponse>>> getBatchTemplates(
            @RequestParam("organizationIds") List<UUID> organizationIds,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu lấy mẫu hồ sơ từ nhiều tổ chức: organizationIds={}, user={}",
                organizationIds, currentUser != null ? currentUser.getUsername() : "anonymous");
        List<ProfileTemplateResponse> templates = profileTemplateService.listTemplatesForMultipleOrganizations(
                organizationIds,
                currentUser);
        log.info(
                "Lấy mẫu hồ sơ từ nhiều tổ chức thành công: số tổ chức={}, tổng số mẫu={}",
                organizationIds.size(),
                templates.size());
        return ResponseEntity.ok(ApiResult.success(templates));
    }
}
