package vn.nguongocso.export.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.export.dto.request.CreateProfileTemplateRequest;
import vn.nguongocso.export.dto.request.UpdateProfileTemplateRequest;
import vn.nguongocso.export.dto.response.FieldGroupDefinition;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;
import vn.nguongocso.export.entity.ProfileTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service quản lý cấu hình mẫu hồ sơ truy xuất nguồn gốc theo đối tác.
 */
public interface ProfileTemplateService {

    /**
     * Tạo mới mẫu hồ sơ truy xuất (TC-01, TC-02).
     */
    ProfileTemplateResponse createTemplate(UUID orgId, CreateProfileTemplateRequest request, CustomUserDetails currentUser);

    /**
     * Lấy danh sách các mẫu hồ sơ thuộc tổ chức của người dùng (TC-04).
     */
    List<ProfileTemplateResponse> listTemplates(UUID orgId, CustomUserDetails currentUser);

    /**
     * Lấy chi tiết mẫu hồ sơ theo ID (TC-04).
     */
    ProfileTemplateResponse getTemplate(UUID orgId, UUID templateId, CustomUserDetails currentUser);

    /**
     * Cập nhật mẫu hồ sơ (TC-02).
     */
    ProfileTemplateResponse updateTemplate(UUID orgId, UUID templateId, UpdateProfileTemplateRequest request, CustomUserDetails currentUser);

    /**
     * Xóa mẫu hồ sơ.
     */
    void deleteTemplate(UUID orgId, UUID templateId, CustomUserDetails currentUser);

    /**
     * Lấy mẫu hồ sơ mặc định của tổ chức (TC-03).
     */
    ProfileTemplate getDefaultTemplate(UUID orgId);

    /**
     * Lấy DTO thông tin mẫu hồ sơ mặc định của tổ chức (TC-03).
     */
    ProfileTemplateResponse getDefaultTemplateResponse(UUID orgId, CustomUserDetails currentUser);

    /**
     * Lấy danh mục tất cả các trường dữ liệu hệ thống hỗ trợ cấu hình.
     */
    List<FieldGroupDefinition> getAllAvailableFields();

    /**
     * Xem trước nội dung hồ sơ truy xuất của lô hàng theo mẫu cấu hình (dạng JSON không tải file).
     */
    Map<String, Object> buildPreview(UUID shipmentId, UUID templateId, CustomUserDetails currentUser);
}
