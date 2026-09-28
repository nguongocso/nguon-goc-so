package vn.nguongocso.alert.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả xem trước số bản ghi sẽ được xuất.
 */
@Getter
@Builder
public class ActivityLogExportPreviewResponse {
    private long count;

    private String mode;
}
