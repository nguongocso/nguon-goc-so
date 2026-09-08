package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;

/**
 * DTO phản hồi thông tin phản hồi sản phẩm.
 */
@Getter
@Setter
@Builder
public class ProductFeedbackResponse {
    private UUID id;

    private UUID productionLotId;

    private String productionLotName;

    private String content;

    private LocalDateTime createdAt;

    private UUID organizationId;

    private String organizationName;

    private String productCategoryName;

    private UUID traceCodeId;

    private String traceCodeValue;

    private ProductFeedbackStatus status;

    private ProductFeedbackSeverity severity;

    private UUID assignedToUserId;

    private String assignedToName;

    private LocalDateTime assignedAt;

    private String processingContent;

    private String publicResponse;

    private String closeReason;

    private UUID closedByUserId;

    private String closedByName;

    private LocalDateTime closedAt;

    private UUID latestRecallRequestId;

    private String latestRecallRequestStatus;

    private boolean hasPendingRecallRequest;

    private LocalDateTime updatedAt;
}
