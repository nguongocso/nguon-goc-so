package vn.nguongocso.trace.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/** DTO response thông tin tổ chức đối tác. */
@Data
@Builder
public class PartnerOrganizationResponse {
    private UUID id;

    private String code;

    private String name;
}
