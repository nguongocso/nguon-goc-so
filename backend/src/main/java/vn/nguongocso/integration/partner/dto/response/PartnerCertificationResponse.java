package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerCertificationResponse {
    private String certificationName;
    private String standardCode;
    private String certificateNumber;
    private LocalDate issuedDate;
    private LocalDate expiredDate;
    private String issuingBody;
}
