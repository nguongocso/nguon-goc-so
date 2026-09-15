package vn.nguongocso.integration.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi kết quả kiểm tra bắn thử nghiệm webhook (Test Ping) tới đối tác (NCL-12-CN-006).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookTestPingResponse {

    private String targetUrl;
    private Integer httpStatus;
    private Long durationMs;
    private Boolean isSuccess;
    private String responseBody;
    private String errorMessage;
}
