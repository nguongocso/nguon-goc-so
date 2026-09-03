package vn.nguongocso.organization.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.enums.OrganizationUserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể OrganizationUser.
 */
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, UUID> {
	/**
	 * Tìm người dùng trong tổ chức theo người dùng và mã tổ chức.
	 *
	 * @param user    người dùng
	 * @param orgCode mã tổ chức
	 * @return Optional chứa người dùng trong tổ chức nếu tìm thấy, hoặc rỗng nếu
	 *         không tìm thấy
	 */
	Optional<OrganizationUser> findByUserAndOrganization_Code(User user, String orgCode);

	/**
	 * Tìm người dùng đầu tiên trong tổ chức theo người dùng.
	 *
	 * @param user người dùng
	 * @return Optional chứa người dùng đầu tiên trong tổ chức nếu tìm thấy, hoặc
	 *         rỗng nếu không tìm thấy
	 */
	Optional<OrganizationUser> findFirstByUser(User user);

	/**
	 * Tìm tất cả người dùng trong tổ chức theo ID tổ chức và trạng thái.
	 *
	 * @param orgId                  ID của tổ chức
	 * @param organizationUserStatus trạng thái của người dùng trong tổ chức
	 * @return danh sách người dùng trong tổ chức phù hợp
	 */
	List<OrganizationUser> findByOrganization_OrganizationIdAndStatus(UUID orgId,
			OrganizationUserStatus organizationUserStatus);

	/**
	 * Tìm người dùng trong tổ chức theo ID tổ chức và ID người dùng.
	 *
	 * @param orgId  ID của tổ chức
	 * @param userId ID của người dùng
	 * @return Optional chứa người dùng trong tổ chức nếu tìm thấy, hoặc rỗng nếu
	 *         không tìm thấy
	 */
	Optional<OrganizationUser> findByOrganization_OrganizationIdAndUser_UserId(UUID orgId,
			@NotNull(message = "User ID is required") UUID userId);

	/** Tìm người dùng trong tổ chức theo ID tổ chức và mã vai trò. */
	Optional<OrganizationUser> findByOrganization_OrganizationIdAndRole_Code(UUID organizationId, String roleCode);

	/**
	 * Đếm số membership đang hoạt động của một vai trò trong tổ chức,
	 * dùng để bảo vệ rule "không vô hiệu hóa quản lý duy nhất còn lại".
	 *
	 * @param organizationId ID của tổ chức
	 * @param status         trạng thái membership
	 * @param roleCode       mã vai trò
	 * @return số membership khớp điều kiện
	 */
	long countByOrganization_OrganizationIdAndStatusAndRole_Code(
			UUID organizationId,
			OrganizationUserStatus status,
			String roleCode);

	/** Lấy tất cả người dùng theo vai trò. */
	List<OrganizationUser> findAllByRole_Code(String roleCode);

	/** Lấy tất cả người dùng theo vai trò trong tổ chức. */
	List<OrganizationUser> findAllByOrganization_OrganizationIdAndRole_Code(
			UUID organizationId,
			String roleCode);

	/**
	 * Lấy tất cả người dùng theo ID người dùng.
	 *
	 * @param userId ID của người dùng
	 * @return danh sách người dùng phù hợp
	 */
	List<OrganizationUser> findAllByUser_UserId(UUID userId);

	/**
	 * Tìm tất cả người dùng theo ID người dùng và trạng thái.
	 *
	 * @param userId                 ID của người dùng
	 * @param organizationUserStatus trạng thái của người dùng trong tổ chức
	 * @return danh sách người dùng phù hợp
	 */
	List<OrganizationUser> findByUser_UserIdAndStatus(UUID userId, OrganizationUserStatus status);

	/**
	 * Lấy tất cả người dùng trong các tổ chức.
	 *
	 * @param organizations danh sách tổ chức
	 * @return danh sách người dùng phù hợp
	 */
	List<OrganizationUser> findAllByOrganizationIn(List<Organization> organizations);

	/**
	 * Tìm người dùng đầu tiên trong tổ chức theo người dùng và loại tổ chức.
	 *
	 * @param user người dùng
	 * @param type loại tổ chức
	 * @return Optional chứa người dùng đầu tiên trong tổ chức nếu tìm thấy, hoặc
	 *         rỗng nếu không tìm thấy
	 */
	Optional<OrganizationUser> findFirstByUserAndOrganization_Type(User user, OrganizationType type);

	/**
	 * Kiểm tra xem người dùng có tồn tại trong tổ chức hay không.
	 *
	 * @param organization tổ chức
	 * @param user         người dùng
	 * @return true nếu người dùng tồn tại trong tổ chức, false nếu không
	 */
	boolean existsByOrganizationAndUser(Organization organization, User user);

	/**
	 * Tìm tất cả người dùng trong tổ chức theo ID tổ chức.
	 *
	 * @param organizationId ID của tổ chức
	 * @return danh sách người dùng trong tổ chức phù hợp
	 */
	List<OrganizationUser> findByOrganization_OrganizationId(UUID organizationId);

	@Query("""
			    SELECT DISTINCT ou.user
			    FROM OrganizationUser ou
			    JOIN ou.role r
			    JOIN RolePermission rp ON rp.role = r
			    JOIN Permission p ON p.permissionId = rp.permission.permissionId
			    WHERE ou.organization.organizationId = :organizationId
			      AND rp.enabled = true
			      AND p.resource = :resource
			      AND p.action = :action
			""")
	List<User> findUsersByPermission(
			@Param("organizationId") UUID organizationId,
			@Param("resource") String resource,
			@Param("action") String action);

	/**
	 * Tìm membership của user trong một organization cụ thể.
	 *
	 * @param userId         ID người dùng
	 * @param organizationId ID tổ chức
	 * @return OrganizationUser nếu user thuộc organization
	 */
	Optional<OrganizationUser> findByUser_UserIdAndOrganization_OrganizationId(
			UUID userId,
			UUID organizationId);
}
