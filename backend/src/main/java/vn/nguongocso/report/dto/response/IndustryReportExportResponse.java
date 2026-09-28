package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phản hồi xuất báo cáo tổng hợp ngành. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndustryReportExportResponse {
    private String fileUrl;

    private String format;

    private LocalDateTime exportedAt;

    private UUID auditLogId;
}
