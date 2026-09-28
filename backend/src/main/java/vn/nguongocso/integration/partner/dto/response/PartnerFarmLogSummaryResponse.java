package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tóm tắt nhật ký canh tác trong hồ sơ truy xuất của đối tác.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerFarmLogSummaryResponse {
    private Integer totalLogsRecorded;

    private LocalDateTime lastActivityAt;
}
