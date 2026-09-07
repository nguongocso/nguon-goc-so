package vn.nguongocso.trace.controller;

import java.util.List;
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
import vn.nguongocso.trace.dto.request.ApproveSupplementRequest;
import vn.nguongocso.trace.dto.request.CreateSupplementRequest;
import vn.nguongocso.trace.dto.request.RejectSupplementRequest;
import vn.nguongocso.trace.dto.response.CodeRangeSupplementResponse;
import vn.nguongocso.trace.dto.response.EvidenceEventResponse;
import vn.nguongocso.trace.service.CodeRangeSupplementService;

/**
 * Controller quản lý yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 *
 * <p>
 * Quy trình:
 * <ol>
 *   <li>Quản lý hợp tác xã (VT-02) tạo yêu cầu khi hạn mức còn lại
 *       dưới ngưỡng cảnh báo hoặc đã hết.</li>
 *   <li>Quản trị viên nền tảng (VT-01) duyệt toàn bộ / duyệt một phần
 *       hoặc từ chối kèm lý do.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/code-range-supplement-requests")
@RequiredArgsConstructor
@Validated
public class CodeRangeSupplementController {

    private final CodeRangeSupplementService supplementService;

    /**
     * Tạo yêu cầu cấp bổ sung dải mã.
     *
     * <p>Chỉ Quản lý hợp tác xã (VT-02) được phép.</p>
     *
     * POST /api/v1/code-range-supplement-requests
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CodeRangeSupplementResponse>> create(
            @Valid @RequestBody CreateSupplementRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CodeRangeSupplementResponse response = supplementService.create(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Lấy danh sách sự kiện bằng chứng sản lượng thực (thu hoạch / sơ chế)
     * của tổ chức để chọn khi tạo yêu cầu cấp bổ sung.
     *
     * <p>Chỉ Quản lý hợp tác xã (VT-02) được phép. Danh sách phẳng, sắp xếp
     * mới nhất trước, tối đa 200 sự kiện.</p>
     *
     * GET /api/v1/code-range-supplement-requests/evidence-events
     */
    @GetMapping("/evidence-events")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<List<EvidenceEventResponse>>> listEvidenceEvents(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<EvidenceEventResponse> response = supplementService.listEvidenceEvents(currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Lấy danh sách yêu cầu của tổ chức mình.
     *
     * <p>Chỉ Quản lý hợp tác xã (VT-02) được phép.</p>
     *
     * GET /api/v1/code-range-supplement-requests/my?status=PENDING&page=0&size=20
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<PageResponse<CodeRangeSupplementResponse>>> listMine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PageResponse<CodeRangeSupplementResponse> response =
                supplementService.listMine(status, page, size, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Lấy danh sách tất cả yêu cầu theo trạng thái, phân trang.
     *
     * <p>Chỉ Quản trị viên nền tảng (VT-01) được phép.</p>
     *
     * GET /api/v1/code-range-supplement-requests?status=PENDING&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<PageResponse<CodeRangeSupplementResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PageResponse<CodeRangeSupplementResponse> response =
                supplementService.list(status, page, size, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Lấy chi tiết một yêu cầu.
     *
     * <p>VT-01 xem tất cả; VT-02 chỉ xem yêu cầu của tổ chức mình.</p>
     *
     * GET /api/v1/code-range-supplement-requests/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<CodeRangeSupplementResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CodeRangeSupplementResponse response = supplementService.getById(id, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Duyệt toàn bộ hoặc một phần một yêu cầu.
     *
     * <p>Chỉ Quản trị viên nền tảng (VT-01) được phép. Khi duyệt, hạn mức
     * ({@code totalLimit}) của dải mã hiện có của tổ chức tăng ngay
     * theo số lượng thực cấp.</p>
     *
     * PUT /api/v1/code-range-supplement-requests/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<CodeRangeSupplementResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveSupplementRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CodeRangeSupplementResponse response = supplementService.approve(id, request, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Từ chối một yêu cầu kèm lý do bắt buộc.
     *
     * <p>Chỉ Quản trị viên nền tảng (VT-01) được phép.</p>
     *
     * PUT /api/v1/code-range-supplement-requests/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<CodeRangeSupplementResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectSupplementRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CodeRangeSupplementResponse response = supplementService.reject(id, request, currentUser);

        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }
}
