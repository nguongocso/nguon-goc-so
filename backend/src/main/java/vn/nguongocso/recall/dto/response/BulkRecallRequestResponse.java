package vn.nguongocso.recall.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO cho yêu cầu thu hồi hàng loạt (NCL-08-CN-011).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRecallRequestResponse {

    private UUID id;
    private UUID productionLotId;
    private String productionLotName;
    private String reason;
    private String evidence;
    private String status;
    private UserInfo requestedBy;
    private LocalDateTime requestedAt;
    private UserInfo approvedBy;
    private LocalDateTime approvedAt;
    private String approvalRemarks;
    private UserInfo rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private UserInfo closedBy;
    private LocalDateTime closedAt;
    private String remediationMeasures;
    private List<UUID> evidenceFileIds;
    private List<RecallEvidenceResponse> evidenceFiles;
    private String caseCode;
    private List<BulkRecallShipmentItem> shipments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Thông tin người dùng rút gọn.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID userId;
        private String fullName;
    }
}
