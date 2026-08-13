package vn.nguongocso.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO địa điểm sự kiện trong hồ sơ GS1 mô phỏng.
 *
 * @author NCL-12-CN-003
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLocation {
    // Vĩ độ
    private Double latitude;

    // Kinh độ
    private Double longitude;

    // Địa chỉ (không tồn tại trong domain hiện tại -> null)
    private String address;
}