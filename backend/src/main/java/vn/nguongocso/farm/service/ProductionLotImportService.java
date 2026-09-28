package vn.nguongocso.farm.service;

import java.util.UUID;

import org.springframework.core.io.Resource;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.ProductionLotImportRequest;
import vn.nguongocso.farm.dto.response.ProductionLotImportResultResponse;

/**
 * Nghiệp vụ nhập lô sản xuất từ tệp.
*/
public interface ProductionLotImportService {
    /** Nhập lô sản xuất từ tệp. */
    ProductionLotImportResultResponse importProductionLots(
            ProductionLotImportRequest request,
            CustomUserDetails userDetails,
            String ipAddress);

    /** Tạo tệp Excel mẫu nhập lô sản xuất. */
    Resource generateImportExcelTemplate(
            UUID productCategoryId,
            UUID farmAreaId,
            CustomUserDetails userDetails);
}