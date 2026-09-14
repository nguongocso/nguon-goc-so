package vn.nguongocso.alert.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Kết quả nội bộ phân nhánh xuất trực tiếp hoặc xử lý nền. */
@Getter
@Builder
public class ActivityLogExportResult {
    private String mode;
    private byte[] csvBytes;
    private ActivityLogExportJobResponse job;
}
