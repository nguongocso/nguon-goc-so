package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;

import vn.nguongocso.farm.enums.ProductFeedbackStatus;

@Getter
@Builder
/** Kết quả tra cứu công khai trạng thái phản hồi sản phẩm. */
public class PublicProductFeedbackLookupResponse {
    private ProductFeedbackStatus status;
    private String publicResponse;
}
