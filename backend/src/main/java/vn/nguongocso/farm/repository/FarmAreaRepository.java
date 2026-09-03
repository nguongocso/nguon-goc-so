package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.farm.entity.FarmArea;

import java.util.Optional;

/**
 * Repository thao tác dữ liệu vùng trồng.
 */
public interface FarmAreaRepository extends JpaRepository<FarmArea, UUID> {
	/**
	 * Tìm tất cả các vùng trồng theo ID tổ chức.
	 *
	 * @param organizationId ID của tổ chức.
	 * @return Danh sách các vùng trồng thuộc tổ chức.
	 */
	List<FarmArea> findByOrganization_OrganizationId(UUID organizationId);

	/**
	 * Tìm tất cả các vùng trồng đang hoạt động (isActive = true) theo ID tổ chức.
	 */
	List<FarmArea> findByOrganization_OrganizationIdAndIsActiveTrue(UUID organizationId);

	/**
	 * Tìm vùng trồng theo ID và ID tổ chức (đảm bảo Tenant Isolation).
	 */
	Optional<FarmArea> findByIdAndOrganization_OrganizationId(UUID id, UUID organizationId);
}
