package vn.nguongocso.trace.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu xuất danh sách mã tem ra file CSV (NCL-04-CN-008).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTraceCodesRequest {
    private String status;
    private String search;
}
