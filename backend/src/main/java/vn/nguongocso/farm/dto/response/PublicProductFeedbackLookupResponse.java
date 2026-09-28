package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;

import vn.nguongocso.farm.enums.ProductFeedbackStatus;

/**
 * Kết quả tra cứu công khai trạng thái phản hồi sản phẩm.
*/
@Getter
@Builder
public class PublicProductFeedbackLookupResponse {
    private ProductFeedbackStatus status;

    private String publicResponse;
}
