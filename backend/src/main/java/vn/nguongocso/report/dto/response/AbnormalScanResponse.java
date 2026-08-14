package vn.nguongocso.report.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi tra cứu bất thường.
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbnormalScanResponse {
    private UUID scanId;

    private String codeValue;

    private String lotName;

    private LocalDateTime scannedAt;

    private String ipAddress;

    private String userAgent;

    private String location;

    private Double latitude;

    private Double longitude;

    private String reason;
}
