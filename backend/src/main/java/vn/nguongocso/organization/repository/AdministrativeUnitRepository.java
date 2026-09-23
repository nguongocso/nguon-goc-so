package vn.nguongocso.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;

/** Repository truy vấn danh mục đơn vị hành chính. */
@Repository
public interface AdministrativeUnitRepository extends JpaRepository<AdministrativeUnit, UUID> {
    /** Tìm đơn vị hành chính theo mã. */
    Optional<AdministrativeUnit> findByCode(String code);

    /** Kiểm tra mã đơn vị hành chính đã tồn tại chưa. */
    boolean existsByCode(String code);

    /** Lấy danh sách đơn vị theo cấp hành chính. */
    List<AdministrativeUnit> findAllByLevelOrderByNameAsc(AdministrativeUnitLevel level);

    /** Lấy danh sách đơn vị đang hoạt động theo cấp hành chính. */
    List<AdministrativeUnit> findAllByLevelAndActiveTrueOrderByNameAsc(AdministrativeUnitLevel level);

    /** Lấy danh sách đơn vị theo tỉnh và cấp hành chính. */
    List<AdministrativeUnit> findAllByProvinceIdAndLevelOrderByNameAsc(UUID provinceId, AdministrativeUnitLevel level);
}
