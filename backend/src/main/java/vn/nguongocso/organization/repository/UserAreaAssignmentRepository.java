package vn.nguongocso.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.organization.entity.UserAreaAssignment;

/**
 * Repository truy vấn bản ghi gán địa bàn cho tài khoản.
 */
@Repository
public interface UserAreaAssignmentRepository extends JpaRepository<UserAreaAssignment, UUID> {

	boolean existsByUser_UserIdAndUnit_Id(UUID userId, UUID unitId);

	List<UserAreaAssignment> findAllByUser_UserIdOrderByAssignedAtDesc(UUID userId);

	Optional<UserAreaAssignment> findFirstByUser_UserIdAndUnit_Id(UUID userId, UUID unitId);
}
