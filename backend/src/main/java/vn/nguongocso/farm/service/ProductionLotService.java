package vn.nguongocso.farm.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CancelProductionLotRequest;
import vn.nguongocso.farm.dto.request.CloneProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.DisposeProductionLotRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CloneProductionLotPreviewResponse;
import vn.nguongocso.farm.dto.response.CloneProductionLotResponse;
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

    /** Hủy lô sản xuất và ghi lý do (NCL-02-CN-006). */
    CreateProductionLotResponse cancelProductionLot(UUID lotId, CancelProductionLotRequest request,
            CustomUserDetails userDetails);

    /**
     * Loại bỏ lô sản xuất sau kết luận kiểm nghiệm Không đạt
     * (NCL-11-CN-005, QTN-30).
     */
    CreateProductionLotResponse disposeProductionLot(UUID lotId, DisposeProductionLotRequest request,
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

    /**
     * Lấy dữ liệu xem trước khi tạo lô sản xuất mới từ mẫu vụ trước
     * (NCL-02-CN-007).
     */
    CloneProductionLotPreviewResponse getClonePreview(UUID sourceLotId, CustomUserDetails userDetails);

    /**
     * Tạo lô sản xuất mới từ mẫu vụ trước (NCL-02-CN-007).
     *
     * <p>
     * Lô mới luôn ở trạng thái DRAFT, kế thừa vùng trồng / loại nông sản /
     * chứng nhận còn hiệu lực của lô mẫu và tuyệt đối không sao chép lịch
     * sử vận hành của lô cũ.
     * </p>
     */
    CloneProductionLotResponse cloneProductionLot(UUID sourceLotId, CloneProductionLotRequest request,
            CustomUserDetails userDetails);

    /** Lấy bảng theo dõi tiến độ chuỗi của từng lô (NCL-10-CN-013). */
    vn.nguongocso.farm.dto.response.ChainProgressBoardResponse getChainProgressBoard(
            UUID targetOrganizationId,
            Integer stagnantThresholdDays,
            String search,
            CustomUserDetails userDetails);
}