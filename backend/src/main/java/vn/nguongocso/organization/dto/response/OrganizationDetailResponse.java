package vn.nguongocso.organization.dto.response;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;

/**
 * Phản hồi khi truy vấn chi tiết thông tin tổ chức.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDetailResponse {
    private OrganizationProfileResponse profile;

    private List<OrganizationUserResponse> members;
}