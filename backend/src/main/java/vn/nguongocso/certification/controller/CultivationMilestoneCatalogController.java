package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneCatalogRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneCatalogResponse;
import vn.nguongocso.certification.service.CultivationMilestoneCatalogService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

/**
 * Controller for managing the cultivation milestone catalog.
 * Story: NCL-09-CN-011
 */
@RestController
@RequestMapping("/api/v1/cultivation-milestones")
@RequiredArgsConstructor
public class CultivationMilestoneCatalogController {

    private final CultivationMilestoneCatalogService milestoneService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResult<PageResponse<CultivationMilestoneCatalogResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CultivationMilestoneCatalogResponse> result =
                milestoneService.searchMilestones(keyword, status, activityType, pageable, currentUser);
        return ApiResult.success(PageResponse.from(result, result.getContent()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<CultivationMilestoneCatalogResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(milestoneService.getMilestone(id, currentUser));
    }

    @PostMapping
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<CultivationMilestoneCatalogResponse>> create(
            @Valid @RequestBody CultivationMilestoneCatalogRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        CultivationMilestoneCatalogResponse response =
                milestoneService.createMilestone(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<CultivationMilestoneCatalogResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CultivationMilestoneCatalogRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(milestoneService.updateMilestone(id, request, currentUser));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<Void> disable(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        milestoneService.disableMilestone(id, currentUser);
        return ApiResult.success(null);
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<CultivationMilestoneCatalogResponse> enable(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(milestoneService.enableMilestone(id, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        milestoneService.deleteMilestone(id, currentUser);
        return ApiResult.success(null);
    }
}
