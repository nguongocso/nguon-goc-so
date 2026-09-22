package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO phản hồi lượt quét bất thường. */
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
