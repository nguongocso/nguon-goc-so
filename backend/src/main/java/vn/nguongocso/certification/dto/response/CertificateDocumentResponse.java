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

    /**
     * Tên tệp gốc.
     */
    private String fileName;

    /**
     * Kiểu nội dung MIME (application/pdf, image/jpeg, image/png).
     */
    private String contentType;

    /**
     * Kích thước tệp (bytes).
     */
    private Long fileSize;

    /**
     * Đường dẫn API xem tệp chứng nhận an toàn (có kiểm tra quyền).
     */
    private String viewUrl;
}
