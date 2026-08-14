package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.ProductionLotCertification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể ProductionLotCertification.
 */
public interface ProductionLotCertificationRepository extends JpaRepository<ProductionLotCertification, UUID> {
    /**
     * Tìm tất cả chứng nhận gắn vào lô sản xuất theo ID lô sản xuất.
     *
     * @param lotId ID của lô sản xuất.
     * @return Danh sách chứng nhận gắn vào lô sản xuất.
     */
    List<ProductionLotCertification> findByProductionLotId(UUID lotId);

    /**
     * Tìm chứng nhận gắn vào lô sản xuất theo ID lô sản xuất và ID chứng nhận.
     *
     * @param lotId  ID của lô sản xuất.
     * @param certId ID của chứng nhận.
     * @return Optional chứa chứng nhận gắn vào lô sản xuất nếu tìm thấy, ngược lại
     *         là Optional rỗng.
     */
    Optional<ProductionLotCertification> findByProductionLotIdAndCertificationId(UUID lotId, UUID certId);

    /**
     * Kiểm tra sự tồn tại của chứng nhận gắn vào lô sản xuất theo ID lô sản xuất và
     * ID chứng nhận.
     *
     * @param lotId  ID của lô sản xuất.
     * @param certId ID của chứng nhận.
     * @return true nếu tồn tại, ngược lại là false.
     */
    boolean existsByProductionLotIdAndCertificationId(UUID lotId, UUID certId);

    /**
     * Xóa chứng nhận gắn vào lô sản xuất theo ID lô sản xuất và ID chứng nhận.
     *
     * @param lotId  ID của lô sản xuất.
     * @param certId ID của chứng nhận.
     */
    void deleteByProductionLotIdAndCertificationId(UUID lotId, UUID certId);

    /**
     * Tìm tất cả chứng nhận gắn vào lô sản xuất theo danh sách ID lô sản xuất.
     *
     * @param productionLotIds Danh sách ID của các lô sản xuất.
     * @return Danh sách chứng nhận gắn vào các lô sản xuất.
     */
    @Query("SELECT plc FROM ProductionLotCertification plc " +
            "JOIN FETCH plc.certification " +
            "WHERE plc.productionLot.id IN :productionLotIds")
    List<ProductionLotCertification> findByProductionLotIdIn(@Param("productionLotIds") List<UUID> productionLotIds);
}
