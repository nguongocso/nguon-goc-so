package vn.nguongocso.certification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Lớp StandardResponse đại diện cho phản hồi tiêu chuẩn trong hệ thống.
 */
@Getter
@Builder
public class StandardResponse {
    private UUID id;

    private String name;

    private String description;

    private String issuingBody;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}