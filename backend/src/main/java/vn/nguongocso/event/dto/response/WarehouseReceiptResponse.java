package vn.nguongocso.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi cho sự kiện nhập kho và đối chiếu số lượng.
 *
 * @author Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class WarehouseReceiptResponse {
    private UUID id;
    private ChainEventType eventType;
    private UUID shipmentId;
    private String shipmentName;
    private Double declaredQuantity;
    private Double receivedQuantity;
    private Double discrepancy;
    private Double discrepancyPercent;
    private Boolean isDiscrepancyExceeded;
    private Boolean reasonRequired;
    private String reason;
    private String conditionNote;
    private LocalDate receiptDate;
    private LocalDateTime recordedAt;
    private String recordedBy;
    private Boolean notificationSent;
}