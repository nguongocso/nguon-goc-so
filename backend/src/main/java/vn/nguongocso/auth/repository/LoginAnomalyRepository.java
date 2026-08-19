package vn.nguongocso.auth.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.enums.AnomalyStatus;

/**
 * Repository quản lý các bản ghi bất thường đăng nhập.
 */
@Repository
public interface LoginAnomalyRepository extends JpaRepository<LoginAnomaly, UUID> {
    
    /**
     * Lấy danh sách bất thường của một tổ chức, sắp xếp theo thời gian phát hiện mới nhất trước.
     */
    Page<LoginAnomaly> findByOrganization_OrganizationIdOrderByDetectedAtDesc(
        UUID organizationId,
        Pageable pageable
    );
    
    /**
     * Lấy danh sách tất cả bất thường (toàn nền tảng), sắp xếp mới nhất trước.
     */
    Page<LoginAnomaly> findAllByOrderByDetectedAtDesc(
        Pageable pageable
    );
    
    /**
     * Lấy danh sách bất thường của một người dùng cụ thể.
     */
    Page<LoginAnomaly> findByUser_UserIdOrderByDetectedAtDesc(
        UUID userId,
        Pageable pageable
    );

    List<LoginAnomaly> findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
        UUID userId,
        java.time.OffsetDateTime detectedAtAfter
    );
    
    /**
     * Đếm số lượng bất thường chưa giải quyết (status = OPEN) của một tổ chức.
     */
    long countByOrganization_OrganizationIdAndStatus(
        UUID organizationId,
        AnomalyStatus status
    );
}
