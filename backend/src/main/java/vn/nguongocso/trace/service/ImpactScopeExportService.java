package vn.nguongocso.trace.service;

import vn.nguongocso.auth.service.CustomUserDetails;

public interface ImpactScopeExportService {

    /**
     * Xuất tệp báo cáo cây truy vết phạm vi ảnh hưởng (Excel hoặc PDF).
     *
     * @param code Mã lô sản xuất, mã lô hàng hoặc mã tem
     * @param format Định dạng file ("EXCEL" hoặc "PDF")
     * @param currentUser Người dùng hiện tại
     * @return mảng byte của tệp xuất
     */
    byte[] exportImpactScopeReport(String code, String format, CustomUserDetails currentUser);
}
