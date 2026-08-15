package vn.nguongocso.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vị trí địa lý của một sự kiện trong lược đồ GS1 mô phỏng.
 *
 * <p>
 * Trong domain hiện tại {@code ChainEvent.location} là một {@code Point} (geometry)
 * nên chỉ có toạ độ (latitude/longitude). Trường {@code address} không tồn tại
 * trong entity và sẽ được xuất là {@code null} kèm warning theo đúng quy tắc
 * "không tự sinh dữ liệu".
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
public class Gs1EventLocation {

    /** Vĩ độ (từ {@code Point.getY()}). */
    @JacksonXmlProperty(localName = "latitude")
    private Double latitude;

    /** Kinh độ (từ {@code Point.getX()}). */
    @JacksonXmlProperty(localName = "longitude")
    private Double longitude;

    /**
     * Địa chỉ. Không tồn tại trong domain hiện tại → luôn {@code null} kèm warning.
     */
    @JacksonXmlProperty(localName = "address")
    private String address;
}