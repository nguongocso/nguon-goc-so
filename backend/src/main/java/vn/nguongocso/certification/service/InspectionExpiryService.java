package vn.nguongocso.certification.service;

import vn.nguongocso.certification.dto.response.InspectionScanResult;
import vn.nguongocso.farm.entity.ProductionLot;

import java.time.LocalDate;

/**
 * Service quét và cảnh báo các kết quả kiểm nghiệm sắp hết hạn hoặc đã hết hạn của lô sản xuất (NCL-11-CN-004).
 */
public interface InspectionExpiryService {
        /**
         * Quét và gửi cảnh báo cho ngày hiện tại.
         */
        InspectionScanResult scanAndAlertExpiringInspections();

        /**
         * Quét và gửi cảnh báo cho một ngày cụ thể (hỗ trợ kiểm thử và quét bù).
         */
        InspectionScanResult scanAndAlertExpiringInspections(
                        LocalDate today);

        /**
         * Quét và gửi cảnh báo hết hạn hoặc sắp hết hạn cho một lô sản xuất cụ thể theo ngày chỉ định.
         */
        boolean checkAndAlertLotExpiry(
                        ProductionLot lot,
                        LocalDate today);
}
