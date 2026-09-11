package vn.nguongocso.recall.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin phản hồi tệp biên bản đính kèm vụ việc thu hồi (NCL-08-CN-012).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallEvidenceResponse {

    /** ID của tệp biên bản. */
    private UUID id;

    /** Tên gốc của tệp biên bản. */
    private String fileName;

    /** Kích thước tệp (bytes). */
    private Long fileSize;

    /** Định dạng nội dung tệp (MIME type). */
    private String contentType;

    /** Đường dẫn tải tệp. */
    private String downloadUrl;

    /** Thời điểm tải lên. */
    private LocalDateTime uploadedAt;
}
