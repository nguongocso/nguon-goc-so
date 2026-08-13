package vn.nguongocso.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO cảnh báo dữ liệu thiếu/không đầy đủ trong hồ sơ GS1 mô phỏng.
 *
 * @author NCL-12-CN-003
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warning {
    // ID sự kiện liên quan
    private UUID eventId;

    // Tên trường thiếu dữ liệu
    private String field;

    // Nội dung cảnh báo
    private String message;
}