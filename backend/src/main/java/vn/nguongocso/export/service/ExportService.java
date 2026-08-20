package vn.nguongocso.export.service;

import org.springframework.core.io.Resource;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;

/** Thực hiện xuất dữ liệu công khai. */
public interface ExportService {
    /** Xuất dữ liệu open data theo định dạng yêu cầu. */
    Resource exportOpenData(ExportOpenDataRequest request, CustomUserDetails currentUser);
}