package vn.nguongocso.alert.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.alert.dto.request.ActivityLogRequest;
import vn.nguongocso.alert.dto.response.ActivityLogResponse;

import java.time.LocalDate;

/**
 * Service để quản lý nhật ký hoạt động của người dùng.
 */
public interface ActivityLogService {
    /**
     * Lấy danh sách nhật ký hoạt động theo bộ lọc.
     */
    PageResponse<ActivityLogResponse> getActivityLogs(
            int page,
            int size,
            String action,
            String actorName,
            LocalDate startDate,
            LocalDate endDate,
            CustomUserDetails currentUser
    );

    /**
     * Ghi nhật ký hoạt động.
     */
    void logActivity(ActivityLogRequest request);

}
