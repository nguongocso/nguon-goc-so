package vn.nguongocso.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi kết quả đánh dấu đã đọc toàn bộ thông báo.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarkAllReadResponse {
    private int markedReadCount;
}