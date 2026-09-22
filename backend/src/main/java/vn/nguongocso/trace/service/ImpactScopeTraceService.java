package vn.nguongocso.trace.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.trace.dto.response.ImpactScopeTraceResponse;

/** Service truy vết phạm vi ảnh hưởng hai chiều. */
public interface ImpactScopeTraceService {
    /** Truy vết phạm vi ảnh hưởng hai chiều từ mã truy xuất. */
    ImpactScopeTraceResponse getImpactScopeTrace(
        String code,
        CustomUserDetails currentUser
    );
}
