package vn.nguongocso.farm.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import vn.nguongocso.farm.enums.ProductFeedbackStatus;

@Getter
@Builder
/** Kết quả tạo phản hồi sản phẩm công khai kèm mã tra cứu. */
public class PublicProductFeedbackCreatedResponse {
    private UUID id;
    private UUID productionLotId;
    private ProductFeedbackStatus status;
    private LocalDateTime createdAt;
    private String lookupCode;
}
