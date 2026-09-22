package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.UUID;

/** DTO response thông tin tổ chức nhận bị ảnh hưởng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingOrganizationTraceDto {

    private UUID organizationId;

    private String organizationName;

    private LocalDateTime receivedAt;

    private Long receivedQuantity;

    private ChainEventType eventType;

    private String eventTypeName;
}

