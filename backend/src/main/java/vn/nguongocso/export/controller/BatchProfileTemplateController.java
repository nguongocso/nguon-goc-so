package vn.nguongocso.export.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;
import vn.nguongocso.export.service.ProfileTemplateService;

import java.util.List;
import java.util.UUID;

/**
 * Controller tổng hợp mẫu hồ sơ từ nhiều tổ chức (dành cho VT-04 xuất batch).
 *
 * <p>Tách riêng khỏi {@code ProfileTemplateController} vì controller đó dùng
 * {@code @RequestMapping("/api/v1/organizations/{orgId}/profile-templates")};
 * nếu đặt {@code @GetMapping("/batch/templates")} bên trong thì Spring sẽ nhầm
 * chuỗi "batch" thành {@code orgId} gây lỗi 400/404. Endpoint riêng này dùng
 * đường dẫn tuyệt đối không chứa {@code {orgId}}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations/batch")
@RequiredArgsConstructor
public class BatchProfileTemplateController {

    private final ProfileTemplateService profileTemplateService;

    /**
     * Lấy danh sách mẫu hồ sơ từ nhiều tổ chức (dành cho VT-04 xuất batch).
     * Cho phép VT-04 xem các mẫu của các HTX có trong danh sách lô hàng cần xuất.
     *
     * <p>Ví dụ: {@code GET /api/v1/organizations/batch/templates?organizationIds=id1&organizationIds=id2}</p>
     */
    @GetMapping("/templates")
    @PreAuthorize("hasRole('VT-04')")
    public ResponseEntity<ApiResult<List<ProfileTemplateResponse>>> getBatchTemplates(
            @RequestParam("organizationIds") List<UUID> organizationIds,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu lấy mẫu hồ sơ từ nhiều tổ chức: organizationIds={}, user={}",
                organizationIds, currentUser != null ? currentUser.getUsername() : "anonymous");
        List<ProfileTemplateResponse> templates = profileTemplateService.listTemplatesForMultipleOrganizations(organizationIds, currentUser);
        log.info("Lấy mẫu hồ sơ từ nhiều tổ chức thành công: số tổ chức={}, tổng số mẫu={}", organizationIds.size(), templates.size());
        return ResponseEntity.ok(ApiResult.success(templates));
    }
}
