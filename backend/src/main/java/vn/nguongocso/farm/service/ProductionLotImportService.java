package vn.nguongocso.farm.service;

import java.util.UUID;

import org.springframework.core.io.Resource;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.ProductionLotImportRequest;
import vn.nguongocso.farm.dto.response.ProductionLotImportResultResponse;

public interface ProductionLotImportService {

    /**
     * Nhập dữ liệu lô sản xuất từ tệp.
     *
     * @param request     thông tin tệp và tổ chức cần nhập
     * @param userDetails người dùng đang đăng nhập
     * @param ipAddress   địa chỉ IP client
     * @return kết quả nhập dữ liệu
     */
    ProductionLotImportResultResponse importProductionLots(
            ProductionLotImportRequest request,
            CustomUserDetails userDetails,
            String ipAddress);

    /**
     * Tạo file Excel mẫu nhập lô sản xuất.
     *
     * @param productCategoryId mã loại nông sản được chọn
     * @param farmAreaId        mã vùng trồng được chọn
     * @param userDetails       người dùng đang đăng nhập
     * @return file Excel mẫu
     */
    Resource generateImportExcelTemplate(
            UUID productCategoryId,
            UUID farmAreaId,
            CustomUserDetails userDetails);
}