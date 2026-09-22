package vn.nguongocso.trace.service;

import vn.nguongocso.auth.service.CustomUserDetails;

/** Service xuất báo cáo phạm vi ảnh hưởng. */
public interface ImpactScopeExportService {
    /** Xuất tệp báo cáo cây truy vết phạm vi ảnh hưởng. */
    byte[] exportImpactScopeReport(
        String code,
        String format,
        CustomUserDetails currentUser
    );
}
