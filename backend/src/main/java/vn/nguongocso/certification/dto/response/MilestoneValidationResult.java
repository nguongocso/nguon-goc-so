package vn.nguongocso.certification.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Kết quả kiểm tra mốc canh tác trước khi đóng gói.
 * Kế thừa ArrayList<String> để tương thích ngược với các lời gọi trả về List<String>
 * chứa danh sách tên mốc canh tác còn thiếu.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MilestoneValidationResult extends ArrayList<String> {

    private boolean eligible;
    private String message;

    public MilestoneValidationResult() {
        super();
        this.eligible = true;
    }

    public MilestoneValidationResult(boolean eligible, String message) {
        super();
        this.eligible = eligible;
        this.message = message;
    }

    public MilestoneValidationResult(Collection<? extends String> c) {
        super(c);
        this.eligible = c.isEmpty();
    }

    public static MilestoneValidationResultBuilder builder() {
        return new MilestoneValidationResultBuilder();
    }

    public static class MilestoneValidationResultBuilder {
        private boolean eligible = true;
        private String message;
        private List<String> missingMilestones = new ArrayList<>();

        public MilestoneValidationResultBuilder eligible(boolean eligible) {
            this.eligible = eligible;
            return this;
        }

        public MilestoneValidationResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public MilestoneValidationResultBuilder missingMilestones(List<String> missingMilestones) {
            if (missingMilestones != null) {
                this.missingMilestones = missingMilestones;
            }
            return this;
        }

        public MilestoneValidationResult build() {
            MilestoneValidationResult result = new MilestoneValidationResult(eligible, message);
            result.addAll(missingMilestones);
            return result;
        }
    }
}
