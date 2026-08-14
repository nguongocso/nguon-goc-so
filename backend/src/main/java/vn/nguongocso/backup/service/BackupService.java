package vn.nguongocso.backup.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.backup.dto.request.BackupScheduleRequest;
import vn.nguongocso.backup.dto.response.BackupHistoryResponse;
import vn.nguongocso.backup.dto.response.BackupScheduleResponse;
import vn.nguongocso.backup.entity.BackupRestoreHistory;
import vn.nguongocso.backup.enums.BackupOperationType;
import vn.nguongocso.backup.enums.BackupStatus;
import vn.nguongocso.backup.enums.BackupType;

import java.io.File;

/** Xử lý sao lưu hệ thống và lịch sử sao lưu. */
public interface BackupService {
    /** Cấu hình lịch sao lưu. */
    BackupScheduleResponse configureSchedule(BackupScheduleRequest request, User updater);

    /** Lấy lịch sao lưu đang hoạt động. */
    BackupScheduleResponse getActiveSchedule();

    /** Sao lưu thủ công theo yêu cầu. */
    BackupHistoryResponse triggerManualBackup(User creator);

    /** Thực thi sao lưu và ghi lịch sử. */
    BackupRestoreHistory executeBackup(BackupType backupType, User creator);

    /** Thực thi sao lưu mà không khóa lịch. */
    BackupRestoreHistory executeBackupWithoutLock(BackupType backupType, User creator);

    /** Cập nhật trạng thái xử lý sao lưu. */
    void updateStatus(Integer id, BackupStatus status, String fileName, String filePath, Long fileSize,
            String errorMessage);

    /** Lấy lịch sử sao lưu theo bộ lọc. */
    Page<BackupHistoryResponse> getHistory(BackupOperationType operationType, BackupStatus status, Pageable pageable);

    /** Lấy tệp sao lưu từ lịch sử. */
    File getBackupFile(Integer historyId);

    /** Xóa bản sao lưu. */
    void deleteBackup(Integer historyId);
}
