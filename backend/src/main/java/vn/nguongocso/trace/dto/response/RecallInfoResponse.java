package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cung cấp thông tin tóm tắt về tình trạng thu hồi của một lô hàng.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecallInfoResponse {
    private UUID shipmentId;

    private Boolean recalled;

    private String reason;

    private LocalDateTime recalledAt;
}