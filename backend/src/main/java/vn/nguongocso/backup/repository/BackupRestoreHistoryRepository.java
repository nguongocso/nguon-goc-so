package vn.nguongocso.backup.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.backup.entity.BackupRestoreHistory;
import vn.nguongocso.backup.enums.BackupOperationType;
import vn.nguongocso.backup.enums.BackupStatus;

import java.util.List;

/**
 * Repository cho thực thể BackupRestoreHistory.
 */
@Repository
public interface BackupRestoreHistoryRepository extends JpaRepository<BackupRestoreHistory, Integer> {
        // Lọc lịch sử hoạt động theo loại thao tác và trạng thái (hỗ trợ phân trang)
        @Query("SELECT h FROM BackupRestoreHistory h " +
                        "LEFT JOIN FETCH h.createdBy " +
                        "LEFT JOIN FETCH h.reference " +
                        "WHERE (:operationType IS NULL OR h.operationType = :operationType) " +
                        "AND (:status IS NULL OR h.status = :status)")

        // Sử dụng LEFT JOIN FETCH để tránh vấn đề N+1 khi truy xuất thông tin người tạo
        // và bản tham chiếu
        Page<BackupRestoreHistory> findHistoryWithFilters(
                        @Param("operationType") BackupOperationType operationType,
                        @Param("status") BackupStatus status,
                        Pageable pageable);

        // Kiểm tra xem có tiến trình nào đang chạy ngầm hay không (để lock tài nguyên)
        boolean existsByStatus(BackupStatus status);

        // Lấy các bản sao lưu thành công để phục vụ dọn dẹp các bản sao lưu cũ vượt quá
        // giới hạn
        List<BackupRestoreHistory> findByOperationTypeAndStatusOrderByCreatedAtDesc(
                        BackupOperationType operationType,
                        BackupStatus status);
}
