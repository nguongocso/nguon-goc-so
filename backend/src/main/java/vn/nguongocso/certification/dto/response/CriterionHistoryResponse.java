package vn.nguongocso.certification.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Lịch sử kiểm nghiệm của một chỉ tiêu trên một lô sản xuất.
 */
@Getter
@Setter
@Builder
public class CriterionHistoryResponse {
    private Long criterionDefinitionId;

    private String criterionCode;

    private String criterionName;

    private List<CriterionHistoryEntry> history;
}
