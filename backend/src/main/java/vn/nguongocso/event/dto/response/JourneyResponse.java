package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi hành trình của lô hàng.
 *
 * @author Team WEB 1
 */
@Data
@Builder
public class JourneyResponse {
    private UUID shipmentId;

    private String shipmentName;

    private Integer totalEvents;

    private List<JourneyPointResponse> points;
}
