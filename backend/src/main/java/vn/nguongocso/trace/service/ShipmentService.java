package vn.nguongocso.trace.service;

import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.response.ShipmentResponse;

import java.util.UUID;


/**
 * Định nghĩa các nghiệp vụ quản lý lô hàng và sinh mã truy xuất.
 */
public interface ShipmentService {

    /**
     * Tạo lô hàng từ lô sản xuất và sinh mã truy xuất tương ứng.
     *
     * @param request thông tin tạo lô hàng
     * @return thông tin lô hàng sau khi tạo
     */
	ShipmentResponse createShipment(CreateShipmentRequest request);
    ShipmentResponse activateShipmentStamps(UUID shipmentId);
    java.util.List<ShipmentResponse> getShipmentsByOrganization(UUID organizationId);
	
}
