package vn.nguongocso.report.dto.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cảnh báo dữ liệu thiếu/không đầy đủ khi xuất hồ sơ GS1 mô phỏng.
 *
 * <p>
 * Warning không làm request thất bại (HTTP vẫn 200). Mỗi warning gắn với một
 * sự kiện và một trường dữ liệu cụ thể.
 * </p>
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1Warning {

    /** ID sự kiện liên quan (có thể null nếu warning ở cấp Shipment). */
    @JacksonXmlProperty(localName = "eventId")
    private UUID eventId;

    /** Tên trường dữ liệu thiếu (vd: {@code location}, {@code address}). */
    @JacksonXmlProperty(localName = "field")
    private String field;

    /** Mô tả chi tiết về cảnh báo. */
    @JacksonXmlProperty(localName = "message")
    private String message;
}