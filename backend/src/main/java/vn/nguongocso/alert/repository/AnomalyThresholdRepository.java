package vn.nguongocso.alert.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import vn.nguongocso.alert.entity.AnomalyThreshold;

/**
 * Repository thao tác cơ sở dữ liệu cho cấu hình ngưỡng quét bất thường (NCL-08-CN-014).
 */
@Repository
public interface AnomalyThresholdRepository extends JpaRepository<AnomalyThreshold, UUID> {

    /**
     * Tìm cấu hình mặc định toàn cục đang hoạt động.
     */
    Optional<AnomalyThreshold> findByProductCategoryIsNullAndIsActiveTrue();

    /**
     * Tìm cấu hình mặc định toàn cục (bất kể trạng thái active).
     */
    Optional<AnomalyThreshold> findByProductCategoryIsNull();

    /**
     * Tìm cấu hình ghi đè theo ID danh mục nông sản đang hoạt động.
     */
    Optional<AnomalyThreshold> findByProductCategoryIdAndIsActiveTrue(UUID productCategoryId);

    /**
     * Tìm cấu hình ghi đè theo ID danh mục nông sản (bất kể trạng thái active).
     */
    Optional<AnomalyThreshold> findByProductCategoryId(UUID productCategoryId);

    /**
     * Lấy danh sách tất cả các cấu hình ghi đè theo danh mục nông sản đang hoạt động.
     */
    @Query("SELECT t FROM AnomalyThreshold t JOIN FETCH t.productCategory pc WHERE t.productCategory IS NOT NULL AND t.isActive = true ORDER BY pc.name ASC")
    List<AnomalyThreshold> findAllActiveCategoryOverrides();

    /**
     * Lấy danh sách tất cả cấu hình kèm thông tin danh mục.
     */
    @Query("SELECT t FROM AnomalyThreshold t LEFT JOIN FETCH t.productCategory pc WHERE t.isActive = true")
    List<AnomalyThreshold> findAllActiveWithCategory();
}
