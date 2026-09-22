package vn.nguongocso.farm.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.request.AssignProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CloseProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRecallRequest;
import vn.nguongocso.farm.dto.request.UpdateProductFeedbackProcessingRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.service.ProductFeedbackService;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;

/**
 * Quản lý phản ánh sản phẩm dành cho nội bộ.
*/
@RestController
@RequestMapping("/api/v1/product-feedbacks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
public class ProductFeedbackManagementController {
    private final ProductFeedbackService productFeedbackService;

    private final PermissionChecker permissionChecker;

    /**
     * Lấy danh sách phản ánh sản phẩm.
     */
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<ProductFeedbackResponse>>> getFeedbacks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductFeedbackStatus status,
            @RequestParam(required = false) ProductFeedbackSeverity severity,
            @RequestParam(required = false) UUID productionLotId,
            @RequestParam(required = false) UUID assignedToUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        permissionChecker.check("product_feedback", "READ");
        PageResponse<ProductFeedbackResponse> response = productFeedbackService.getFeedbacks(
                keyword, status, severity, productionLotId, assignedToUserId, pageable);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy chi tiết một phản ánh sản phẩm.
     */
    @GetMapping("/{feedbackId}")
    public ResponseEntity<ApiResult<ProductFeedbackResponse>> getFeedbackById(
            @PathVariable UUID feedbackId) {

        permissionChecker.check("product_feedback", "READ");
        ProductFeedbackResponse response = productFeedbackService.getFeedbackById(feedbackId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Gán phản ánh sản phẩm cho nhân viên xử lý.
     */
    @PutMapping("/{feedbackId}/assignment")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ProductFeedbackResponse>> assign(
            @PathVariable UUID feedbackId,
            @Valid @RequestBody AssignProductFeedbackRequest request) {
        permissionChecker.check("product_feedback", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(productFeedbackService.assign(feedbackId, request)));
    }

    /**
     * Cập nhật tiến độ xử lý phản ánh sản phẩm.
     */
    @PutMapping("/{feedbackId}/processing")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ProductFeedbackResponse>> updateProcessing(
            @PathVariable UUID feedbackId,
            @Valid @RequestBody UpdateProductFeedbackProcessingRequest request) {
        permissionChecker.check("product_feedback", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(productFeedbackService.updateProcessing(feedbackId, request)));
    }

    /**
     * Đóng phản ánh sản phẩm sau khi hoàn tất xử lý.
     */
    @PutMapping("/{feedbackId}/close")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ProductFeedbackResponse>> close(
            @PathVariable UUID feedbackId,
            @Valid @RequestBody CloseProductFeedbackRequest request) {
        permissionChecker.check("product_feedback", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(productFeedbackService.close(feedbackId, request)));
    }

    /**
     * Tạo yêu cầu thu hồi sản phẩm từ phản ánh sản phẩm.
     */
    @PostMapping("/{feedbackId}/recall-requests")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<RecallRequestResponse>> createRecallRequest(
            @PathVariable UUID feedbackId,
            @Valid @RequestBody CreateProductFeedbackRecallRequest request) {
        permissionChecker.check("product_feedback", "UPDATE");
        permissionChecker.check("recall", "CREATE");
        return ResponseEntity.status(201)
                .body(ApiResult.success(201, productFeedbackService.createRecallRequest(feedbackId, request)));
    }
}
