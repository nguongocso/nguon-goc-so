package vn.nguongocso.trace.recall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Response của một vụ việc thu hồi (NCL-08-CN-012). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallCaseResponse {

    private UUID id;
    private String caseCode;
    private RecallCaseStatus status;
    private UUID productionLotId;
    private String productionLotName;
    private UUID organizationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private UUID closedBy;
    private String remediationMeasures;
    private List<UUID> evidenceFileIds;
    private List<RecallLotResultResponse> lotResults;
    private int shipmentCount;
}
