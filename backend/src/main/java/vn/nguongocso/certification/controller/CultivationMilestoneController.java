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
import vn.nguongocso.certification.dto.request.CultivationMilestoneRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneResponse;
import vn.nguongocso.certification.service.CultivationMilestoneService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

import java.util.UUID;

/**
 * Controller quản lý mốc canh tác (bảng hợp nhất).
 * Story: NCL-09-CN-011
 */
@RestController
@RequestMapping("/api/v1/cultivation-milestones")
@RequiredArgsConstructor
public class CultivationMilestoneController {

    private final CultivationMilestoneService milestoneService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResult<PageResponse<CultivationMilestoneResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID standardId,
            @RequestParam(defaultValue = "false") boolean globalOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CultivationMilestoneResponse> result =
                milestoneService.searchMilestones(keyword, activityType, categoryId, standardId,
                        globalOnly, pageable, currentUser);
        return ApiResult.success(PageResponse.from(result, result.getContent()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<CultivationMilestoneResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(milestoneService.getMilestone(id, currentUser));
    }

    @PostMapping
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<CultivationMilestoneResponse>> create(
            @Valid @RequestBody CultivationMilestoneRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        CultivationMilestoneResponse response =
                milestoneService.createMilestone(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<CultivationMilestoneResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CultivationMilestoneRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(milestoneService.updateMilestone(id, request, currentUser));
    }
}
