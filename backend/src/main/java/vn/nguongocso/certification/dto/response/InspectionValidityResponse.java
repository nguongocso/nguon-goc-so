package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionValidityStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Khối dữ liệu hiệu lực kết quả kiểm nghiệm của lô sản xuất (NCL-11-CN-004).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionValidityResponse {
    private Boolean requiresInspection;

    private InspectionValidityStatus status;

    private LocalDate earliestExpiryDate;

    private Long daysRemaining;

    private Long daysOverdue;

    private Boolean canActivate;

    private Boolean canCreateNewRequest;

    private UUID latestPassedRequestId;

    private Long inactiveStampCount;

    private Long totalStamps;

    private java.util.List<CriterionValidityResponse> criteria;

    private java.util.List<String> expiringCriteria;

    private java.util.List<String> expiredCriteria;

    public LocalDate getExpiryDate() {
        return earliestExpiryDate;
    }

    public Long getDaysUntilExpiry() {
        return daysRemaining;
    }

    public String getInspectionValidityStatus() {
        return status != null ? status.name() : null;
    }

    public String getInspectionExpiryDate() {
        return earliestExpiryDate != null ? earliestExpiryDate.toString() : null;
    }

    public Long getInspectionDaysRemaining() {
        return daysRemaining;
    }
}
