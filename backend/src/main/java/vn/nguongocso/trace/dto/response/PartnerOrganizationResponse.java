package vn.nguongocso.trace.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class PartnerOrganizationResponse { private UUID id; private String code; private String name; }
