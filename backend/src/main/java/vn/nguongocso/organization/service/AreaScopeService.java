package vn.nguongocso.organization.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;

/** Phân tích phạm vi tổ chức được phép xem của người gọi báo cáo theo địa bàn đã gán. */
public interface AreaScopeService {
    /** Thông báo chuẩn khi cán bộ quản lý ngành chưa được gán địa bàn nào. */
    String UNASSIGNED_MESSAGE = "Bạn chưa được phân công địa bàn quản lý nào.";

    /** Xác định tập tổ chức được phép xem dựa trên vai trò và tham số unitIds. */
    AreaScopeResult resolveOrganizationsForReports(CustomUserDetails user, List<UUID> unitIds);
}
