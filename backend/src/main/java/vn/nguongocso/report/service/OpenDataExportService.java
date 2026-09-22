package vn.nguongocso.report.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;

/** Service kết xuất dữ liệu mở. */
public interface OpenDataExportService {

    /**
     * Kết xuất dữ liệu mở theo lược đồ chuẩn và định dạng đã chọn.
     */
    byte[] exportOpenData(
        String region,
        List<UUID> unitIds,
        LocalDate fromDate,
        LocalDate toDate,
        String format,
        CustomUserDetails currentUser,
        String ipAddress
    );
}
