package vn.nguongocso.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.organization.entity.UserAreaAssignment;

/** Repository truy vấn bản ghi gán địa bàn cho tài khoản. */
@Repository
public interface UserAreaAssignmentRepository extends JpaRepository<UserAreaAssignment, UUID> {
    /** Kiểm tra người dùng đã được gán đơn vị hành chính hay chưa. */
    boolean existsByUser_UserIdAndUnit_Id(UUID userId, UUID unitId);

    /** Lấy toàn bộ địa bàn được gán cho người dùng sắp xếp theo thời gian gán mới nhất. */
    List<UserAreaAssignment> findAllByUser_UserIdOrderByAssignedAtDesc(UUID userId);

    /** Tìm bản ghi gán địa bàn của người dùng theo đơn vị hành chính. */
    Optional<UserAreaAssignment> findFirstByUser_UserIdAndUnit_Id(UUID userId, UUID unitId);
}
