package vn.nguongocso.trace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.trace.dto.request.CreateCodeRangeRequest;
import vn.nguongocso.trace.dto.response.CodeRangeResponse;
import vn.nguongocso.trace.dto.response.CodeRangeStatusResponse;
import vn.nguongocso.trace.service.CodeRangeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/code-ranges")
@RequiredArgsConstructor
public class CodeRangeController {

    private final CodeRangeService codeRangeService;

    @PostMapping
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<CodeRangeResponse>> createCodeRange(
            @Valid @RequestBody CreateCodeRangeRequest request,
            @AuthenticationPrincipal CustomUserDetails admin) {
        CodeRangeResponse response = codeRangeService.createCodeRange(request, admin);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<List<CodeRangeStatusResponse>>> getStatus() {
        return ResponseEntity.ok(ApiResult.success(codeRangeService.getCodeRangeStatus()));
    }

    @GetMapping("/organization/code-ranges")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<List<CodeRangeResponse>>> getOrganizationCodeRanges(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResult.success(codeRangeService.getCodeRangesForOrganization(user)));
    }
}
