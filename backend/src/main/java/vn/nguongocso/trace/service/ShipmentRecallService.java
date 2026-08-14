package vn.nguongocso.trace.service;

import java.util.UUID;

import vn.nguongocso.trace.dto.request.RecallRequest;
import vn.nguongocso.trace.dto.response.RecallInfoResponse;
import vn.nguongocso.trace.dto.response.RecallResponse;

/**
 * Dịch vụ quản lý thu hồi lô hàng.
 */
public interface ShipmentRecallService {
    /**
     * Thực hiện thu hồi một lô hàng.
     */
RecallResponse recallShipment(
        UUID shipmentId,
        RecallRequest request,
        String ipAddress);

    /**
     * Lấy thông tin tình trạng thu hồi của một lô hàng.
     */
    RecallInfoResponse getRecallInfo(UUID shipmentId);
}