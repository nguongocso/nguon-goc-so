package vn.nguongocso.farm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.request.PublicProductFeedbackLookupRequest;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackLookupResponse;
import vn.nguongocso.farm.service.ProductFeedbackService;

@RestController
@RequestMapping("/api/v1/public/product-feedbacks")
@RequiredArgsConstructor
public class PublicProductFeedbackLookupController {

    private final ProductFeedbackService productFeedbackService;

    @PostMapping("/lookup")
    public ResponseEntity<ApiResult<PublicProductFeedbackLookupResponse>> lookup(
            @Valid @RequestBody PublicProductFeedbackLookupRequest request) {
        PublicProductFeedbackLookupResponse response = productFeedbackService
                .lookupPublicFeedback(request.getLookupCode());
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
