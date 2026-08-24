package vn.nguongocso.trace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.dto.request.CreateCodeRangeRequest;
import vn.nguongocso.trace.dto.response.CodeRangeResponse;
import vn.nguongocso.trace.dto.response.CodeRangeStatusResponse;
import vn.nguongocso.trace.dto.response.RemainingCodesResponse;
import vn.nguongocso.trace.service.CodeRangeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/code-ranges")
@RequiredArgsConstructor
/** Quản lý cấp dải mã truy xuất. */
public class CodeRangeController {
    private final CodeRangeService codeRangeService;

    /** Cấp mới một dải mã. */
    @PostMapping
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<CodeRangeResponse>> createCodeRange(
            @Valid @RequestBody CreateCodeRangeRequest request,
            @AuthenticationPrincipal CustomUserDetails admin) {
        CodeRangeResponse response = codeRangeService.createCodeRange(request, admin);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /** Lấy trạng thái sử dụng của các dải mã. */
    @GetMapping("/status")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<List<CodeRangeStatusResponse>>> getStatus() {
        return ResponseEntity.ok(ApiResult.success(codeRangeService.getCodeRangeStatus()));
    }

    /** Lấy số lượng mã truy xuất còn lại của một tổ chức. */
    @GetMapping("/organization/{organizationId}/remaining")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<RemainingCodesResponse>> getRemainingCodes(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (!"VT-01".equals(currentUser.getRoleCode())
                && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Bạn không có quyền truy cập dải mã của tổ chức khác.");
        }
        RemainingCodesResponse response = codeRangeService.getRemainingCodesForOrganization(organizationId);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
