package vn.nguongocso.export.dto.response;

import lombok.*;
import vn.nguongocso.export.enums.ProfileFieldGroup;

import java.util.UUID;

/**
 * DTO thông tin trường cấu hình trong mẫu hồ sơ trả về cho client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileTemplateFieldResponse {

    /** ID trường */
    private UUID id;

    /** Khóa định danh trường (vd: organization.name) */
    private String fieldKey;

    /** Nhóm trường */
    private ProfileFieldGroup fieldGroup;

    /** Tên hiển thị */
    private String displayName;

    /** Cờ bắt buộc theo QTN-11 */
    private boolean mandatory;

    /** Thứ tự sắp xếp */
    private int sortOrder;
}
