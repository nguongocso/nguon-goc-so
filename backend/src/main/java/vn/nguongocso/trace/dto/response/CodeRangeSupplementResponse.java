package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Thông tin phản hồi yêu cầu cấp bổ sung dải mã truy xuất. */
@Getter
@Setter
@Builder
public class CodeRangeSupplementResponse {
    private UUID id;

    private UUID organizationId;

    private String organizationName;

    private UserInfo requestedBy;

    private LocalDateTime requestedAt;

    private Long requestedQuantity;

    private Long approvedQuantity;

    private String status;

    private String reason;

    private List<UUID> evidenceEventIds;

    private List<EvidenceEventResponse> evidenceEvents;

    private UserInfo approvedBy;

    private LocalDateTime approvedAt;

    private String approvalRemarks;

    private UserInfo rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectionReason;

    private int notifiedCount;

    /** Thông tin người dùng. */
    @Getter
    @Setter
    @Builder
    public static class UserInfo {

        private UUID userId;

        private String fullName;
    }
}
