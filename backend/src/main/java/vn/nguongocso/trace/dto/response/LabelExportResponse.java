package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO response kết quả xuất tem QR. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LabelExportResponse {
    private byte[] pdfBytes;

    private String fileName;

    private int quantity;

    private String labelSize;

    private int startIndex;

    private int endIndex;
}
