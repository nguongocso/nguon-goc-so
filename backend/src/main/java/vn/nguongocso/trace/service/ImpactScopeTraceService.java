package vn.nguongocso.trace.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.trace.dto.response.ImpactScopeTraceResponse;

public interface ImpactScopeTraceService {

    /**
     * Truy vết phạm vi ảnh hưởng hai chiều (Upstream & Downstream) từ mã lô sản xuất, mã lô hàng hoặc mã tem.
     *
     * @param code Mã tìm kiếm (Lô sản xuất / Lô hàng / Tem QR)
     * @param currentUser Người dùng hiện tại (quản lý HTX / Admin)
     * @return Dữ liệu cây truy vết phạm vi ảnh hưởng
     */
    ImpactScopeTraceResponse getImpactScopeTrace(String code, CustomUserDetails currentUser);
}
