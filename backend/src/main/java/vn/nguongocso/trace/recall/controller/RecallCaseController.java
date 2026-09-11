package vn.nguongocso.trace.recall.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;
import vn.nguongocso.trace.recall.dto.response.RecallCaseResponse;
import vn.nguongocso.trace.recall.service.RecallCaseService;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý vụ việc thu hồi (NCL-08-CN-012).
 *
 * <p>Chỉ Quản lý hợp tác xã (VT-02) được phép. Dữ liệu luôn được giới hạn theo
 * tổ chức hiện tại (QTN-01).</p>
 */
@RestController
@RequestMapping("/api/v1/recall-cases")
@RequiredArgsConstructor
public class RecallCaseController {

    private final RecallCaseService recallCaseService;

    /** Danh sách vụ việc (lazy materialize các lô đã thu hồi chưa có case). */
    @GetMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<List<RecallCaseResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<RecallCaseResponse> response = recallCaseService.list(currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /** Chi tiết vụ việc kèm kết quả xử lý từng lô. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<RecallCaseResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), recallCaseService.getById(id, currentUser)));
    }

    /** Đóng vụ việc thu hồi. */
    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<RecallCaseResponse>> close(
            @PathVariable UUID id,
            @Valid @RequestBody CloseRecallCaseRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        RecallCaseResponse response = recallCaseService.close(id, request, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }
}
