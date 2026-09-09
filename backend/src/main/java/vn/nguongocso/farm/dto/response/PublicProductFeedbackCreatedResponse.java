package vn.nguongocso.farm.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;

@Getter
@Builder
public class PublicProductFeedbackCreatedResponse {
    private UUID id;
    private UUID productionLotId;
    private ProductFeedbackStatus status;
    private LocalDateTime createdAt;
    private String lookupCode;
}
