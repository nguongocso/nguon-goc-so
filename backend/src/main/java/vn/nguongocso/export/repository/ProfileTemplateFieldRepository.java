package vn.nguongocso.export.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.export.entity.ProfileTemplateField;

import java.util.List;
import java.util.UUID;

/** Repository quản lý trường cấu hình của mẫu hồ sơ. */
@Repository
public interface ProfileTemplateFieldRepository extends JpaRepository<ProfileTemplateField, UUID> {
    /** Lấy danh sách các trường thuộc mẫu theo thứ tự sắp xếp. */
    List<ProfileTemplateField> findByTemplate_IdOrderBySortOrderAsc(UUID templateId);

    /** Xóa toàn bộ trường thuộc một mẫu. */
    void deleteByTemplate_Id(UUID templateId);
}
