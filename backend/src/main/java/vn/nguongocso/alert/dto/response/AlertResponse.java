package vn.nguongocso.alert.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;

/** Thông tin một cảnh báo. */
@Getter
@Setter
public class AlertResponse {
    private UUID id;

    private AlertType type;

    private String relatedEntityType;

    private UUID relatedEntityId;

    private AlertSeverity severity;

    private Object details;

    private AlertStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    private UUID resolvedBy;
}