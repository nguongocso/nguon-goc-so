package vn.nguongocso.export.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO mô tả thông tin chi tiết một trường dữ liệu trong danh mục.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldItemDefinition {

    /** Khóa định danh trường (vd: organization.name) */
    private String fieldKey;

    /** Tên hiển thị tiếng Việt */
    private String displayName;

    /** Cờ bắt buộc theo quy tắc QTN-11 */
    private boolean mandatory;

    /** Diễn giải nghiệp vụ của trường */
    private String description;

    /**
     * Alias key để tương thích với frontend
     */
    public String getKey() {
        return fieldKey;
    }

    /**
     * Alias label để tương thích với frontend
     */
    public String getLabel() {
        return displayName;
    }

    /**
     * Alias isMandatory để tương thích với frontend
     */
    @JsonProperty("isMandatory")
    public boolean isMandatoryField() {
        return mandatory;
    }
}

