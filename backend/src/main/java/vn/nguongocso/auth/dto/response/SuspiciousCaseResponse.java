package vn.nguongocso.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspiciousCaseResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String fullName;
    private UUID organizationId;
    private String organizationName;
    private String status;
    private int anomalyCount;
    private OffsetDateTime firstDetectedAt;
    private OffsetDateTime lastDetectedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
}
