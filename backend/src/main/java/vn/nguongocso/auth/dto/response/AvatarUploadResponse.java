package vn.nguongocso.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về sau khi tải lên ảnh đại diện thành công (NCL-01-CN-010).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadResponse {
    private String avatarUrl;
}
