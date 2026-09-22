package vn.nguongocso.trace.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.request.SplitShipmentRequest;
import vn.nguongocso.trace.dto.response.PartnerOrganizationResponse;
import vn.nguongocso.trace.dto.response.ProcurementShipmentResponse;
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.dto.response.ShipmentSummaryResponse;
import vn.nguongocso.trace.dto.response.SplitPreviewResponse;
import vn.nguongocso.trace.dto.response.SplitShipmentResponse;

/** Service quản lý lô hàng và sinh mã truy xuất. */
public interface ShipmentService {

    /**
     * Tạo lô hàng từ lô sản xuất và sinh mã truy xuất tương ứng.
     */
    ShipmentResponse createShipment(
        CreateShipmentRequest request
    );

    /**
     * Kích hoạt các mã truy xuất của lô hàng.
     */
    ShipmentResponse activateShipmentStamps(
        UUID shipmentId
    );

    /**
     * Lấy danh sách lô hàng theo ID của lô sản xuất.
     */
    List<ShipmentResponse> getShipmentsByProductionLot(
        UUID productionLotId
    );

    /**
     * Lấy danh sách lô hàng theo ID lô sản xuất có phân trang.
     */
    PageResponse<ShipmentResponse> getShipmentsByProductionLotPaged(
        UUID productionLotId,
        int page,
        int size
    );

    /**
     * Tra cứu lô hàng bằng mã truy xuất in trên tem QR.
     */
    ShipmentSummaryResponse getShipmentByCode(
        String code
    );

    /**
     * Lấy danh sách lô hàng đủ điều kiện thu mua.
     */
    List<ProcurementShipmentResponse> getEligibleShipments();

    /**
     * Lấy chi tiết lô hàng theo ID.
     */
    ShipmentResponse getShipmentById(
        UUID id
    );

    /**
     * Lấy danh sách tổ chức đối tác có tìm kiếm và phân trang.
     */
    PageResponse<PartnerOrganizationResponse> getPartnerOrganizations(
        String keyword,
        int page,
        int size
    );

    /**
     * Lấy thông tin xem trước khi tách lô hàng.
     */
    SplitPreviewResponse getSplitPreview(
        UUID shipmentId
    );

    /**
     * Thực hiện tách lô hàng thành các lô con.
     */
    SplitShipmentResponse splitShipment(
        UUID shipmentId,
        SplitShipmentRequest request
    );
}
