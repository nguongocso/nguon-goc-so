package vn.nguongocso.alert.dto.request;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Request DTO cho việc tạo log hoạt động.
 */
@Getter
@Builder
public class ActivityLogRequest {
    private UUID userId;

    private String username;

    private String fullName;

    private String actorRole;

    private UUID organizationId;

    private String action;

    private String description;

    private String entityType;

    private UUID entityId;

    private String beforeValue;

    private String afterValue;

    private String ipAddress;
}
