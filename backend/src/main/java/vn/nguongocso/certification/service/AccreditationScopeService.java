package vn.nguongocso.certification.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.response.AccreditationScopeSummaryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý phạm vi công nhận của đơn vị kiểm nghiệm
 * (NCL-11-CN-006 Phase 2).
 */
public interface AccreditationScopeService {

    /**
     * Lấy phạm vi công nhận hiện tại của một đơn vị kiểm nghiệm.
     * Mọi vai trò đã xác thực đều đọc được (để hiển thị cảnh báo khi tạo yêu cầu).
     */
    AccreditationScopeSummaryResponse getAccreditationScope(UUID testingUnitId);

    /**
     * Cập nhật (REPLACE-ALL) phạm vi công nhận của một đơn vị kiểm nghiệm.
     * Chỉ Quản trị viên nền tảng (VT-01) được thực hiện.
     */
    AccreditationScopeSummaryResponse updateAccreditationScope(
            UUID testingUnitId,
            List<Long> criterionDefinitionIds,
            CustomUserDetails currentUser);
}
