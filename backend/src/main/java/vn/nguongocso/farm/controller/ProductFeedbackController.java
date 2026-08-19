package vn.nguongocso.farm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.service.ProductFeedbackService;

import java.util.UUID;

/**
 * Controller quản lý phản ánh sản phẩm.
 * API công khai cho phép người dùng gửi phản ánh sản phẩm mà không yêu cầu đăng
 * nhập.
 */
@RestController
@RequestMapping("/api/v1/public/production-lots")
@RequiredArgsConstructor
public class ProductFeedbackController {

    private final ProductFeedbackService productFeedbackService;

    /**
     * API công khai cho phép người dùng gửi phản ánh sản phẩm.
     * Không yêu cầu đăng nhập (Public access).
     */
    @PostMapping("/{productionLotId}/feedbacks")
    public ResponseEntity<ApiResult<ProductFeedbackResponse>> sendFeedback(
            @PathVariable UUID productionLotId,
            @Valid @RequestBody CreateProductFeedbackRequest request) {

        ProductFeedbackResponse response = productFeedbackService.createFeedback(productionLotId, request);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
