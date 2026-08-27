package vn.nguongocso.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;

/**
 * Repository truy vấn danh mục đơn vị hành chính.
 */
@Repository
public interface AdministrativeUnitRepository extends JpaRepository<AdministrativeUnit, UUID> {

	Optional<AdministrativeUnit> findByCode(String code);

	boolean existsByCode(String code);

	List<AdministrativeUnit> findAllByLevelOrderByNameAsc(AdministrativeUnitLevel level);

	List<AdministrativeUnit> findAllByLevelAndActiveTrueOrderByNameAsc(AdministrativeUnitLevel level);

	List<AdministrativeUnit> findAllByProvinceIdAndLevelOrderByNameAsc(UUID provinceId, AdministrativeUnitLevel level);
}
