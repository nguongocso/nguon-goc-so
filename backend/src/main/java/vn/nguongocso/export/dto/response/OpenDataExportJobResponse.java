package vn.nguongocso.export.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.export.enums.ExportJobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi trạng thái tác vụ xuất dữ liệu mở bất đồng bộ.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenDataExportJobResponse {

    private UUID jobId;

    private ExportJobStatus status;

    private String format;

    private String fileName;

    private Long fileSize;

    private String downloadUrl;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private String errorMessage;
}
