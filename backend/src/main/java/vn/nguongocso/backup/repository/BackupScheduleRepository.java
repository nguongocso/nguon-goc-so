package vn.nguongocso.backup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.backup.entity.BackupSchedule;

import java.util.Optional;

/**
 * Repository cho thực thể BackupSchedule.
 */
@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Integer> {
    // Tìm cấu hình lịch đang hoạt động
    Optional<BackupSchedule> findFirstByIsActiveTrue();
}
