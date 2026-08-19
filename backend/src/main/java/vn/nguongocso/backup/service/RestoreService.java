package vn.nguongocso.backup.service;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.backup.dto.response.BackupHistoryResponse;
import vn.nguongocso.backup.enums.BackupStatus;

/** Khôi phục dữ liệu từ bản sao lưu và quản lý chế độ bảo trì. */
public interface RestoreService {
    /** Khởi chạy quy trình khôi phục. */
    BackupHistoryResponse triggerRestore(Integer backupHistoryId, User creator);

    /** Kiểm tra hệ thống đang ở chế độ bảo trì hay không. */
    boolean isMaintenanceMode();

    /** Bật hoặc tắt chế độ bảo trì. */
    void setMaintenanceMode(boolean mode);

    /** Cập nhật trạng thái khôi phục. */
    void updateStatus(Integer id, BackupStatus status, String errorMessage);
}
