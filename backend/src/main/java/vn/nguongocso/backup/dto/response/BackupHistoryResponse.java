package vn.nguongocso.backup.dto.response;

import lombok.*;
import vn.nguongocso.backup.entity.BackupRestoreHistory;

import java.time.LocalDateTime;

/**
 * Phản hồi chi tiết lịch sử sao lưu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupHistoryResponse {
    private Integer id;

    private String operationType;

    private String fileName;

    private Long fileSize;

    private String backupType;

    private String status;

    private String errorMessage;

    private Integer referenceId;

    private LocalDateTime createdAt;

    private String createdBy;

    /**
     * Chuyển đổi từ thực thể BackupRestoreHistory sang BackupHistoryResponse.
     *
     * @param history Thực thể BackupRestoreHistory cần chuyển đổi.
     * @return Đối tượng BackupHistoryResponse tương ứng.
     */
    public static BackupHistoryResponse fromEntity(BackupRestoreHistory history) {
        if (history == null)
            return null;
        return BackupHistoryResponse.builder()
                .id(history.getId())
                .operationType(history.getOperationType().name())
                .fileName(history.getFileName())
                .fileSize(history.getFileSize())
                .backupType(history.getBackupType() != null ? history.getBackupType().name() : null)
                .status(history.getStatus().name())
                .errorMessage(history.getErrorMessage())
                .referenceId(history.getReference() != null ? history.getReference().getId() : null)
                .createdAt(history.getCreatedAt())
                .createdBy(history.getCreatedBy() != null ? history.getCreatedBy().getFullName() : "Hệ thống")
                .build();
    }
}
