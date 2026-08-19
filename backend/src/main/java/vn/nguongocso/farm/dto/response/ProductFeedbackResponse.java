package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

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
}
