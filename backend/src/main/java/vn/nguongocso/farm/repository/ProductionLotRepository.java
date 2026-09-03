package vn.nguongocso.farm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository thao tác dữ liệu lô sản xuất.
 */
public interface ProductionLotRepository extends JpaRepository<ProductionLot, UUID> {

    /**
     * Tìm tất cả các lô sản xuất theo ID tổ chức.
     *
     * @param organizationId ID của tổ chức.
     * @return Danh sách các lô sản xuất thuộc tổ chức.
     */
    List<ProductionLot> findByOrganization_OrganizationId(UUID organizationId);

    /**
     * Tìm tất cả các lô sản xuất theo ID tổ chức và trạng thái.
     *
     * @param organizationId ID của tổ chức.
     * @param status         Trạng thái của lô sản xuất.
     * @return Danh sách các lô sản xuất thuộc tổ chức và trạng thái.
     */
    List<ProductionLot> findByOrganization_OrganizationIdAndStatus(UUID organizationId, ProductionLotStatus status);

    /**
     * Tìm tất cả các lô sản xuất theo ID vùng trồng.
     *
     * @param farmAreaId ID của vùng trồng.
     * @return Danh sách các lô sản xuất thuộc vùng trồng.
     */
    List<ProductionLot> findByFarmAreaId(UUID farmAreaId);

    /**
     * Đếm số lô sản xuất liên quan tới vùng trồng.
     */
    long countByFarmAreaId(UUID farmAreaId);

    /**
     * Truy vấn tổng hợp số lô, sản lượng thực tế và dự kiến theo trạng thái của tổ
     * chức.
     */
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

    /**
     * Lấy các lô sản xuất có ngày xuống giống khác null để phục vụ gom nhóm theo
     * chu kỳ thời gian trên Java.
     */
    @Query("""
                SELECT pl.plantingDate, pl.expectedQuantity, pl.actualQuantity
                FROM ProductionLot pl
                WHERE pl.organization.organizationId = :organizationId
                  AND pl.plantingDate IS NOT NULL
                  AND (:startDate IS NULL OR pl.plantingDate >= :startDate)
                  AND (:endDate IS NULL OR pl.plantingDate <= :endDate)
                ORDER BY pl.plantingDate ASC
            """)
    List<Object[]> getDashboardTimeSeriesData(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Truy vấn danh sách các lô sản xuất phục vụ phân tích theo vùng trồng và mùa
     * vụ.
     * Sử dụng JOIN FETCH để tải trước các thực thể liên quan, tránh N+1 Query.
     */
    @Query("""
                SELECT pl
                FROM ProductionLot pl
                JOIN FETCH pl.farmArea fa
                JOIN FETCH pl.productCategory pc
                JOIN FETCH pl.organization org
                WHERE pl.plantingDate BETWEEN :startDate AND :endDate
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

    /**
     * Như {@link #findLotsForAnalysis} nhưng giới hạn cứng trong danh sách tổ
     * chức cho phép (lọc địa bàn VT-05 / bộ lọc unitIds của NCL-743).
     */
    @Query("""
                SELECT pl
                FROM ProductionLot pl
                JOIN FETCH pl.farmArea fa
                JOIN FETCH pl.productCategory pc
                JOIN FETCH pl.organization org
                WHERE pl.plantingDate BETWEEN :startDate AND :endDate
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

    /**
     * Lấy các lô sản xuất phục vụ so sánh sản lượng giữa nhiều mùa vụ.
     */
    @Query("""
            SELECT pl
            FROM ProductionLot pl
            JOIN FETCH pl.farmArea fa
            JOIN FETCH pl.productCategory pc
            JOIN FETCH pl.organization org
            WHERE YEAR(pl.plantingDate) IN :years
              AND (:farmAreaId IS NULL OR fa.id = :farmAreaId)
              AND (:productCategoryId IS NULL OR pc.id = :productCategoryId)
              AND (:organizationId IS NULL OR org.organizationId = :organizationId)
            """)
    List<ProductionLot> findLotsForSeasonYieldComparison(
            @Param("years") List<Integer> years,
            @Param("farmAreaId") UUID farmAreaId,
            @Param("productCategoryId") UUID productCategoryId,
            @Param("organizationId") UUID organizationId);

    /**
     * Như {@link #findLotsForSeasonYieldComparison} nhưng giới hạn cứng trong
     * danh sách tổ chức cho phép (lọc địa bàn VT-05 / bộ lọc unitIds).
     */
    @Query("""
            SELECT pl
            FROM ProductionLot pl
            JOIN FETCH pl.farmArea fa
            JOIN FETCH pl.productCategory pc
            JOIN FETCH pl.organization org
            WHERE YEAR(pl.plantingDate) IN :years
              AND (:farmAreaId IS NULL OR fa.id = :farmAreaId)
              AND (:productCategoryId IS NULL OR pc.id = :productCategoryId)
              AND org.organizationId IN :orgIds
            """)
    List<ProductionLot> findLotsForSeasonYieldComparisonInOrganizations(
            @Param("years") List<Integer> years,
            @Param("farmAreaId") UUID farmAreaId,
            @Param("productCategoryId") UUID productCategoryId,
            @Param("orgIds") Collection<UUID> orgIds);

    /**
     * Lấy danh sách lô sản xuất đủ điều kiện xuất dữ liệu mở (QTN-11).
     * Bao gồm các lô thuộc các tổ chức trong danh sách, trong khoảng thời gian,
     * và có trạng thái thuộc danh sách cho phép.
     * Sử dụng JOIN FETCH để tải các quan hệ cần thiết, tránh N+1.
     */
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

    /**
     * Tìm lô sản xuất theo ID lô và ID tổ chức.
     *
     * Dùng để đảm bảo người dùng chỉ được thao tác
     * trên lô thuộc tổ chức hiện tại.
     *
     * @param lotId          ID lô sản xuất
     * @param organizationId ID tổ chức hiện tại
     * @return lô sản xuất nếu thuộc tổ chức
     */
    @Query("""
        SELECT pl
        FROM ProductionLot pl
        WHERE pl.id = :lotId
          AND pl.organization.organizationId = :organizationId
        """)
    Optional<ProductionLot> findByIdAndOrganization_OrganizationId(
            @Param("lotId") UUID lotId,
            @Param("organizationId") UUID organizationId);

    /**
     * Truy vấn hồ sơ lô sản xuất đầy đủ cho cổng dữ liệu đối tác bên thứ ba.
     * Sử dụng JOIN FETCH để nạp trước Organization, FarmArea, ProductCategory và Certifications
     * đồng thời thực thi bảo mật Cách ly dữ liệu tổ chức (Tenant Isolation - TC-04).
     */
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
}