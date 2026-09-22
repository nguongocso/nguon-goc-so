package vn.nguongocso.certification.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi thông tin tối thiểu của yêu cầu kiểm nghiệm trên cổng công khai.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionResultEntryResponse {
    private String testingUnitName;

    private String testingUnit;

    private String lotCode;

    private String lotName;

    private LocalDate sampleSentDate;

    private LocalDateTime expiresAt;

    private List<PublicInspectionResultEntryCriterionResponse> criteria;
}
