package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.farm.entity.FarmArea;

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
}
