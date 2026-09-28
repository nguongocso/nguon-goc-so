package vn.nguongocso.auth.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.auth.entity.SuspiciousCase;
import vn.nguongocso.auth.enums.AnomalyStatus;

/* 
 * Repository chứa các phương thức truy vấn đến bảng suspicious_cases
 */
@Repository
public interface SuspiciousCaseRepository extends JpaRepository<SuspiciousCase, UUID> {
    /*
     * Tìm tất cả các trường hợp nghi vấn theo tổ chức và phân trang
     */
    Page<SuspiciousCase> findByOrganization_OrganizationIdOrderByLastDetectedAtDesc(
            UUID organizationId,
            Pageable pageable);

    /**
     * Tìm tất cả các trường hợp nghi vấn và phân trang
     */
    Page<SuspiciousCase> findAllByOrderByLastDetectedAtDesc(Pageable pageable);

    /**
     * Tìm tất cả các trường hợp nghi vấn theo người dùng và phân trang
     */
    List<SuspiciousCase> findByUser_UserIdOrderByLastDetectedAtDesc(UUID userId);

    /**
     * Tìm tất cả các trường hợp nghi vấn theo người dùng và trạng thái và phân trang
     */
    List<SuspiciousCase> findByUser_UserIdAndStatusOrderByLastDetectedAtDesc(
            UUID userId,
            AnomalyStatus status);

    /**
     * Tìm tất cả các trường hợp nghi vấn theo trạng thái và phân trang
     */
    Page<SuspiciousCase> findByStatusOrderByLastDetectedAtDesc(
            AnomalyStatus status,
            Pageable pageable);

    /**
     * Tìm tất cả các trường hợp nghi vấn theo người dùng và thời gian và phân trang
     */
    List<SuspiciousCase> findByUser_UserIdAndLastDetectedAtAfter(
            UUID userId,
            OffsetDateTime threshold);

    /**
     * Kiểm tra xem có tồn tại trường hợp nghi vấn theo người dùng và trạng thái và thời gian hay không
     */
    boolean existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
            UUID userId,
            AnomalyStatus status,
            OffsetDateTime threshold);
}
