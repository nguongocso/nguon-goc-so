package vn.nguongocso.trace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.trace.dto.response.CodeRangeResponse;
import vn.nguongocso.trace.service.CodeRangeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
public class OrganizationCodeRangeController {

    private final CodeRangeService codeRangeService;

    @GetMapping("/code-ranges")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<List<CodeRangeResponse>>> getOrganizationCodeRanges(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<CodeRangeResponse> response = codeRangeService.getCodeRangesForOrganization(user);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}