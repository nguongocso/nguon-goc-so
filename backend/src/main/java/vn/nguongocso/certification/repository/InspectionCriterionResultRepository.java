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
 * Repository cho kết quả kiểm nghiệm của từng chỉ tiêu (InspectionCriterionResult).
 */
public interface InspectionCriterionResultRepository
    extends JpaRepository<InspectionCriterionResult, UUID> {
  /**
   * Tìm kết quả kiểm nghiệm theo ID của chỉ tiêu kiểm nghiệm.
   */
  Optional<InspectionCriterionResult> findByInspectionCriterion_Id(
      UUID criterionId);

  /**
   * Lấy danh sách kết quả kiểm nghiệm cho tất cả chỉ tiêu thuộc một yêu cầu kiểm nghiệm.
   */
  List<InspectionCriterionResult> findByInspectionCriterion_InspectionRequest_Id(
      UUID inspectionRequestId);

  /**
   * Kiểm tra xem tất cả chỉ tiêu của yêu cầu kiểm nghiệm đều đạt và còn hiệu lực hay không.
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
   * Lấy ngày hết hiệu lực sớm nhất của tất cả kết quả kiểm nghiệm thuộc một yêu cầu kiểm nghiệm.
   */
  @Query("""
      SELECT MIN(r.expiryDate)
      FROM InspectionCriterionResult r
      WHERE r.inspectionCriterion.inspectionRequest.id = :inspectionRequestId
      """)
  Optional<LocalDate> findEarliestExpiryDateByInspectionRequest(
      @Param("inspectionRequestId") UUID inspectionRequestId);

  /**
   * Đếm số lượng chỉ tiêu đạt và còn hiệu lực trong một yêu cầu kiểm nghiệm.
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
   */
  @Query("""
      SELECT COUNT(c)
      FROM InspectionCriterion c
      WHERE c.inspectionRequest.id = :inspectionRequestId
      """)
  int countTotalCriteria(
      @Param("inspectionRequestId") UUID inspectionRequestId);

  /**
   * Đếm số lượng chỉ tiêu không đạt (passed = false) cho từng yêu cầu kiểm nghiệm trong danh sách.
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

  /**
   * Lấy toàn bộ kết quả kiểm nghiệm thuộc mọi yêu cầu của một lô sản xuất (join sẵn chỉ tiêu để đối soát khi kích hoạt
   * tem QTN-21).
   */
  @Query("""
      SELECT r
      FROM InspectionCriterionResult r
      JOIN r.inspectionCriterion c
      WHERE c.inspectionRequest.productionLot.id = :lotId
      """)
  List<InspectionCriterionResult> findAllByProductionLotId(
      @Param("lotId") UUID lotId);
}