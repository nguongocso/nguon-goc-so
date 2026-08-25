package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.InspectionCriterionResult;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể InspectionCriterionResult.
 *
 * Quản lý kết quả kiểm nghiệm tương ứng với từng chỉ tiêu
 * trong một yêu cầu kiểm nghiệm.
 */
public interface InspectionCriterionResultRepository
        extends JpaRepository<InspectionCriterionResult, UUID> {

    /**
     * Tìm kết quả kiểm nghiệm theo ID của chỉ tiêu kiểm nghiệm.
     *
     * Entity InspectionCriterionResult có field:
     *
     *     private InspectionCriterion inspectionCriterion;
     *
     * Vì vậy Spring Data JPA phải sử dụng:
     *
     *     findByInspectionCriterion_Id(...)
     *
     * thay vì:
     *
     *     findByCriterion_Id(...)
     *
     * Mỗi chỉ tiêu chỉ có tối đa một kết quả do database
     * có unique constraint trên inspection_criterion_id.
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @return Optional chứa kết quả kiểm nghiệm nếu tồn tại.
     */
    Optional<InspectionCriterionResult> findByInspectionCriterion_Id(
            UUID criterionId);

    /**
     * Lấy danh sách kết quả kiểm nghiệm cho tất cả chỉ tiêu
     * thuộc một yêu cầu kiểm nghiệm.
     *
     * Quan hệ:
     *
     * InspectionCriterionResult
     *      -> inspectionCriterion
     *          -> inspectionRequest
     *              -> id
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @return Danh sách kết quả kiểm nghiệm.
     */
    List<InspectionCriterionResult> findByInspectionCriterion_InspectionRequest_Id(
            UUID inspectionRequestId);

    /**
     * Kiểm tra xem tất cả chỉ tiêu của yêu cầu kiểm nghiệm
     * đều đạt và còn hiệu lực hay không.
     *
     * Một chỉ tiêu được xem là đạt và hợp lệ khi:
     *
     * - Có kết quả kiểm nghiệm.
     * - passed = true.
     * - expiryDate >= ngày hiện tại.
     *
     * LEFT JOIN được sử dụng để vẫn lấy các chỉ tiêu chưa
     * có kết quả. Khi đó r sẽ là NULL và COUNT(r) sẽ nhỏ
     * hơn COUNT(c), kết quả trả về false.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @param today Ngày hiện tại để kiểm tra hiệu lực.
     * @return true nếu tất cả chỉ tiêu đều đạt và còn hiệu lực,
     *         false nếu còn chỉ tiêu chưa đạt/chưa có kết quả/hết hạn.
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
     * Lấy ngày hết hiệu lực sớm nhất của tất cả kết quả kiểm nghiệm
     * thuộc một yêu cầu kiểm nghiệm.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @return Optional chứa ngày hết hiệu lực sớm nhất nếu tồn tại
     *         kết quả kiểm nghiệm.
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
     * Điều kiện:
     *
     * - Kết quả thuộc yêu cầu kiểm nghiệm.
     * - passed = true.
     * - expiryDate >= today.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @param today Ngày hiện tại để kiểm tra hiệu lực.
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
     * Đếm tổng số lượng chỉ tiêu trong một yêu cầu kiểm nghiệm.
     *
     * @param inspectionRequestId ID của yêu cầu kiểm nghiệm.
     * @return Tổng số lượng chỉ tiêu.
     */
    @Query("""
        SELECT COUNT(c)
        FROM InspectionCriterion c
        WHERE c.inspectionRequest.id = :inspectionRequestId
    """)
    int countTotalCriteria(
            @Param("inspectionRequestId") UUID inspectionRequestId);

    /**
     * Đếm số lượng chỉ tiêu KHÔNG ĐẠT (passed = false) cho từng
     * yêu cầu kiểm nghiệm trong một danh sách, bằng một truy vấn
     * duy nhất để tránh N+1 khi map trang danh sách yêu cầu.
     *
     * Lưu ý: chỉ đếm kết quả có passed = false. Các kết quả hết hạn
     * nhưng passed = true KHÔNG được tính là không đạt.
     *
     * @param requestIds Danh sách ID yêu cầu kiểm nghiệm.
     * @return Danh sách mảng [requestId, số chỉ tiêu không đạt].
     */
    @Query("""
        SELECT r.inspectionCriterion.inspectionRequest.id, COUNT(r)
        FROM InspectionCriterionResult r
        WHERE r.inspectionCriterion.inspectionRequest.id IN :requestIds
          AND r.passed = false
        GROUP BY r.inspectionCriterion.inspectionRequest.id
    """)
    List<Object[]> countFailedCriteriaByRequestIds(
            @Param("requestIds") Collection<UUID> requestIds);
}