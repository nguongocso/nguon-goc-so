package vn.nguongocso.export.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.export.enums.ProfileFieldGroup;

/** DTO thông tin trường cấu hình trong mẫu hồ sơ trả về cho client. */
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
