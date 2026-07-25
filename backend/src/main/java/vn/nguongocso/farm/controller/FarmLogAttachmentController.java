package vn.nguongocso.farm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.response.AttachmentResponse;
import vn.nguongocso.farm.service.AttachmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farm-logs")
@RequiredArgsConstructor
public class FarmLogAttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/{logId}/attachments")
    @PreAuthorize("hasRole('VT-03')")
    public ResponseEntity<ApiResult<AttachmentResponse>> uploadAttachment(
            @PathVariable UUID logId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        AttachmentResponse response = attachmentService.uploadAttachment(logId, file, description, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(response));
    }

    @GetMapping("/{logId}/attachments")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<List<AttachmentResponse>>> getAttachments(
            @PathVariable UUID logId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResult.success(attachmentService.getAttachments(logId, userDetails)));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasRole('VT-03')")
    public ResponseEntity<ApiResult<Void>> deleteAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        attachmentService.deleteAttachment(attachmentId, userDetails);
        return ResponseEntity.noContent().build();
    }
}