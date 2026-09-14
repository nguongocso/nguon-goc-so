package vn.nguongocso.export.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.export.entity.ProfileTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý dữ liệu mẫu hồ sơ truy xuất.
 */
@Repository
public interface ProfileTemplateRepository extends JpaRepository<ProfileTemplate, UUID> {

    /**
     * Lấy danh sách mẫu hồ sơ thuộc một tổ chức, sắp xếp theo tên (QTN-01, TC-04).
     */
    List<ProfileTemplate> findAllByOrganization_OrganizationIdOrderByNameAsc(UUID organizationId);

    /**
     * Tìm mẫu hồ sơ theo ID và tổ chức sở hữu (QTN-01).
     */
    Optional<ProfileTemplate> findByIdAndOrganization_OrganizationId(UUID id, UUID organizationId);

    /**
     * Tìm mẫu hồ sơ mặc định của một tổ chức.
     */
    Optional<ProfileTemplate> findByOrganization_OrganizationIdAndIsDefaultTrue(UUID organizationId);

    /**
     * Kiểm tra trùng tên mẫu trong cùng một tổ chức.
     */
    boolean existsByNameAndOrganization_OrganizationId(String name, UUID organizationId);

    /**
     * Kiểm tra trùng tên mẫu trong cùng một tổ chức ngoại trừ mẫu hiện tại (dùng khi cập nhật).
     */
    boolean existsByNameAndOrganization_OrganizationIdAndIdNot(String name, UUID organizationId, UUID id);

    /**
     * Tìm các mẫu mặc định khác của tổ chức (dùng để reset is_default khi mẫu mới được đặt làm default).
     */
    List<ProfileTemplate> findByOrganization_OrganizationIdAndIsDefaultTrueAndIdNot(UUID organizationId, UUID id);
}
