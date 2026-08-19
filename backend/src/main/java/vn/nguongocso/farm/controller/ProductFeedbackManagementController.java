package vn.nguongocso.farm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.service.ProductFeedbackService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.UUID;

/**
 * Controller quản lý phản ánh sản phẩm dành cho nội bộ (VT-01, VT-02).
 */
@RestController
@RequestMapping("/api/v1/product-feedbacks")
@RequiredArgsConstructor
public class ProductFeedbackManagementController {

    private final ProductFeedbackService productFeedbackService;
    private final PermissionChecker permissionChecker;

    /**
     * Lấy danh sách phản ánh sản phẩm (phân trang).
     * VT-01: xem toàn bộ phản ánh của tất cả tổ chức.
     * VT-02: chỉ xem phản ánh thuộc tổ chức của mình.
     */
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<ProductFeedbackResponse>>> getFeedbacks(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        permissionChecker.check("product_feedback", "READ");
        PageResponse<ProductFeedbackResponse> response = productFeedbackService.getFeedbacks(pageable);
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
}