package vn.nguongocso.farm.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.dto.response.PackagingCheckResult;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;

import java.util.List;
import java.util.UUID;

public interface ProductionLotService {
    CreateProductionLotResponse createProductionLot(CreateProductionLotRequest request, CustomUserDetails userDetails);

    List<CreateProductionLotResponse> getAllProductionLots(CustomUserDetails userDetails);

    UpdateProductionLotResponse updateProductionLot(UUID id, UpdateProductionLotRequest request, CustomUserDetails userDetails);

    CreateProductionLotResponse approveProductionLot(UUID lotId, ApproveProductionLotRequest request, CustomUserDetails userDetails);

    CreateProductionLotResponse submitForApproval(UUID lotId, CustomUserDetails userDetails);

    /**
     * Kiểm tra điều kiện đóng gói của lô sản xuất.
     * Chỉ có hiệu lực khi lô ở trạng thái {@link vn.nguongocso.farm.enums.ProductionLotStatus#HARVESTED}.
     *
     * @param lotId mã lô sản xuất
     * @return kết quả kiểm tra
     */
    PackagingCheckResult checkPackagingReadiness(UUID lotId);

    /**
     * Thực hiện đóng gói lô sản xuất.
     * Tự động kiểm tra điều kiện đóng gói trước khi chuyển trạng thái.
     *
     * @param lotId       mã lô sản xuất
     * @param userDetails thông tin người dùng hiện tại
     * @return thông tin lô sau khi đóng gói
     */
    CreateProductionLotResponse packageLot(UUID lotId, CustomUserDetails userDetails);
}
