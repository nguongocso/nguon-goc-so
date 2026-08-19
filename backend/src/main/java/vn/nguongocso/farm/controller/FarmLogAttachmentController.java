package vn.nguongocso.farm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.response.AttachmentResponse;
import vn.nguongocso.farm.service.AttachmentService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farm-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
/** Quản lý tệp đính kèm của nhật ký canh tác. */
public class FarmLogAttachmentController {

    private final AttachmentService attachmentService;
    private final PermissionChecker permissionChecker;

    /** Tải lên tệp đính kèm cho nhật ký. */
    @PostMapping("/{logId}/attachments")
    public ResponseEntity<ApiResult<AttachmentResponse>> uploadAttachment(
            @PathVariable UUID logId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        permissionChecker.check("FARM_LOG", "UPDATE");
        AttachmentResponse response = attachmentService.uploadAttachment(logId, file, description, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(response));
    }

    /** Lấy danh sách tệp đính kèm của nhật ký. */
    @GetMapping("/{logId}/attachments")
    public ResponseEntity<ApiResult<List<AttachmentResponse>>> getAttachments(
            @PathVariable UUID logId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("FARM_LOG", "READ");
        return ResponseEntity.ok(ApiResult.success(attachmentService.getAttachments(logId, userDetails)));
    }

    /** Xóa một tệp đính kèm. */
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<ApiResult<Void>> deleteAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("FARM_LOG", "DELETE");
        attachmentService.deleteAttachment(attachmentId, userDetails);
        return ResponseEntity.noContent().build();
    }

    /** Xem / hiển thị tệp đính kèm (ảnh, PDF). */
    @GetMapping("/attachments/{attachmentId}/view")
    public ResponseEntity<Resource> viewAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("FARM_LOG", "READ");
        var pair = attachmentService.getAttachmentForView(attachmentId, userDetails);
        Resource resource = pair.getKey();
        MediaType contentType = pair.getValue();
        return ResponseEntity.ok()
                .contentType(contentType)
                .body(resource);
    }

    /** Tải xuống tệp đính kèm. */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("FARM_LOG", "READ");
        var result = attachmentService.getAttachmentForDownload(attachmentId, userDetails);
        return ResponseEntity.ok()
                .contentType(result.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\"")
                .body(result.resource());
    }
}
