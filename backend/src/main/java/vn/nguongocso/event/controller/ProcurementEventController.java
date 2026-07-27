package vn.nguongocso.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.event.dto.request.RecordProcurementEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.service.ProcurementEventService;

@RestController
@RequestMapping("/api/v1/chain-events")
@RequiredArgsConstructor
public class ProcurementEventController {

    private final ProcurementEventService procurementEventService;

    /**
     * API ghi nhận sự kiện thu mua cho lô hàng.
     * Chỉ dành cho Doanh nghiệp thu mua (VT-04).
     */
    @PostMapping("/procurement")
    @PreAuthorize("hasRole('VT-04')")
    public ResponseEntity<ApiResult<ChainEventResponse>> recordProcurement(
            @Valid @RequestBody RecordProcurementEventRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ChainEventResponse response = procurementEventService.recordProcurementEvent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(response));
    }
}
