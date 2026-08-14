package vn.nguongocso.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý dòng sự kiện truy xuất của lô hàng.
 * Chỉ VT-02 mới được phép xem.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentTimelineController {
    private final ChainEventService chainEventService;
    private final PermissionChecker permissionChecker;

    /**
     * Xem dòng sự kiện truy xuất của một lô hàng.
     * Chỉ VT-02 mới được phép.
     *
     * @param shipmentId UUID của lô hàng
     * @return danh sách sự kiện theo thứ tự thời gian
     */
    @GetMapping("/{shipmentId}/chain-events")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<List<ChainEventResponse>>> getShipmentTimeline(
            @PathVariable UUID shipmentId) {

        permissionChecker.check("SHIPMENT", "READ");
        List<ChainEventResponse> events = chainEventService.getShipmentTimeline(shipmentId);
        return ResponseEntity.ok(ApiResult.success(events));
    }
}