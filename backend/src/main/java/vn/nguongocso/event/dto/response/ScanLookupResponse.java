package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Response tra cứu mã truy xuất khi quét mã để mở biểu mẫu ghi sự kiện. */
@Getter
@Builder
public class ScanLookupResponse {
    private Boolean valid;

    private String message;

    private String traceCode;

    private UUID shipmentId;

    private String shipmentName;

    private String shipmentStatus;

    private UUID productionLotId;

    private String productCategoryName;

    private String farmAreaName;

    private UUID organizationId;

    private String organizationName;

    private List<String> allowedEventTypes;

    private String lastEventType;

    private LocalDateTime lastEventRecordedAt;

    private Long totalQuantity;

    private Boolean storageEligible;
}
