package vn.nguongocso.farm.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;

/**
 * Repository thao tác dữ liệu lô sản xuất.
*/
public interface ProductionLotRepository extends JpaRepository<ProductionLot, UUID> {
    /** Tìm các lô sản xuất theo ID tổ chức. */
    List<ProductionLot> findByOrganization_OrganizationId(UUID organizationId);

    /** Tìm các lô sản xuất theo ID tổ chức và trạng thái. */
    List<ProductionLot> findByOrganization_OrganizationIdAndStatus(UUID organizationId, ProductionLotStatus status);

    /** Tìm các lô sản xuất theo tổ chức và danh sách trạng thái. */
    List<ProductionLot> findByOrganization_OrganizationIdAndStatusIn(UUID organizationId, Collection<ProductionLotStatus> statuses);

    /** Tìm các lô sản xuất theo trạng thái. */
    List<ProductionLot> findByStatus(ProductionLotStatus status);

    /** Tìm các lô sản xuất theo danh sách trạng thái. */
    List<ProductionLot> findByStatusIn(Collection<ProductionLotStatus> statuses);

    /** Tìm các lô sản xuất theo ID vùng trồng. */
    List<ProductionLot> findByFarmAreaId(UUID farmAreaId);

    /** Đếm số lô sản xuất liên quan tới vùng trồng. */
    long countByFarmAreaId(UUID farmAreaId);

    /** Truy vấn tổng hợp số lô và sản lượng theo trạng thái của tổ chức. */
    @Query("""
                SELECT pl.status, COUNT(pl), SUM(pl.expectedQuantity), SUM(pl.actualQuantity)
                FROM ProductionLot pl
                WHERE pl.organization.organizationId = :organizationId
                  AND (:startDate IS NULL OR pl.plantingDate >= :startDate)
                  AND (:endDate IS NULL OR pl.plantingDate <= :endDate)
                GROUP BY pl.status
            """)
    List<Object[]> getDashboardSummaryAndStatus(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Lấy các lô sản xuất có ngày xuống giống để gom nhóm theo thời gian. */
    @Query("""
                SELECT pl.plantingDate, pl.expectedQuantity, pl.actualQuantity
                FROM ProductionLot pl
                WHERE pl.organization.organizationId = :organizationId
                  AND pl.plantingDate IS NOT NULL
                  AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED
                  AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED
                  AND (:startDate IS NULL OR pl.plantingDate >= :startDate)
                  AND (:endDate IS NULL OR pl.plantingDate <= :endDate)
                ORDER BY pl.plantingDate ASC
            """)
    List<Object[]> getDashboardTimeSeriesData(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Lấy danh sách lô sản xuất phục vụ phân tích theo vùng trồng và mùa vụ. */
    @Query("""
                SELECT pl
                FROM ProductionLot pl
                JOIN FETCH pl.farmArea fa
                JOIN FETCH pl.productCategory pc
                JOIN FETCH pl.organization org
                WHERE pl.plantingDate BETWEEN :startDate AND :endDate
                  AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED
                  AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED
                  AND (:farmAreaId IS NULL OR fa.id = :farmAreaId)
                  AND (:productCategoryId IS NULL OR pc.id = :productCategoryId)
                  AND (:organizationId IS NULL OR org.organizationId = :organizationId)
            """)
    List<ProductionLot> findLotsForAnalysis(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("farmAreaId") UUID farmAreaId,
            @Param("productCategoryId") UUID productCategoryId,
            @Param("organizationId") UUID organizationId);

    /** Lấy danh sách lô sản xuất phục vụ phân tích trong danh sách tổ chức. */
    @Query("""
                SELECT pl
                FROM ProductionLot pl
                JOIN FETCH pl.farmArea fa
                JOIN FETCH pl.productCategory pc
                JOIN FETCH pl.organization org
                WHERE pl.plantingDate BETWEEN :startDate AND :endDate
                  AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED
                  AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED
                  AND (:farmAreaId IS NULL OR fa.id = :farmAreaId)
                  AND (:productCategoryId IS NULL OR pc.id = :productCategoryId)
                  AND org.organizationId IN :orgIds
            """)
    List<ProductionLot> findLotsForAnalysisInOrganizations(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("farmAreaId") UUID farmAreaId,
            @Param("productCategoryId") UUID productCategoryId,
            @Param("orgIds") Collection<UUID> orgIds);

    /** Lấy các lô sản xuất phục vụ so sánh sản lượng giữa nhiều mùa vụ. */
    @Query("""
            SELECT pl
            FROM ProductionLot pl
            JOIN FETCH pl.farmArea fa
            JOIN FETCH pl.productCategory pc
            JOIN FETCH pl.organization org
            WHERE YEAR(pl.plantingDate) IN :years
              AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED
              AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED
              AND (:farmAreaId IS NULL OR fa.id = :farmAreaId)
              AND (:productCategoryId IS NULL OR pc.id = :productCategoryId)
              AND (:organizationId IS NULL OR org.organizationId = :organizationId)
            """)
    List<ProductionLot> findLotsForSeasonYieldComparison(
            @Param("years") List<Integer> years,
            @Param("farmAreaId") UUID farmAreaId,
            @Param("productCategoryId") UUID productCategoryId,
            @Param("organizationId") UUID organizationId);

    /** Lấy các lô sản xuất so sánh sản lượng trong danh sách tổ chức. */
    @Query("""
            SELECT pl
            FROM ProductionLot pl
            JOIN FETCH pl.farmArea fa
            JOIN FETCH pl.productCategory pc
            JOIN FETCH pl.organization org
            WHERE YEAR(pl.plantingDate) IN :years
              AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED
              AND pl.status <> vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED
              AND (:farmAreaId IS NULL OR fa.id = :farmAreaId)
              AND (:productCategoryId IS NULL OR pc.id = :productCategoryId)
              AND org.organizationId IN :orgIds
            """)
    List<ProductionLot> findLotsForSeasonYieldComparisonInOrganizations(
            @Param("years") List<Integer> years,
            @Param("farmAreaId") UUID farmAreaId,
            @Param("productCategoryId") UUID productCategoryId,
            @Param("orgIds") Collection<UUID> orgIds);

    /** Lấy danh sách lô sản xuất đủ điều kiện xuất dữ liệu mở. */
    @Query("""
                SELECT DISTINCT pl
                FROM ProductionLot pl
                JOIN FETCH pl.organization org
                LEFT JOIN FETCH pl.farmArea fa
                JOIN FETCH pl.productCategory pc
                WHERE org.organizationId IN :orgIds
                  AND pl.harvestDate BETWEEN :fromDate AND :toDate
                  AND pl.status IN :statuses
                ORDER BY pl.harvestDate DESC
            """)
    List<ProductionLot> findEligibleLotsForExport(
            @Param("orgIds") List<UUID> orgIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<ProductionLotStatus> statuses);

    /** Tìm lô sản xuất theo ID lô và ID tổ chức. */
    @Query("""
        SELECT pl
        FROM ProductionLot pl
        WHERE pl.id = :lotId
          AND pl.organization.organizationId = :organizationId
        """)
    Optional<ProductionLot> findByIdAndOrganization_OrganizationId(
            @Param("lotId") UUID lotId,
            @Param("organizationId") UUID organizationId);

    /** Lấy hồ sơ lô sản xuất đầy đủ cho cổng dữ liệu đối tác. */
    @Query("""
        SELECT DISTINCT pl
        FROM ProductionLot pl
        JOIN FETCH pl.organization org
        LEFT JOIN FETCH pl.farmArea fa
        LEFT JOIN FETCH pl.productCategory pc
        LEFT JOIN FETCH pl.certifications plc
        LEFT JOIN FETCH plc.certification cert
        LEFT JOIN FETCH cert.standard std
        WHERE pl.id = :lotId
          AND org.organizationId = :organizationId
        """)
    Optional<ProductionLot> findDossierByIdAndOrganizationId(
            @Param("lotId") UUID lotId,
            @Param("organizationId") UUID organizationId);

    /** Tìm các lô sản xuất theo danh sách tổ chức kèm thông tin liên quan. */
    @Query("""
        SELECT DISTINCT pl
        FROM ProductionLot pl
        JOIN FETCH pl.organization org
        LEFT JOIN FETCH pl.farmArea fa
        JOIN FETCH pl.productCategory pc
        WHERE org.organizationId IN :orgIds
        """)
    List<ProductionLot> findAllInOrganizationsWithDetails(@Param("orgIds") Collection<UUID> orgIds);

    /** Tìm các lô sản xuất trên toàn hệ thống kèm thông tin liên quan. */
    @Query("""
        SELECT DISTINCT pl
        FROM ProductionLot pl
        JOIN FETCH pl.organization org
        LEFT JOIN FETCH pl.farmArea fa
        JOIN FETCH pl.productCategory pc
        """)
    List<ProductionLot> findAllWithDetails();

    /** Tìm chi tiết một lô sản xuất kèm thông tin liên quan. */
    @Query("""
        SELECT pl
        FROM ProductionLot pl
        JOIN FETCH pl.organization org
        LEFT JOIN FETCH pl.farmArea fa
        JOIN FETCH pl.productCategory pc
        WHERE pl.id = :id
        """)
    Optional<ProductionLot> findByIdWithDetails(@Param("id") UUID id);

    /** Đếm số lô sản xuất tạo mới theo từng tổ chức trong khoảng thời gian. */
    @Query("""
        SELECT pl.organization.organizationId, COUNT(pl)
        FROM ProductionLot pl
        WHERE pl.createdAt BETWEEN :from AND :to
        GROUP BY pl.organization.organizationId
        """)
    List<Object[]> countLotsGroupedByOrg(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Lấy thời điểm tạo lô mới nhất của từng tổ chức. */
    @Query("""
        SELECT pl.organization.organizationId, MAX(pl.createdAt)
        FROM ProductionLot pl
        GROUP BY pl.organization.organizationId
        """)
    List<Object[]> maxLotCreatedAtGroupedByOrg();

    /** Tổng hợp số lô đang canh tác, đã thu hoạch và tổng diện tích của một tổ chức (TASK-AI-05). */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN pl.status = vn.nguongocso.farm.enums.ProductionLotStatus.APPROVED THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN pl.status IN (
                vn.nguongocso.farm.enums.ProductionLotStatus.HARVESTED,
                vn.nguongocso.farm.enums.ProductionLotStatus.PREPROCESSED,
                vn.nguongocso.farm.enums.ProductionLotStatus.PACKAGED,
                vn.nguongocso.farm.enums.ProductionLotStatus.CLOSED
            ) THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(fa.area), 0),
            COUNT(pl),
            COALESCE(SUM(CASE WHEN pl.status = vn.nguongocso.farm.enums.ProductionLotStatus.PACKAGED THEN 1 ELSE 0 END), 0)
        FROM ProductionLot pl
        LEFT JOIN pl.farmArea fa
        WHERE pl.organization.organizationId = :organizationId
          AND pl.status NOT IN (vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED, vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED)
        """)
    List<Object[]> getLotAggregateSummaryByOrgId(@Param("organizationId") UUID organizationId);

    /** Tổng hợp số lô đang canh tác, đã thu hoạch và tổng diện tích theo danh sách tổ chức (TASK-AI-05 & TASK-AI-07). */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN pl.status = vn.nguongocso.farm.enums.ProductionLotStatus.APPROVED THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN pl.status IN (
                vn.nguongocso.farm.enums.ProductionLotStatus.HARVESTED,
                vn.nguongocso.farm.enums.ProductionLotStatus.PREPROCESSED,
                vn.nguongocso.farm.enums.ProductionLotStatus.PACKAGED,
                vn.nguongocso.farm.enums.ProductionLotStatus.CLOSED
            ) THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(fa.area), 0),
            COUNT(pl),
            COALESCE(SUM(CASE WHEN pl.status = vn.nguongocso.farm.enums.ProductionLotStatus.PACKAGED THEN 1 ELSE 0 END), 0)
        FROM ProductionLot pl
        LEFT JOIN pl.farmArea fa
        WHERE pl.organization.organizationId IN :organizationIds
          AND pl.status NOT IN (vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED, vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED)
        """)
    List<Object[]> getLotAggregateSummaryByOrgIds(@Param("organizationIds") Collection<UUID> organizationIds);

    /** Tìm tên các lô sản xuất đang canh tác có ngày thu hoạch dự kiến gần nhất (TASK-AI-05). */
    @Query("""
        SELECT pl.name
        FROM ProductionLot pl
        WHERE pl.organization.organizationId = :organizationId
          AND pl.status = vn.nguongocso.farm.enums.ProductionLotStatus.APPROVED
          AND pl.harvestDate >= :today
        ORDER BY pl.harvestDate ASC
        """)
    List<String> findUpcomingHarvestLotNames(
            @Param("organizationId") UUID organizationId,
            @Param("today") LocalDate today,
            org.springframework.data.domain.Pageable pageable);

    /** Lấy danh sách các lô sản xuất gần nhất của tổ chức để AI nắm thông tin chi tiết (TASK-AI-05). */
    @Query("""
        SELECT pl
        FROM ProductionLot pl
        LEFT JOIN FETCH pl.farmArea
        LEFT JOIN FETCH pl.productCategory
        WHERE pl.organization.organizationId = :organizationId
          AND pl.status NOT IN (vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED, vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED)
        ORDER BY pl.createdAt DESC
        """)
    List<ProductionLot> findRecentLotsByOrgId(
            @Param("organizationId") UUID organizationId,
            org.springframework.data.domain.Pageable pageable);

    /** Lấy danh sách các lô sản xuất gần nhất theo danh sách tổ chức (TASK-AI-05 & TASK-AI-07). */
    @Query("""
        SELECT pl
        FROM ProductionLot pl
        LEFT JOIN FETCH pl.farmArea
        LEFT JOIN FETCH pl.productCategory
        WHERE pl.organization.organizationId IN :organizationIds
          AND pl.status NOT IN (vn.nguongocso.farm.enums.ProductionLotStatus.CANCELLED, vn.nguongocso.farm.enums.ProductionLotStatus.DISPOSED)
        ORDER BY pl.createdAt DESC
        """)
    List<ProductionLot> findRecentLotsByOrgIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            org.springframework.data.domain.Pageable pageable);
}