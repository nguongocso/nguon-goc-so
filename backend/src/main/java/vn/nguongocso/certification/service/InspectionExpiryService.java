package vn.nguongocso.certification.service;

import java.time.LocalDate;
import vn.nguongocso.certification.dto.response.InspectionScanResult;

/**
 * Service quét và cảnh báo các kết quả kiểm nghiệm sắp hết hạn hoặc đã hết hạn của lô sản xuất.
 * (NCL-11-CN-004)
 */
public interface InspectionExpiryService {

    /**
     * Quét và gửi cảnh báo cho ngày hiện tại (LocalDate.now()).
     *
     * @return kết quả tổng hợp sau khi quét
     */
    InspectionScanResult scanAndAlertExpiringInspections();

    /**
     * Quét và gửi cảnh báo cho một ngày cụ thể (hỗ trợ kiểm thử và quét bù).
     *
     * @param today ngày tính toán mốc hiệu lực
     * @return kết quả tổng hợp sau khi quét
     */
    InspectionScanResult scanAndAlertExpiringInspections(LocalDate today);

    /**
     * Quét và gửi cảnh báo hết hạn/sắp hết hạn cho một lô sản xuất cụ thể theo ngày chỉ định.
     * Thường được gọi ngay tại thời điểm ghi nhận kết quả kiểm nghiệm hoặc cập nhật kết quả.
     *
     * @param lot lô sản xuất cần kiểm tra
     * @param today ngày tính toán mốc hiệu lực
     * @return true nếu đã tạo cảnh báo và gửi thông báo, false nếu bỏ qua hoặc không thỏa mãn điều kiện
     */
    boolean checkAndAlertLotExpiry(vn.nguongocso.farm.entity.ProductionLot lot, LocalDate today);
}

