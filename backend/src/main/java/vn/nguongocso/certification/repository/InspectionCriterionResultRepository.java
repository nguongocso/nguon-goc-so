package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.InspectionCriterionResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể InspectionCriterionResult.
 */
public interface InspectionCriterionResultRepository
        extends JpaRepository<InspectionCriterionResult, UUID> {

    /**
     * Tìm kết quả kiểm nghiệm theo ID chỉ tiêu kiểm nghiệm.
     * Mỗi chỉ tiêu chỉ có tối đa một kết quả.
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @return Optional chứa kết quả kiểm nghiệm nếu tồn tại.
     */
    Optional<InspectionCriterionResult> findByCriterion_Id(UUID criterionId);

    /**
     * Lấy danh sách kết quả kiểm nghiệm cho tất cả chỉ tiêu của một yêu cầu kiểm nghiệm.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @return Danh sách kết quả kiểm nghiệm.
     */
    List<InspectionCriterionResult> findByCriterion_InspectionRequest_Id(
            UUID inspectionRequestId);

    /**
     * Kiểm tra xem tất cả chỉ tiêu của yêu cầu kiểm nghiệm đều đạt và còn hiệu lực hay không.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @param today               Ngày hiện tại để kiểm tra hiệu lực.
     * @return true nếu tất cả đạt và còn hiệu lực, ngược lại là false.
     */
    @Query("""
        SELECT COUNT(r) = COUNT(c)
        FROM InspectionCriterion c
        LEFT JOIN InspectionCriterionResult r
            ON c.id = r.inspectionCriterion.id
            AND r.passed = true
            AND r.expiryDate >= :today
        WHERE c.inspectionRequest.id = :inspectionRequestId
    """)
    boolean areAllCriteriaPassedAndValid(
            @Param("inspectionRequestId") UUID inspectionRequestId,
            @Param("today") LocalDate today);

    /**
     * Lấy ngày hết hiệu lực sớm nhất của tất cả kết quả kiểm nghiệm trong yêu cầu.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @return Optional chứa ngày hết hiệu lực sớm nhất nếu tồn tại.
     */
    @Query("""
        SELECT MIN(r.expiryDate)
        FROM InspectionCriterionResult r
        WHERE r.inspectionCriterion.inspectionRequest.id = :inspectionRequestId
    """)
    Optional<LocalDate> findEarliestExpiryDateByInspectionRequest(
            @Param("inspectionRequestId") UUID inspectionRequestId);

    /**
     * Đếm số lượng chỉ tiêu đạt và còn hiệu lực.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @param today               Ngày hiện tại để kiểm tra hiệu lực.
     * @return Số lượng chỉ tiêu đạt và còn hiệu lực.
     */
    @Query("""
        SELECT COUNT(r)
        FROM InspectionCriterionResult r
        WHERE r.inspectionCriterion.inspectionRequest.id = :inspectionRequestId
        AND r.passed = true
        AND r.expiryDate >= :today
    """)
    int countPassedAndValidCriteria(
            @Param("inspectionRequestId") UUID inspectionRequestId,
            @Param("today") LocalDate today);

    /**
     * Đếm tổng số lượng chỉ tiêu trong yêu cầu kiểm nghiệm.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @return Tổng số lượng chỉ tiêu.
     */
    @Query("""
        SELECT COUNT(c)
        FROM InspectionCriterion c
        WHERE c.inspectionRequest.id = :inspectionRequestId
    """)
    int countTotalCriteria(@Param("inspectionRequestId") UUID inspectionRequestId);
}
