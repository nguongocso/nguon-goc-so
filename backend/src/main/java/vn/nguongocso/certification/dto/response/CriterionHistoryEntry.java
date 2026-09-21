package vn.nguongocso.certification.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Một mục trong lịch sử kiểm nghiệu của một chỉ tiêu — đại diện cho kết quả kiểm nghiệm tại một thời điểm (một yêu cầu
 * kiểm nghiệm).
 */
@Getter
@Setter
@Builder
public class CriterionHistoryEntry {
    private String requestId;

    private LocalDate sampleSentDate;

    private LocalDate resultDate;

    private LocalDate expiryDate;

    private boolean passed;

    private String testingUnit;

    private String createdByName;

    private LocalDateTime createdAt;
}
