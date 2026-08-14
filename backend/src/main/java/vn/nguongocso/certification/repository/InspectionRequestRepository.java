package vn.nguongocso.certification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;

import java.util.List;
import java.util.UUID;

/**
 * Repository cho thực thể InspectionRequest.
 */
public interface InspectionRequestRepository
        extends JpaRepository<InspectionRequest, UUID> {

    /**
     * Lấy danh sách yêu cầu kiểm nghiệm theo ID lô sản xuất, sắp xếp theo ngày tạo
     * giảm dần.
     *
     * @param productionLotId ID của lô sản xuất.
     * @return Danh sách yêu cầu kiểm nghiệm.
     */
    List<InspectionRequest> findByProductionLot_IdOrderByCreatedAtDesc(
            UUID productionLotId);

    /**
     * Kiểm tra sự tồn tại của yêu cầu kiểm nghiệm theo ID lô sản xuất và trạng
     * thái.
     *
     * @param productionLotId ID của lô sản xuất.
     * @param status          Trạng thái của yêu cầu kiểm nghiệm.
     * @return true nếu tồn tại, ngược lại là false.
     */
    boolean existsByProductionLot_IdAndStatus(
            UUID productionLotId,
            InspectionRequestStatus status);

    /**
     * Lấy chi tiết yêu cầu kiểm nghiệm theo ID, bao gồm danh sách chỉ tiêu và thông
     * tin tiêu chuẩn.
     *
     * @param id ID của yêu cầu kiểm nghiệm.
     * @return Optional chứa yêu cầu kiểm nghiệm nếu tìm thấy, ngược lại là Optional
     *         rỗng.
     */
    @Query("""
                SELECT DISTINCT ir
                FROM InspectionRequest ir
                LEFT JOIN FETCH ir.criteria c
                LEFT JOIN FETCH c.standard
                WHERE ir.id = :id
            """)
    java.util.Optional<InspectionRequest> findDetailById(
            @Param("id") UUID id);

    /**
     * Lấy danh sách yêu cầu kiểm nghiệm theo ID lô sản xuất và trạng thái.
     * @param productionLotId
     * @param status
     * @return
     */
    List<InspectionRequest> findByProductionLot_IdAndStatus(
        UUID productionLotId,
        InspectionRequestStatus status);

    Page<InspectionRequest> findByProductionLot_Id(UUID productionLotId, Pageable pageable);

    Page<InspectionRequest> findByProductionLot_IdAndStatus(
            UUID productionLotId,
            InspectionRequestStatus status,
            Pageable pageable);

    Page<InspectionRequest> findByStatus(
            InspectionRequestStatus status,
            Pageable pageable);

    Page<InspectionRequest> findAll(Pageable pageable);
}