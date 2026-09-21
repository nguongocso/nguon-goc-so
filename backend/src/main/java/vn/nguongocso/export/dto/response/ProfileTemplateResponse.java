package vn.nguongocso.export.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO trả về thông tin chi tiết hoặc tóm tắt mẫu hồ sơ truy xuất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileTemplateResponse {

    /** Khóa chính định danh mẫu hồ sơ */
    private UUID id;

    /** ID tổ chức sở hữu mẫu */
    private UUID organizationId;

    /** Tên mẫu hồ sơ */
    private String name;

    /** Tên đối tác / khách hàng áp dụng */
    private String partnerName;

    /** Mô tả mục đích sử dụng */
    private String description;

    /** Cờ mẫu mặc định của tổ chức */
    @JsonProperty("isDefault")
    private boolean isDefault;

    /** Tổng số trường được chọn */
    private int totalFields;

    /** Danh sách các trường dữ liệu thuộc mẫu */
    private List<ProfileTemplateFieldResponse> fields;

    /** Thời điểm tạo */
    private LocalDateTime createdAt;

    /** Thời điểm cập nhật lần cuối */
    private LocalDateTime updatedAt;

    @JsonProperty("isDefault")
    public boolean isDefault() {
        return this.isDefault;
    }
}
