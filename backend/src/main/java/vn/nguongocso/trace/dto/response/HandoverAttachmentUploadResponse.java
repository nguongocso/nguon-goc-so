package vn.nguongocso.trace.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Phản hồi tải lên chứng từ giao hàng của phiếu bàn giao.
 */
@Getter
@Setter
@Builder
public class HandoverAttachmentUploadResponse {

    /**
     * Đường dẫn file chứng từ đã lưu (filePath), gửi kèm trong
     * attachmentPath khi tạo phiếu bàn giao.
     */
    private String filePath;
}