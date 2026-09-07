package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.trace.enums.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTraceDto {
    private UUID id;
    private String code;
    private String name;
    private ShipmentStatus status;
    private Long totalQuantity;
    private String packagingInfo;
    private LocalDateTime createdAt;
    private long activatedStampsCount;
    private ScanStatsTraceDto scanStats;
    private List<ReceivingOrganizationTraceDto> receivingOrganizations;
}
