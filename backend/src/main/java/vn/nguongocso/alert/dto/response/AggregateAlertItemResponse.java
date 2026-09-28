package vn.nguongocso.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AggregateAlertType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thông tin chi tiết một dòng cảnh báo tổng hợp (NCL-08-CN-016).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAlertItemResponse {
    private UUID id;

    private AggregateAlertType type;

    private String typeName;

    private AlertSeverity severity;

    private String title;

    private String message;

    private String relatedEntityType;

    private UUID relatedEntityId;

    private String relatedEntityName;

    private LocalDateTime createdAt;

    private String actionUrl;

    private UUID organizationId;

    private String organizationName;

    private String status;
}
