package vn.nguongocso.alert.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sự kiện ghi nhật ký hoạt động của người dùng.
 */
@Getter
@Builder
public class ActivityLogEvent {
    private UUID userId;

    private String username;

    private String fullName;

    private String actorRole;

    private UUID organizationId;

    private String action;

    private String description;

    private String entityType;

    private String entityId;

    private String beforeValue;

    private String afterValue;

    private String ipAddress;

    private LocalDateTime timestamp;
}
