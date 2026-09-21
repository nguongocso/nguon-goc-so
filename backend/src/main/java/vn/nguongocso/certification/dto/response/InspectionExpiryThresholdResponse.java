package vn.nguongocso.certification.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi thông tin cấu hình ngưỡng cảnh báo hết hiệu lực kiểm nghiệm
 * (NCL-11-CN-004).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionExpiryThresholdResponse {
    private Integer warningThresholdDays;

    private LocalDateTime updatedAt;

    private String updatedByName;
}
