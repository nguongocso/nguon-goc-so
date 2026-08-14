package vn.nguongocso.farm.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;
import vn.nguongocso.report.dto.response.ProductionLotDashboardResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Quản lý lô sản xuất và thống kê liên quan. */
public interface ProductionLotService {
    /** Tạo lô sản xuất mới. */
    CreateProductionLotResponse createProductionLot(CreateProductionLotRequest request, CustomUserDetails userDetails);

    /** Lấy danh sách lô sản xuất của tổ chức hiện tại. */
    List<CreateProductionLotResponse> getAllProductionLots(CustomUserDetails userDetails);

    /** Cập nhật lô sản xuất. */
    UpdateProductionLotResponse updateProductionLot(UUID id, UpdateProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Phê duyệt hoặc từ chối lô sản xuất. */
    CreateProductionLotResponse approveProductionLot(UUID lotId, ApproveProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Gửi lô sản xuất sang trạng thái chờ duyệt. */
    CreateProductionLotResponse submitForApproval(UUID lotId, CustomUserDetails userDetails);

    /** Lấy dashboard thống kê lô sản xuất. */
    ProductionLotDashboardResponse getDashboard(
            LocalDate startDate,
            LocalDate endDate,
            UUID targetOrganizationId,
            String groupBy,
            CustomUserDetails userDetails,
            String ipAddress);

    /** Lấy chi tiết lô sản xuất theo ID. */
    CreateProductionLotResponse getProductionLotById(UUID id);
}