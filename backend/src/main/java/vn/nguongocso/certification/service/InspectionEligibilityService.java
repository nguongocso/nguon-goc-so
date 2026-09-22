package vn.nguongocso.certification.service;

import vn.nguongocso.certification.dto.response.InspectionEligibilityResult;
import vn.nguongocso.farm.entity.ProductionLot;

/**
 * Service đánh giá điều kiện kiểm nghiệm của lô sản xuất theo QTN-30 (NCL-11-CN-005).
 */
public interface InspectionEligibilityService {
        /**
         * Đánh giá lô có đủ điều kiện tạo lô hàng hoặc kích hoạt tem hay không.
         */
        InspectionEligibilityResult evaluateForShipment(
                        ProductionLot lot);

        /**
         * Kiểm tra kết luận kiểm nghiệm hoàn thành mới nhất của lô có phải là Không đạt hay không.
         */
        boolean hasLatestFailedConclusion(
                        ProductionLot lot);
}
