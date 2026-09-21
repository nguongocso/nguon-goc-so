package vn.nguongocso.certification.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể yêu cầu kiểm nghiệm (InspectionRequest).
 */
public interface InspectionRequestRepository
    extends JpaRepository<InspectionRequest, UUID> {
  /**
   * Lấy danh sách yêu cầu kiểm nghiệm theo ID lô sản xuất, sắp xếp theo ngày tạo giảm dần.
   */
  List<InspectionRequest> findByProductionLot_IdOrderByCreatedAtDesc(
      UUID productionLotId);

  /**
   * Kiểm tra sự tồn tại của yêu cầu kiểm nghiệm theo ID lô sản xuất và trạng thái.
   */
  boolean existsByProductionLot_IdAndStatus(
      UUID productionLotId,
      InspectionRequestStatus status);

  /**
   * Lấy chi tiết yêu cầu kiểm nghiệm theo ID, bao gồm danh sách chỉ tiêu và tiêu chuẩn.
   */
  @Query("""
      SELECT DISTINCT ir
      FROM InspectionRequest ir
      LEFT JOIN FETCH ir.criteria c
      LEFT JOIN FETCH c.standard
      WHERE ir.id = :id
      """)
  Optional<InspectionRequest> findDetailById(
      @Param("id") UUID id);

  /**
   * Tìm yêu cầu kiểm nghiệm theo ID và ID tổ chức sở hữu lô để đảm bảo cách ly dữ liệu.
   */
  @Query("""
      SELECT ir
      FROM InspectionRequest ir
      JOIN FETCH ir.productionLot pl
      JOIN FETCH pl.organization
      LEFT JOIN FETCH pl.productCategory
      WHERE ir.id = :id AND pl.organization.organizationId = :organizationId
      """)
  Optional<InspectionRequest> findByIdAndProductionLot_Organization_OrganizationId(
      @Param("id") UUID id,
      @Param("organizationId") UUID organizationId);

  /**
   * Khóa bi quan (PESSIMISTIC_WRITE) yêu cầu kiểm nghiệm kết hợp kiểm tra tổ chức sở hữu.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT ir
      FROM InspectionRequest ir
      JOIN FETCH ir.productionLot pl
      JOIN FETCH pl.organization
      LEFT JOIN FETCH pl.productCategory
      WHERE ir.id = :id AND pl.organization.organizationId = :organizationId
      """)
  Optional<InspectionRequest> findByIdAndOrganizationIdForUpdate(
      @Param("id") UUID id,
      @Param("organizationId") UUID organizationId);

  /**
   * Khóa bi quan yêu cầu kiểm nghiệm chứa chỉ tiêu được chọn, đồng thời kiểm tra tổ chức sở hữu.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT ir
      FROM InspectionRequest ir
      JOIN ir.criteria criterion
      JOIN FETCH ir.productionLot pl
      JOIN FETCH pl.organization
      LEFT JOIN FETCH pl.productCategory
      WHERE criterion.id = :criterionId
        AND pl.organization.organizationId = :organizationId
      """)
  Optional<InspectionRequest> findByCriterionIdAndOrganizationIdForUpdate(
      @Param("criterionId") UUID criterionId,
      @Param("organizationId") UUID organizationId);

  /**
   * Khóa bi quan (PESSIMISTIC_WRITE) yêu cầu kiểm nghiệm theo ID.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT ir
      FROM InspectionRequest ir
      JOIN FETCH ir.productionLot pl
      JOIN FETCH pl.organization
      LEFT JOIN FETCH pl.productCategory
      WHERE ir.id = :id
      """)
  Optional<InspectionRequest> findByIdForUpdate(@Param("id") UUID id);

  /**
   * Lấy danh sách yêu cầu kiểm nghiệm theo ID lô sản xuất và trạng thái.
   */
  List<InspectionRequest> findByProductionLot_IdAndStatus(
      UUID productionLotId,
      InspectionRequestStatus status);

  /**
   * Lấy danh sách yêu cầu kiểm nghiệm theo ID lô sản xuất có phân trang.
   */
  Page<InspectionRequest> findByProductionLot_Id(
      UUID productionLotId,
      Pageable pageable);

  /**
   * Lấy danh sách yêu cầu kiểm nghiệm theo ID lô sản xuất và trạng thái có phân trang.
   */
  Page<InspectionRequest> findByProductionLot_IdAndStatus(
      UUID productionLotId,
      InspectionRequestStatus status,
      Pageable pageable);

  /**
   * Lấy danh sách yêu cầu kiểm nghiệm theo trạng thái có phân trang.
   */
  Page<InspectionRequest> findByStatus(
      InspectionRequestStatus status,
      Pageable pageable);

  /**
   * Lấy tất cả yêu cầu kiểm nghiệm có phân trang.
   */
  Page<InspectionRequest> findAll(Pageable pageable);

  /**
   * Lấy danh sách yêu cầu kiểm nghiệm theo tổ chức sở hữu lô sản xuất có phân trang.
   */
  Page<InspectionRequest> findByProductionLot_Organization_OrganizationId(
      UUID organizationId,
      Pageable pageable);

  /**
   * Lấy danh sách yêu cầu kiểm nghiệm theo tổ chức sở hữu lô sản xuất và trạng thái có phân trang.
   */
  Page<InspectionRequest> findByProductionLot_Organization_OrganizationIdAndStatus(
      UUID organizationId,
      InspectionRequestStatus status,
      Pageable pageable);

  /**
   * Tìm danh sách yêu cầu kiểm nghiệm theo danh sách lô sản xuất và trạng thái (NCL-07-CN-006).
   */
  @Query("""
      SELECT ir FROM InspectionRequest ir
      WHERE ir.productionLot.id IN :lotIds AND ir.status = :status
      """)
  List<InspectionRequest> findByProductionLotIdInAndStatus(
      @Param("lotIds") Collection<UUID> lotIds,
      @Param("status") InspectionRequestStatus status);
}
