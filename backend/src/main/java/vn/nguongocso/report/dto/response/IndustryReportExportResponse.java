package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response trả về sau khi xuất báo cáo tổng hợp ngành thành công.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndustryReportExportResponse {
    private String fileUrl;

    private String format;

    private LocalDateTime exportedAt;

    private UUID auditLogId;
}