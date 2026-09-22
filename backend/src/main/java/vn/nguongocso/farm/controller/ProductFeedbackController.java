package vn.nguongocso.farm.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackCreatedResponse;
import vn.nguongocso.farm.service.ProductFeedbackService;

/**
 * Quản lý phản ánh sản phẩm.
*/
@RestController
@RequestMapping("/api/v1/public/production-lots")
@RequiredArgsConstructor
public class ProductFeedbackController {
    private final ProductFeedbackService productFeedbackService;

    /**
     * API công khai cho phép người dùng gửi phản ánh sản phẩm.
     */
    @PostMapping("/{productionLotId}/feedbacks")
    public ResponseEntity<ApiResult<PublicProductFeedbackCreatedResponse>> sendFeedback(
            @PathVariable UUID productionLotId,
            @Valid @RequestBody CreateProductFeedbackRequest request) {

        PublicProductFeedbackCreatedResponse response = productFeedbackService.createFeedback(productionLotId, request);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
