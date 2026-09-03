package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for assigning milestones to a product category.
 * Story: NCL-09-CN-011
 */
@Getter
@Setter
public class CategoryMilestoneRequest {

    @NotNull(message = "Danh sách mốc canh tác không được rỗng")
    private List<Long> milestoneIds;

    private UUID standardId;

    /**
     * Danh sách id các mốc được đánh dấu bắt buộc trong số {@code milestoneIds}.
     * Khi null (không gửi) thì tất cả mốc được gán đều được coi là bắt buộc.
     */
    private List<Long> mandatoryMilestoneIds;
}
