package vn.nguongocso.recall.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Thông tin phản hồi yêu cầu thu hồi lô hàng. */
@Getter
@Setter
@Builder
public class RecallRequestResponse {

    private UUID id;

    private UUID shipmentId;

    private String shipmentName;

    private UUID lotId;

    private UUID sourceFeedbackId;

    private String lotName;

    private UserInfo requestedBy;

    private LocalDateTime requestedAt;

    private String status;

    private String reason;

    private String evidence;

    private UserInfo approvedBy;

    private LocalDateTime approvedAt;

    private String approvalRemarks;

    private UserInfo rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectionReason;

    private int notifiedBuyerCount;

    /** Thông tin người dùng. */
    @Getter
    @Setter
    @Builder
    public static class UserInfo {

        private UUID userId;

        private String fullName;
    }
}
