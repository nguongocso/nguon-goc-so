package vn.nguongocso.export.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.export.enums.ProfileFieldGroup;

/** DTO định nghĩa nhóm trường phục vụ hiển thị trên giao diện cấu hình mẫu. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldGroupDefinition {
    /** Mã nhóm trường */
    private ProfileFieldGroup fieldGroup;

    /** Tên hiển thị của nhóm */
    private String groupLabel;

    /** Danh sách các trường thuộc nhóm */
    private List<FieldItemDefinition> fields;

    /** Alias thuộc tính group để tương thích hoàn toàn với frontend */
    public String getGroup() {
        return fieldGroup != null ? fieldGroup.name() : null;
    }
}
