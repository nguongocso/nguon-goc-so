package vn.nguongocso.export.service;

import java.util.UUID;

import org.springframework.core.io.Resource;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;

/** Thực hiện xuất dữ liệu công khai. */
public interface ExportService {
    /** Xuất dữ liệu open data theo định dạng yêu cầu. */
    Resource exportOpenData(ExportOpenDataRequest request, CustomUserDetails currentUser);

    /** Xuất hồ sơ truy xuất áp dụng mẫu cấu hình theo yêu cầu đối tác (NCL-07-CN-007). */
    Resource exportWithTemplate(UUID shipmentId, UUID templateId, String format, CustomUserDetails currentUser);
}