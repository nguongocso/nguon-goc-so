package vn.nguongocso.certification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Phản hồi tải lên phiếu kết quả kiểm nghiệm.
 */
@Getter
@Setter
@Builder
public class InspectionResultFileUploadResponse {

    /**
     * Đường dẫn tệp phiếu kết quả đã lưu (filePath).
     */
    private String filePath;
}