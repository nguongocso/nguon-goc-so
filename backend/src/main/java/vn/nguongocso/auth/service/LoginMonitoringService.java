package vn.nguongocso.auth.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.dto.response.AccountLockResponse;
import vn.nguongocso.auth.dto.response.LoginAnomalyResponse;
import vn.nguongocso.auth.dto.response.LoginHistoryResponse;
import vn.nguongocso.auth.dto.response.SuspiciousCaseResponse;
import vn.nguongocso.common.PageResponse;

/**
 * Service coordinator cho giám sát đăng nhập bất thường.
 */
public interface LoginMonitoringService {
    /**
     * Lấy lịch sử đăng nhập với bộ lọc.
     */
    PageResponse<LoginHistoryResponse> getLoginHistory(
            UUID userId,
            String result,
            UUID organizationId,
            String startDate,
            String endDate,
            Pageable pageable);

    /**
     * Lấy danh sách bất thường với bộ lọc.
     */
    default PageResponse<LoginAnomalyResponse> getLoginAnomalies(
            String status,
            String reasonCode,
            UUID organizationId,
            Pageable pageable) {
        return getLoginAnomalies(status, reasonCode, organizationId, null, pageable);
    }

    /**
     * Lấy danh sách bất thường với bộ lọc.
     */
    PageResponse<LoginAnomalyResponse> getLoginAnomalies(
            String status,
            String reasonCode,
            UUID organizationId,
            String username,
            Pageable pageable);

    /**
     * Lấy danh sách các case bất thường cần xử lý với bộ lọc (NCL-01-CN-017).
     */
    PageResponse<SuspiciousCaseResponse> getSuspiciousCases(
            String status,
            UUID organizationId,
            String username,
            Pageable pageable);

    /**
     * Khoá tạm một tài khoản.
     */
    AccountLockResponse lockAccount(
            UUID accountId,
            UUID anomalyId,
            String reason);

    /**
     * Khoá tạm một tài khoản với thời gian cụ thể.
     */
    AccountLockResponse lockAccount(
            UUID accountId,
            UUID anomalyId,
            String reason,
            Integer days,
            Integer hours,
            Integer minutes,
            boolean permanent);

    /**
     * Mở khoá một tài khoản.
     */
    AccountLockResponse unlockAccount(UUID accountId);

    /**
     * Đánh dấu tất cả bản ghi bất thường của một tài khoản là đã giải quyết.
     */
    void markUserAnomaliesResolved(UUID accountId);
}
