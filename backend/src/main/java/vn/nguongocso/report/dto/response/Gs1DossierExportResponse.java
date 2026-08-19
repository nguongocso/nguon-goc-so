package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phản hồi xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng.
 *
 * <p>
 * DTO này biểu diễn hồ sơ theo bốn chiều dữ liệu {@code who / when / where / why}.
 * Khi xuất ở định dạng {@code json}, DTO được bọc trong {@code ApiResult}. Khi
 * xuất ở định dạng {@code xml}, DTO là root element {@code <gs1Dossier>} (không
 * bọc {@code ApiResult}) để giữ ngữ nghĩa lược đồ như tài liệu thiết kế.
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
@JacksonXmlRootElement(localName = "gs1Dossier")
public class Gs1DossierExportResponse {

    /** Thông tin lô hàng. */
    @JacksonXmlProperty(localName = "shipment")
    private Gs1ShipmentInfo shipment;

    /** Danh sách sự kiện đã ánh xạ theo lược đồ GS1, sắp xếp recordedAt ASC. */
    @JacksonXmlElementWrapper(localName = "events")
    @JacksonXmlProperty(localName = "event")
    private List<Gs1Event> events;

    /** Bảng ánh xạ hệ thống → lược đồ GS1 (nếu {@code includeMapping=true}). */
    @JacksonXmlProperty(localName = "mapping")
    private Map<String, String> mapping;

    /** Danh sách cảnh báo dữ liệu thiếu/không đầy đủ. */
    @JacksonXmlElementWrapper(localName = "warnings")
    @JacksonXmlProperty(localName = "warning")
    private List<Gs1Warning> warnings;

    /** Thời điểm xuất hồ sơ (server time). */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JacksonXmlProperty(localName = "exportedAt")
    private LocalDateTime exportedAt;

    /** Người xuất hồ sơ (authenticated principal). */
    @JacksonXmlProperty(localName = "exportedBy")
    private String exportedBy;

    /** Phiên bản lược đồ mô phỏng. */
    @JacksonXmlProperty(localName = "schemaVersion")
    private String schemaVersion;

    /** Mô tả lược đồ (không phải chứng nhận GS1 compliance). */
    @JacksonXmlProperty(localName = "schemaDescription")
    private String schemaDescription;
}