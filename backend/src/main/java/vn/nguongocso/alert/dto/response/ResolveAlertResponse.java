package vn.nguongocso.alert.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.alert.enums.AlertStatus;

/** Kết quả xử lý cảnh báo. */
@Getter
@Setter
public class ResolveAlertResponse {
    private UUID id;

    private AlertStatus status;

    private LocalDateTime resolvedAt;

    private UUID resolvedBy;
}