package vn.nguongocso.farm.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CancelProductionLotRequest;
import vn.nguongocso.farm.dto.request.CloneProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.DisposeProductionLotRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.ChainProgressBoardResponse;
import vn.nguongocso.farm.dto.response.CloneProductionLotPreviewResponse;
import vn.nguongocso.farm.dto.response.CloneProductionLotResponse;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;
import vn.nguongocso.report.dto.response.ProductionLotDashboardResponse;
/**
 * Nghiệp vụ lô sản xuất.
*/
public interface ProductionLotService {
    /** Tạo lô sản xuất. */
    CreateProductionLotResponse createProductionLot(CreateProductionLotRequest request, CustomUserDetails userDetails);

    /** Lấy danh sách lô sản xuất. */
    List<CreateProductionLotResponse> getAllProductionLots(CustomUserDetails userDetails);

    /** Cập nhật lô sản xuất. */
    UpdateProductionLotResponse updateProductionLot(UUID id, UpdateProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Phê duyệt lô sản xuất. */
    CreateProductionLotResponse approveProductionLot(UUID lotId, ApproveProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Hủy lô sản xuất. */
    CreateProductionLotResponse cancelProductionLot(UUID lotId, CancelProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Loại bỏ lô sản xuất. */
    CreateProductionLotResponse disposeProductionLot(UUID lotId, DisposeProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Gửi lô sản xuất chờ duyệt. */
    CreateProductionLotResponse submitForApproval(UUID lotId, CustomUserDetails userDetails);

    /** Lấy dashboard lô sản xuất. */
    ProductionLotDashboardResponse getDashboard(
            LocalDate startDate,
            LocalDate endDate,
            UUID targetOrganizationId,
            String groupBy,
            CustomUserDetails userDetails,
            String ipAddress);

    /** Lấy chi tiết lô sản xuất theo ID. */
    CreateProductionLotResponse getProductionLotById(UUID id);

    /** Xem trước nhân bản lô sản xuất. */
    CloneProductionLotPreviewResponse getClonePreview(UUID sourceLotId, CustomUserDetails userDetails);

    /** Nhân bản lô sản xuất từ lô mẫu. */
    CloneProductionLotResponse cloneProductionLot(UUID sourceLotId, CloneProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Lấy bảng tiến độ chuỗi của lô. */
    ChainProgressBoardResponse getChainProgressBoard(
            UUID targetOrganizationId,
            Integer stagnantThresholdDays,
            String search,
            CustomUserDetails userDetails);
}