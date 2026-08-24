package vn.nguongocso.alert.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho log hoạt động.
 */
@Getter
@Builder
public class ActivityLogResponse {
    private UUID id;

    private UUID userId;

    private String username;

    private String fullName;

    private String actorName;

    private String action;

    private String actionType;

    private String description;

    private String entityType;

    private String targetType;

    private String entityId;

    private String targetId;

    private String ipAddress;

    private String details;

    private LocalDateTime createdAt;
}
