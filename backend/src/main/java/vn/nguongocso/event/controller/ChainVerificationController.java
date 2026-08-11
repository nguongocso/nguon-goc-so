package vn.nguongocso.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.event.dto.response.ChainVerificationResponse;
import vn.nguongocso.event.service.ChainEventService;

import java.util.UUID;

/**
 * Controller kiểm chứng tính toàn vẹn dòng sự kiện.
 * Endpoint theo API docs: GET /api/v1/shipments/{shipmentId}/verify-chain
 */
@RestController
@RequestMapping("/api/v1/shipments")
public class ChainVerificationController {
    private final ChainEventService chainEventService;

    public ChainVerificationController(ChainEventService chainEventService) {
        this.chainEventService = chainEventService;
    }

    @GetMapping("/{shipmentId}/verify-chain")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-04', 'VT-05')")
    public ResponseEntity<ApiResult<ChainVerificationResponse>> verifyChainIntegrity(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ChainVerificationResponse response = chainEventService.verifyChainIntegrity(shipmentId, currentUser);
        return ResponseEntity.ok(ApiResult.success(200, response));
    }
}