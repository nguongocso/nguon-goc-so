package vn.nguongocso.recall.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
import vn.nguongocso.recall.dto.request.ApproveRecallRequest;
import vn.nguongocso.recall.dto.request.CreateRecallRequest;
import vn.nguongocso.recall.dto.request.RejectRecallRequest;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;
import vn.nguongocso.recall.service.RecallRequestService;

/**
 * Controller quản lý yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 *
 * <p>
 * Quy trình 2 bước:
 * <ol>
 *   <li>Người ghi sự kiện (VT-03) tạo yêu cầu.</li>
 *   <li>Quản lý hợp tác xã (VT-02) duyệt hoặc từ chối.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/recall-requests")
@RequiredArgsConstructor
@Validated
public class RecallRequestController {

    private final RecallRequestService recallRequestService;

    /**
     * Tạo yêu cầu thu hồi lô sản xuất.
     *
     * <p>Chỉ người ghi sự kiện (VT-03) được phép.</p>
     *
     * POST /api/v1/recall-requests
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-03')")
    public ResponseEntity<ApiResult<RecallRequestResponse>> create(
            @Valid @RequestBody CreateRecallRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        RecallRequestResponse response = recallRequestService.create(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Lấy danh sách yêu cầu thu hồi theo trạng thái, phân trang.
     *
     * <p>Chỉ quản lý hợp tác xã (VT-02) được phép.</p>
     *
     * GET /api/v1/recall-requests?status=PENDING&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<PageResponse<RecallRequestResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PageResponse<RecallRequestResponse> response =
                recallRequestService.list(status, page, size, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Lấy chi tiết một yêu cầu thu hồi.
     *
     * <p>Chỉ quản lý hợp tác xã (VT-02) được phép.</p>
     *
     * GET /api/v1/recall-requests/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<RecallRequestResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        RecallRequestResponse response = recallRequestService.getById(id, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Duyệt một yêu cầu thu hồi.
     *
     * <p>
     * Chỉ quản lý hợp tác xã (VT-02) được phép. Người duyệt phải khác người tạo
     * (QTN-22).
     * </p>
     *
     * PUT /api/v1/recall-requests/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<RecallRequestResponse>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid ApproveRecallRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        RecallRequestResponse response = recallRequestService.approve(id, request, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Từ chối một yêu cầu thu hồi.
     *
     * <p>Chỉ quản lý hợp tác xã (VT-02) được phép.</p>
     *
     * PUT /api/v1/recall-requests/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<RecallRequestResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRecallRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        RecallRequestResponse response = recallRequestService.reject(id, request, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }
}