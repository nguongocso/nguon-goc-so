package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một sự kiện chuỗi cung ứng được ánh xạ theo lược đồ GS1 mô phỏng.
 *
 * <p>
 * Lược đồ mô phỏng tổ chức sự kiện theo bốn chiều:
 * </p>
 *
 * <ul>
 * <li><b>who</b> – actor ghi nhận sự kiện ({@code recordedBy}).</li>
 * <li><b>when</b> – thời điểm ghi nhận ({@code recordedAt}).</li>
 * <li><b>where</b> – địa điểm sự kiện ({@code location}).</li>
 * <li><b>why/what</b> – loại sự kiện + dữ liệu chi tiết ({@code eventType} +
 * {@code details}).</li>
 * </ul>
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1Event {

    /** ID sự kiện (ChainEvent.id). */
    @JacksonXmlProperty(localName = "eventId")
    private UUID eventId;

    /** Loại sự kiện (ChainEvent.eventType – enum hiện có). */
    @JacksonXmlProperty(localName = "eventType")
    private String eventType;

    /** Nhãn tiếng Việt của loại sự kiện. */
    @JacksonXmlProperty(localName = "eventTypeLabel")
    private String eventTypeLabel;

    /** Thời điểm ghi nhận (ChainEvent.recordedAt). */
    @JacksonXmlProperty(localName = "recordedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recordedAt;

    /** Actor ghi nhận (ChainEvent.recordedBy.fullName). */
    @JacksonXmlProperty(localName = "recordedBy")
    private String recordedBy;

    /** Địa điểm sự kiện (có thể null kèm warning). */
    @JacksonXmlProperty(localName = "location")
    private Gs1EventLocation location;

    /** Dữ liệu chi tiết sự kiện (ChainEvent.eventData đã parse). */
    @JacksonXmlProperty(localName = "details")
    private Map<String, Object> details;
}