package vn.nguongocso.report.service;

import java.util.UUID;

/** Service quản lý nhật ký truy cập báo cáo. */
public interface ReportAccessLogService {

    /**
     * Ghi nhận lịch sử truy cập báo cáo.
     */
    void logAccess(
        UUID userId,
        UUID userOrgId,
        UUID targetOrgId,
        String reportName,
        boolean success,
        String ipAddress
    );
}