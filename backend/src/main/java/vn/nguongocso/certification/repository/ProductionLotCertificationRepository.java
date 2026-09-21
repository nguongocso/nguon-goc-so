package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.ProductionLotCertification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể chứng nhận gắn lô sản xuất
 * (ProductionLotCertification).
 */
public interface ProductionLotCertificationRepository
                extends JpaRepository<ProductionLotCertification, UUID> {
        /**
         * Tìm tất cả chứng nhận gắn vào lô sản xuất theo ID lô sản xuất.
         */
        List<ProductionLotCertification> findByProductionLotId(UUID lotId);

        /**
         * Tìm chứng nhận gắn vào lô sản xuất theo ID lô và ID chứng nhận.
         */
        Optional<ProductionLotCertification> findByProductionLotIdAndCertificationId(
                        UUID lotId,
                        UUID certId);

        /**
         * Kiểm tra sự tồn tại của chứng nhận gắn vào lô sản xuất theo ID lô và ID chứng
         * nhận.
         */
        boolean existsByProductionLotIdAndCertificationId(
                        UUID lotId,
                        UUID certId);

        /**
         * Xóa chứng nhận gắn vào lô sản xuất theo ID lô và ID chứng nhận.
         */
        void deleteByProductionLotIdAndCertificationId(
                        UUID lotId,
                        UUID certId);

        /**
         * Tìm tất cả chứng nhận gắn vào lô sản xuất theo danh sách ID lô sản xuất.
         */
        @Query("""
                        SELECT plc FROM ProductionLotCertification plc
                        JOIN FETCH plc.certification
                        WHERE plc.productionLot.id IN :productionLotIds
                        """)
        List<ProductionLotCertification> findByProductionLotIdIn(
                        @Param("productionLotIds") List<UUID> productionLotIds);
}
