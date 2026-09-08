package vn.nguongocso.farm.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignProductFeedbackRequest {

    @NotNull(message = "Người xử lý không được để trống")
    private UUID assignedToUserId;
}
