package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.LotAssignment;
import vn.nguongocso.farm.enums.ProductionLotStatus;

/**
 * Repository thao tác dữ liệu phân công thành viên vào lô sản xuất.
 */
public interface LotAssignmentRepository extends JpaRepository<LotAssignment, UUID> {

    /**
     * Lấy các phân công còn hiệu lực của một thành viên trong tổ chức
     * mà lô tương ứng đang ở một trong các trạng thái cho trước
     * (dùng với danh sách trạng thái "lô chưa hoàn thành").
     *
     * @param userId   ID của thành viên được phân công
     * @param orgId    ID của tổ chức
     * @param statuses danh sách trạng thái lô cần lấy
     * @return danh sách phân công còn hiệu lực khớp điều kiện
     */
    @Query("""
            SELECT la
            FROM LotAssignment la
            JOIN FETCH la.productionLot pl
            WHERE la.user.userId = :userId
              AND la.organization.organizationId = :orgId
              AND la.active = true
              AND pl.status IN :statuses
            """)
    List<LotAssignment> findActiveByUserOrganizationAndLotStatusIn(
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId,
            @Param("statuses") List<ProductionLotStatus> statuses);

    /**
     * Lấy toàn bộ phân công còn hiệu lực của một thành viên trong tổ chức,
     * dùng khi chuyển giao sang người thay thế (không lọc trạng thái lô
     * vì việc chuyển giao phải bao trùm mọi phân công đang mở).
     *
     * @param userId ID của thành viên được phân công
     * @param orgId  ID của tổ chức
     * @return danh sách phân công còn hiệu lực
     */
    List<LotAssignment> findByUser_UserIdAndOrganization_OrganizationIdAndActiveTrue(
            UUID userId,
            UUID orgId);
}
