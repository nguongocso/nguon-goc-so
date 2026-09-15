package vn.nguongocso.alert.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/** Phản hồi trạng thái yêu cầu xuất nhật ký hoạt động trong nền. */
@Getter
@Builder
public class ActivityLogExportJobResponse {
    private UUID exportId;
    private String mode;
    private String status;
    private long recordCount;
    private String fileName;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String downloadUrl;
}
