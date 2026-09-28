package vn.nguongocso.certification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO phản hồi để kiểm tra điều kiện kích hoạt tem.
 */
@Getter
@Setter
@Builder
public class CanActivateSealCheckResponse {
    private String productionLotId;

    private Boolean canActivate;

    private String reason;

    private LocalDate earliestExpiryDate;

    private Integer totalCriteria;

    private Integer passedCriteria;

    private Integer failedOrExpiredCriteria;
}
