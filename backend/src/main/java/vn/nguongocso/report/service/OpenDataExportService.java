package vn.nguongocso.report.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import java.time.LocalDate;

/**
 * Service định nghĩa chức năng kết xuất dữ liệu mở.
 */
public interface OpenDataExportService {
    /**
     * Kết xuất dữ liệu mở theo lược đồ chuẩn và định dạng đã chọn.
     *
     * @param region      địa bàn lọc
     * @param fromDate    ngày bắt đầu thu hoạch
     * @param toDate      ngày kết thúc thu hoạch
     * @param format      định dạng (JSON/XML/CSV)
     * @param currentUser người thực hiện
     * @param ipAddress   địa chỉ IP client
     * @return mảng byte chứa nội dung tệp tin
     */
    byte[] exportOpenData(String region, LocalDate fromDate, LocalDate toDate, String format,
            CustomUserDetails currentUser, String ipAddress);
}
