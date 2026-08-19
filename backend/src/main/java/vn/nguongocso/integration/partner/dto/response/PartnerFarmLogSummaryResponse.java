package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerFarmLogSummaryResponse {
    private Integer totalLogsRecorded;
    private LocalDateTime lastActivityAt;
}
