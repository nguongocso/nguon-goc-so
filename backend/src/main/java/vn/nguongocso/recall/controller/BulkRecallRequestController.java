package vn.nguongocso.recall.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.recall.dto.request.ApproveBulkRecallRequest;
import vn.nguongocso.recall.dto.request.CreateBulkRecallRequest;
import vn.nguongocso.recall.dto.request.RejectBulkRecallRequest;
import vn.nguongocso.recall.dto.response.BulkRecallRequestResponse;
import vn.nguongocso.recall.service.BulkRecallRequestService;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;

/**
 * Controller quản lý yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011, NCL-08-CN-012).
 */
@RestController
@RequestMapping("/api/v1/recall-requests/bulk")
@RequiredArgsConstructor
public class BulkRecallRequestController {

    private final BulkRecallRequestService bulkRecallRequestService;
    private final PermissionChecker permissionChecker;

    /**
     * Tạo yêu cầu thu hồi hàng loạt.
     *
     * POST /api/v1/recall-requests/bulk
     */
    @PostMapping
    public ResponseEntity<ApiResult<BulkRecallRequestResponse>> createBulkRecallRequest(
            @Valid @RequestBody CreateBulkRecallRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "CREATE");

        BulkRecallRequestResponse response = bulkRecallRequestService
                .createBulkRecallRequest(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Lấy chi tiết yêu cầu thu hồi hàng loạt.
     *
     * GET /api/v1/recall-requests/bulk/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<BulkRecallRequestResponse>> getBulkRecallRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "READ");

        BulkRecallRequestResponse response = bulkRecallRequestService
                .getBulkRecallRequest(id, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Lấy danh sách yêu cầu thu hồi hàng loạt với phân trang.
     *
     * GET /api/v1/recall-requests/bulk?status=PENDING&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<BulkRecallRequestResponse>>> listBulkRecallRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "READ");

        PageResponse<BulkRecallRequestResponse> response = bulkRecallRequestService
                .listBulkRecallRequests(status, page, size, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Phê duyệt yêu cầu thu hồi hàng loạt.
     *
     * PUT /api/v1/recall-requests/bulk/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResult<BulkRecallRequestResponse>> approveBulkRecallRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveBulkRecallRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "UPDATE");

        BulkRecallRequestResponse response = bulkRecallRequestService
                .approveBulkRecallRequest(id, request, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Từ chối yêu cầu thu hồi hàng loạt.
     *
     * PUT /api/v1/recall-requests/bulk/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResult<BulkRecallRequestResponse>> rejectBulkRecallRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectBulkRecallRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "UPDATE");

        BulkRecallRequestResponse response = bulkRecallRequestService
                .rejectBulkRecallRequest(id, request, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Kết thúc vụ việc thu hồi gắn liền với yêu cầu thu hồi hàng loạt (NCL-08-CN-012).
     *
     * PUT /api/v1/recall-requests/bulk/{id}/close
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResult<BulkRecallRequestResponse>> closeBulkRecallRequest(
            @PathVariable UUID id,
            @Valid @RequestBody CloseRecallCaseRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "UPDATE");

        BulkRecallRequestResponse response = bulkRecallRequestService
                .closeBulkRecallRequest(id, request, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Tải lên tệp biên bản đính kèm vụ việc thu hồi (NCL-08-CN-012).
     * Hỗ trợ định dạng PDF (.pdf) hoặc Word (.docx, .doc).
     *
     * POST /api/v1/recall-requests/bulk/evidence
     */
    @PostMapping(value = "/evidence", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<vn.nguongocso.recall.dto.response.RecallEvidenceResponse>> uploadEvidence(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "UPDATE");

        vn.nguongocso.recall.dto.response.RecallEvidenceResponse response = bulkRecallRequestService
                .uploadEvidenceFile(file, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Tải xuống hoặc xem tệp biên bản thu hồi đã tải lên.
     *
     * GET /api/v1/recall-requests/bulk/evidence/{fileId}
     */
    @GetMapping("/evidence/{fileId}")
    public ResponseEntity<org.springframework.core.io.Resource> getEvidenceFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        permissionChecker.check("recall", "READ");

        BulkRecallRequestService.EvidenceFileContent fileContent = bulkRecallRequestService
                .getEvidenceFile(fileId, currentUser);

        org.springframework.http.MediaType mediaType;
        try {
            mediaType = org.springframework.http.MediaType.parseMediaType(fileContent.contentType());
        } catch (Exception e) {
            mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileContent.fileName() + "\"")
                .body(fileContent.resource());
    }
}
