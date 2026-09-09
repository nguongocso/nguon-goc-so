package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;

@Getter
@Builder
public class PublicProductFeedbackLookupResponse {
    private ProductFeedbackStatus status;
    private String publicResponse;
}
