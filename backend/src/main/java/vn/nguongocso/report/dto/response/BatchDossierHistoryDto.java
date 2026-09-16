package vn.nguongocso.report.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDossierHistoryDto {

    private UUID id;
    private String title;
    private LocalDateTime exportedAt;
    private String exporterName;
    private String organizationName;
    private int totalSelectedLots;
    private int eligibleLotsCount;
    private int ineligibleLotsCount;
    private String fileName;
    private Long fileSize;
    private String status;
    private String ipAddress;

    /** Mẫu hồ sơ đã áp dụng (NCL-07-CN-007), NULL nếu dùng mẫu mặc định hoặc bộ trường chuẩn */
    private UUID templateId;
}
