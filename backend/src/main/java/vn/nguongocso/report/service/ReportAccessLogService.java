package vn.nguongocso.report.service;

import java.util.UUID;

/**
 * Service quản lý log truy cập báo cáo.
 *
 * @author Triệu Văn Đại
 */
public interface ReportAccessLogService {
    /**
     * Ghi nhận lịch sử truy cập báo cáo (giao dịch độc lập).
     */
    void logAccess(UUID userId, UUID userOrgId, UUID targetOrgId,
            String reportName, boolean success, String ipAddress);
}