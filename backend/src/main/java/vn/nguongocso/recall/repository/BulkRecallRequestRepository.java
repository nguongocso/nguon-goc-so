package vn.nguongocso.recall.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import vn.nguongocso.recall.entity.BulkRecallRequest;
import vn.nguongocso.recall.enums.BulkRecallRequestStatus;

/**
 * Repository quản lý các yêu cầu thu hồi hàng loạt (NCL-08-CN-011).
 */
@Repository
public interface BulkRecallRequestRepository extends JpaRepository<BulkRecallRequest, UUID> {

    /**
     * Tìm yêu cầu theo ID và tổ chức của lô sản xuất.
     */
    @Query("SELECT r FROM BulkRecallRequest r " +
           "WHERE r.id = :id AND r.productionLot.organization.organizationId = :organizationId")
    Optional<BulkRecallRequest> findByIdAndProductionLot_Organization_OrganizationId(
            @Param("id") UUID id, @Param("organizationId") UUID organizationId);

    /**
     * Tìm yêu cầu theo ID với pessimistic lock để tránh concurrent approval.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BulkRecallRequest r WHERE r.id = :id")
    Optional<BulkRecallRequest> findByIdWithLock(@Param("id") UUID id);

    /**
     * Kiểm tra đã có yêu cầu PENDING cho lô sản xuất chưa.
     */
    boolean existsByProductionLot_IdAndStatus(UUID productionLotId, BulkRecallRequestStatus status);

    /**
     * Lấy danh sách yêu cầu theo tổ chức với phân trang.
     */
    Page<BulkRecallRequest> findByProductionLot_Organization_OrganizationId(
            UUID organizationId, Pageable pageable);

    /**
     * Lấy danh sách yêu cầu theo tổ chức và trạng thái với phân trang.
     */
    Page<BulkRecallRequest> findByProductionLot_Organization_OrganizationIdAndStatus(
            UUID organizationId, BulkRecallRequestStatus status, Pageable pageable);
}
