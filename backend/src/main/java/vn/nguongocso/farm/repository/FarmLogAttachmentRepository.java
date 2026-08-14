package vn.nguongocso.farm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.farm.entity.FarmLogAttachment;

import java.util.List;
import java.util.UUID;

/**
 * Repository thao tác dữ liệu tệp đính kèm của nhật ký nông trại.
 */
public interface FarmLogAttachmentRepository extends JpaRepository<FarmLogAttachment, UUID> {
    /**
     * Tìm tất cả các tệp đính kèm của nhật ký nông trại theo ID của nhật ký nông
     * trại.
     *
     * @param farmLogId ID của nhật ký nông trại.
     * @return Danh sách các tệp đính kèm liên quan đến nhật ký nông trại.
     */
    List<FarmLogAttachment> findByFarmLogId(UUID farmLogId);

    /**
     * Đếm số lượng tệp đính kèm của nhật ký nông trại theo ID của nhật ký nông
     * trại.
     *
     * @param farmLogId ID của nhật ký nông trại.
     * @return Số lượng tệp đính kèm liên quan đến nhật ký nông trại.
     */
    int countByFarmLogId(UUID farmLogId);

    /**
     * Tìm tất cả các tệp đính kèm của nhật ký nông trại theo danh sách ID của nhật
     * ký nông trại.
     *
     * @param farmLogIds Danh sách ID của nhật ký nông trại.
     * @return Danh sách các tệp đính kèm liên quan đến các nhật ký nông trại.
     */
    @Query("SELECT fla FROM FarmLogAttachment fla WHERE fla.farmLog.id IN :farmLogIds")
    List<FarmLogAttachment> findByFarmLogIdIn(@Param("farmLogIds") List<UUID> farmLogIds);
}
