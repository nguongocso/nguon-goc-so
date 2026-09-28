package vn.nguongocso.certification.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.IssueInspectionResultEntryLinkRequest;
import vn.nguongocso.certification.dto.response.InspectionResultEntryLinkResponse;
import vn.nguongocso.certification.service.InspectionResultEntryLinkService;
import vn.nguongocso.common.ApiResult;

/**
 * Controller quản lý liên kết nhập kết quả kiểm nghiệm dành cho Quản lý HTX (VT-02).
 */
@RestController
@RequestMapping("/api/v1/inspection-requests/{requestId}/result-entry-links")
@RequiredArgsConstructor
public class InspectionResultEntryLinkController {

    private final InspectionResultEntryLinkService linkService;

    /**
     * Cấp hoặc cấp lại liên kết nhập kết quả kiểm nghiệm cho đơn vị kiểm nghiệm.
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<InspectionResultEntryLinkResponse>> issueLink(
            @PathVariable UUID requestId,
            @Valid @RequestBody IssueInspectionResultEntryLinkRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        InspectionResultEntryLinkResponse response = linkService.issueLink(requestId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Xem thông tin trạng thái liên kết mới nhất của yêu cầu kiểm nghiệm (không trả token bí mật hay URL).
     */
    @GetMapping("/latest")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<InspectionResultEntryLinkResponse>> getLatestLink(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        InspectionResultEntryLinkResponse response = linkService.getLatestLink(requestId, currentUser);
        return ResponseEntity.ok(
                ApiResult.success(HttpStatus.OK.value(), response));
    }
}
