package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.farm.entity.LotAssignment;

/**
 * Repository thao tác dữ liệu phân công thành viên vào lô sản xuất.
 *
 * <p>
 * Ghi chú (D-4): luồng chuyển giao phân công lô khi vô hiệu hóa thành viên
 * đã tạm gỡ bỏ vì hệ thống chưa có phân quyền ghi sự kiện theo lô.
 * Repository chỉ giữ các truy vấn phục vụ tra cứu/báo cáo phân công.
 * </p>
 */
public interface LotAssignmentRepository extends JpaRepository<LotAssignment, UUID> {

    /**
     * Lấy toàn bộ phân công còn hiệu lực của một thành viên trong tổ chức.
     *
     * @param userId ID của thành viên được phân công
     * @param orgId  ID của tổ chức
     * @return danh sách phân công còn hiệu lực
     */
    List<LotAssignment> findByUser_UserIdAndOrganization_OrganizationIdAndActiveTrue(
            UUID userId,
            UUID orgId);
}
