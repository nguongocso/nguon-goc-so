package vn.nguongocso.report.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service định nghĩa chức năng kết xuất dữ liệu mở.
 */
public interface OpenDataExportService {
    /**
     * Kết xuất dữ liệu mở theo lược đồ chuẩn và định dạng đã chọn, giới hạn
     * trong phạm vi địa bàn được phép của người thực hiện (NCL-743).
     *
     * @param region      địa bàn lọc (tuỳ chọn — bỏ trống khi lọc theo unitIds)
     * @param fromDate    ngày bắt đầu thu hoạch
     * @param toDate      ngày kết thúc thu hoạch
     * @param format      định dạng (JSON/XML/CSV)
     * @param currentUser người thực hiện
     * @param ipAddress   địa chỉ IP client
     * @return mảng byte chứa nội dung tệp tin
     */
    byte[] exportOpenData(String region, List<UUID> unitIds, LocalDate fromDate, LocalDate toDate, String format,
            CustomUserDetails currentUser, String ipAddress);
}
