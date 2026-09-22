package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.FarmArea;
/**
 * Repository thao tác dữ liệu vùng trồng.
*/
public interface FarmAreaRepository extends JpaRepository<FarmArea, UUID> {
    /** Tìm tất cả các vùng trồng theo ID tổ chức. */
    List<FarmArea> findByOrganization_OrganizationId(UUID organizationId);

    /** Tìm tất cả các vùng trồng đang hoạt động theo ID tổ chức. */
    List<FarmArea> findByOrganization_OrganizationIdAndIsActiveTrue(UUID organizationId);

    /** Tìm vùng trồng theo ID và ID tổ chức. */
    Optional<FarmArea> findByIdAndOrganization_OrganizationId(UUID id, UUID organizationId);

    /** Tìm và khóa vùng trồng để cập nhật ranh giới. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select farmArea from FarmArea farmArea where farmArea.id = :id")
    Optional<FarmArea> findByIdForBoundaryUpdate(@Param("id") UUID id);
}
