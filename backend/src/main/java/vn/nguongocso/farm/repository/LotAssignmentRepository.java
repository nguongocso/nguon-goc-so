package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.farm.entity.LotAssignment;

/**
 * Repository thao tác dữ liệu phân công thành viên vào lô sản xuất.
*/
public interface LotAssignmentRepository extends JpaRepository<LotAssignment, UUID> {
    /** Lấy phân công còn hiệu lực của thành viên trong tổ chức. */
    List<LotAssignment> findByUser_UserIdAndOrganization_OrganizationIdAndActiveTrue(
            UUID userId,
            UUID orgId);

    /** Lấy phân công còn hiệu lực của lô sản xuất. */
    List<LotAssignment> findByProductionLot_IdAndActiveTrue(UUID lotId);
}
