package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadata tệp chứng nhận đính kèm (không để lộ đường dẫn lưu trữ vật lý).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateDocumentResponse {
    private String fileName;

    private String contentType;

    private Long fileSize;

    private String viewUrl;
}
