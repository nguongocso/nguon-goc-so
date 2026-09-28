package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.FarmLogAttachment;

/**
 * Repository thao tác dữ liệu tệp đính kèm của nhật ký nông trại.
*/
public interface FarmLogAttachmentRepository extends JpaRepository<FarmLogAttachment, UUID> {
    /** Tìm tất cả các tệp đính kèm theo ID nhật ký nông trại. */
    List<FarmLogAttachment> findByFarmLogId(UUID farmLogId);

    /** Đếm số lượng tệp đính kèm theo ID nhật ký nông trại. */
    int countByFarmLogId(UUID farmLogId);

    /** Tìm tất cả các tệp đính kèm theo danh sách ID nhật ký nông trại. */
    @Query("SELECT fla FROM FarmLogAttachment fla WHERE fla.farmLog.id IN :farmLogIds")
    List<FarmLogAttachment> findByFarmLogIdIn(@Param("farmLogIds") List<UUID> farmLogIds);
}
