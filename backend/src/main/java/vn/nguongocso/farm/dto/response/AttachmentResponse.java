package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi thông tin tệp đính kèm.
 */
@Data
@Builder
public class AttachmentResponse {
    private UUID id;

    private UUID farmLogId;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private String fileUrl;

    private String description;

    private String uploadedBy;

    private LocalDateTime uploadedAt;
}
