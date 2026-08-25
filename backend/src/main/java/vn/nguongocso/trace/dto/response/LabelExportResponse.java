package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả xuất tem QR (NCL-04-CN-005).
 *
 * <p>
 * Chứa file PDF dưới dạng byte[] cùng metadata phục vụ header
 * {@code Content-Disposition} và ghi log.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelExportResponse {

    /** Nội dung file PDF. */
    private byte[] pdfBytes;

    /** Tên file gợi ý khi tải xuống. */
    private String fileName;

    /** Số tem thực tế đã xuất. */
    private int quantity;

    /** Khổ tem đã dùng. */
    private String labelSize;

    /** Chỉ số bắt đầu trong danh sách mã đã sinh. */
    private int startIndex;

    /** Chỉ số kết thúc (bao gồm). */
    private int endIndex;
}
