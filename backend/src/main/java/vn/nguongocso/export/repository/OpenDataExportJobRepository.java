package vn.nguongocso.export.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.export.entity.OpenDataExportJob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository quản lý tác vụ xuất dữ liệu mở.
 */
@Repository
public interface OpenDataExportJobRepository extends JpaRepository<OpenDataExportJob, UUID> {

    /** Tìm danh sách các job đã hoàn thành hoặc thất bại trước một thời điểm để dọn dẹp file tạm. */
    List<OpenDataExportJob> findByCreatedAtBefore(LocalDateTime before);

    /** Xóa các job cũ hơn thời gian quy định. */
    @Modifying
    @Query("DELETE FROM OpenDataExportJob j WHERE j.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
