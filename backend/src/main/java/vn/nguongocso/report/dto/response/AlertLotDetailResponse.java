package vn.nguongocso.report.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phản hồi chi tiết một lô có cảnh báo cho Cán bộ quản lý ngành (Read-only).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLotDetailResponse {
    private AlertLotInfoItem lotInfo;
    private AlertLotOrgItem organization;
    private List<LotAlertEvidenceDetail> activeAlerts;
    private List<ReadonlyChainEventItem> timelineEvents;
}
