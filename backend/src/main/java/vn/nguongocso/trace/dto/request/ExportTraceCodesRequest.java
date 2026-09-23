package vn.nguongocso.trace.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Yêu cầu xuất danh sách mã tem ra file CSV. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTraceCodesRequest {
    private String status;

    private String search;
}
