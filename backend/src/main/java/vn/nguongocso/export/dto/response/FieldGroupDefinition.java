package vn.nguongocso.export.dto.response;

import lombok.*;
import vn.nguongocso.export.enums.ProfileFieldGroup;

import java.util.List;

/**
 * DTO định nghĩa nhóm trường phục vụ hiển thị trên giao diện cấu hình mẫu.
 */
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
}
