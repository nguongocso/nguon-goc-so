package vn.nguongocso.export.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTO yêu cầu xem trước tệp PDF mẫu hồ sơ truy xuất đối tác.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewTemplatePdfRequest {

    /** Tên mẫu hồ sơ đang thiết lập */
    private String name;

    /** Tên đối tác áp dụng */
    private String partnerName;

    /** Danh sách mã các trường dữ liệu được chọn (ví dụ: organization.name) */
    @JsonProperty("selectedFieldKeys")
    @JsonAlias({"selectedFieldKeys", "fields"})
    private Set<String> selectedFieldKeys;

    /** Danh sách chi tiết các trường được chọn nếu gửi từ form cấu hình */
    private List<FieldSelectionDto> selectedFields;

    /** ID lô hàng mẫu thử nghiệm (tùy chọn) */
    private UUID shipmentId;
}
