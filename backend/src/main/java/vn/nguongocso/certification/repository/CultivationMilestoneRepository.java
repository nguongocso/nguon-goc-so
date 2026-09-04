package vn.nguongocso.certification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.CultivationMilestone;
import java.util.List;
import java.util.UUID;

/**
 * Repository cho bảng mốc canh tác hợp nhất.
 * Story: NCL-09-CN-011
 */
public interface CultivationMilestoneRepository extends JpaRepository<CultivationMilestone, Long> {

    /**
     * Kiểm tra trùng tên trong cùng (product_category_id, standard_id).
     * NULL = áp dụng toàn bộ loại / mọi tiêu chuẩn, so khớp tường minh bằng IS NULL.
     */
    @Query("SELECT COUNT(m) > 0 FROM CultivationMilestone m " +
           "WHERE LOWER(m.name) = LOWER(:name) " +
           "AND ( (:categoryId IS NULL AND m.productCategory IS NULL) " +
           "      OR (:categoryId IS NOT NULL AND m.productCategory.id = :categoryId) ) " +
           "AND ( (:standardId IS NULL AND m.standard IS NULL) " +
           "      OR (:standardId IS NOT NULL AND m.standard.id = :standardId) )")
    boolean existsByNameAndCategoryAndStandard(
            @Param("name") String name,
            @Param("categoryId") UUID categoryId,
            @Param("standardId") UUID standardId);

    /**
     * Kiểm tra trùng tên khi cập nhật, loại trừ id hiện tại.
     */
    @Query("SELECT COUNT(m) > 0 FROM CultivationMilestone m " +
           "WHERE LOWER(m.name) = LOWER(:name) " +
           "AND ( (:categoryId IS NULL AND m.productCategory IS NULL) " +
           "      OR (:categoryId IS NOT NULL AND m.productCategory.id = :categoryId) ) " +
           "AND ( (:standardId IS NULL AND m.standard IS NULL) " +
           "      OR (:standardId IS NOT NULL AND m.standard.id = :standardId) ) " +
           "AND m.id <> :excludeId")
    boolean existsByNameAndCategoryAndStandardAndIdNot(
            @Param("name") String name,
            @Param("categoryId") UUID categoryId,
            @Param("standardId") UUID standardId,
            @Param("excludeId") Long excludeId);

    /**
     * Tìm mốc theo bộ lọc, phân trang.
     * categoryId/standardId = null nghĩa là không lọc theo giá trị đó
     * (không nhầm với mốc GLOBAL — dùng tham số riêng global flag khi cần).
     */
    @Query("SELECT m FROM CultivationMilestone m " +
           "LEFT JOIN FETCH m.productCategory pc " +
           "LEFT JOIN FETCH m.standard st " +
           "WHERE (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:activityType IS NULL OR m.activityType = :activityType) " +
           "AND (:categoryId IS NULL OR pc.id = :categoryId) " +
           "AND (:standardId IS NULL OR st.id = :standardId) " +
           "AND (:globalOnly = false OR m.productCategory IS NULL) " +
           "ORDER BY m.name ASC")
    Page<CultivationMilestone> search(
            @Param("keyword") String keyword,
            @Param("activityType") String activityType,
            @Param("categoryId") UUID categoryId,
            @Param("standardId") UUID standardId,
            @Param("globalOnly") boolean globalOnly,
            Pageable pageable);

    /**
     * Tìm tất cả mốc (cho danh sách đầy đủ).
     */
    @Query("SELECT m FROM CultivationMilestone m " +
           "LEFT JOIN FETCH m.productCategory pc " +
           "LEFT JOIN FETCH m.standard st " +
           "ORDER BY m.name ASC")
    List<CultivationMilestone> findAllWithDetails();

    /**
     * Mốc bắt buộc dùng để kiểm tra khi đóng gói.
     * Áp dụng cho category của lô (hoặc GLOBAL category NULL) và
     * trong tập standardIds của lô (hoặc GLOBAL standard NULL).
     */
    @Query("SELECT m FROM CultivationMilestone m " +
           "WHERE m.isMandatory = true " +
           "AND (m.productCategory IS NULL OR m.productCategory.id = :categoryId) " +
           "AND (m.standard IS NULL OR m.standard.id IN :standardIds) " +
           "ORDER BY m.name ASC")
    List<CultivationMilestone> findMandatoryMilestonesForValidation(
            @Param("categoryId") UUID categoryId,
            @Param("standardIds") List<UUID> standardIds);
}
