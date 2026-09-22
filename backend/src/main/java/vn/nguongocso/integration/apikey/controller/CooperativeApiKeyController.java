package vn.nguongocso.integration.apikey.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.integration.apikey.dto.request.CreateTestApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;

/**
 * Controller cấp khóa thử nghiệm dành cho Hợp tác xã.
*/
@RestController
@RequestMapping("/api/v1/cooperative")
@RequiredArgsConstructor
public class CooperativeApiKeyController {
    private static final Logger log = LoggerFactory.getLogger(CooperativeApiKeyController.class);

    private final PartnerApiKeyService partnerApiKeyService;

    /**
     * Cấp mới khóa thử nghiệm cho đối tác bên thứ ba.
     */
    @PostMapping("/test-api-keys")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<PartnerApiKeyResponse>> createTestApiKey(
            @Valid @RequestBody CreateTestApiKeyRequest request) {
        log.info("Nhận yêu cầu cấp khóa thử nghiệm qua /api/v1/cooperative/test-api-keys cho đối tác '{}', limit={}/h",
                request.getPartnerName(), request.getRateLimitPerHour());

        PartnerApiKeyResponse response = partnerApiKeyService.createTestApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(201, response));
    }
}
