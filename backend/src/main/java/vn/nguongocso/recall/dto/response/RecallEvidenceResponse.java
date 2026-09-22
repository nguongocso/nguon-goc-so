package vn.nguongocso.recall.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Thông tin phản hồi tệp biên bản đính kèm vụ việc thu hồi. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallEvidenceResponse {

    private UUID id;

    private String fileName;

    private Long fileSize;

    private String contentType;

    private String downloadUrl;

    private LocalDateTime uploadedAt;
}
