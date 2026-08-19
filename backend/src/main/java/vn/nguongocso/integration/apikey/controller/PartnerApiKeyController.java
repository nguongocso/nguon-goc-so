package vn.nguongocso.integration.apikey.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.integration.apikey.dto.request.CreateApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;

/**
 * Controller quản lý khóa truy cập dành cho Quản lý Hợp tác xã (VT-02).
 * <p>
 * Phân quyền nghiêm ngặt: Chỉ tài khoản có vai trò VT-02 mới được phép thực hiện (TC-04).
 */
@RestController
@RequestMapping("/api/v1/organization/api-keys")
@RequiredArgsConstructor
public class PartnerApiKeyController {

    private static final Logger log = LoggerFactory.getLogger(PartnerApiKeyController.class);

    private final PartnerApiKeyService partnerApiKeyService;

    /**
     * Cấp mới khóa truy cập cho bên thứ ba (TC-01, TC-03).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<PartnerApiKeyResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request) {

        log.info("Nhận yêu cầu cấp khóa truy cập đối tác '{}', limit={}/h",
                request.getPartnerName(), request.getRateLimitPerHour());

        PartnerApiKeyResponse response = partnerApiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(201, response));
    }

    /**
     * Lấy danh sách khóa truy cập thuộc Hợp tác xã hiện tại.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<Page<PartnerApiKeyResponse>>> getOrganizationApiKeys(
            @RequestParam(required = false) PartnerApiKeyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PartnerApiKeyResponse> responses = partnerApiKeyService.getOrganizationApiKeys(status, pageable);
        return ResponseEntity.ok(ApiResult.success(responses));
    }

    /**
     * Thu hồi khóa truy cập (TC-02).
     */
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<PartnerApiKeyResponse>> revokeApiKey(
            @PathVariable UUID id) {

        log.info("Nhận yêu cầu thu hồi khóa truy cập id={}", id);
        PartnerApiKeyResponse response = partnerApiKeyService.revokeApiKey(id);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
