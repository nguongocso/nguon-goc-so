package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.report.enums.LotAlertType;

/**
 * DTO đại diện cho một dòng lô có cảnh báo trong danh sách báo cáo theo địa bàn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLotSummaryResponse {
    private UUID lotId;
    private String lotCode;
    private String lotName;
    private UUID organizationId;
    private String organizationName;
    private UUID productCategoryId;
    private String productCategoryName;
    private String farmAreaName;
    private String communeName;
    private String provinceName;
    private String lotStatus;
    private List<LotAlertType> alertTypes;
    private LotAlertType primaryAlertType;
    private int alertCount;
    private LocalDateTime latestAlertTriggeredAt;
    private List<AlertBadgeSummary> alertSummaries;
    private LocalDateTime createdAt;
}
