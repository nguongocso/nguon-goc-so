package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.ProductCategory;
/**
 * Repository thao tác dữ liệu danh mục loại cây trồng.
*/
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
	/** Lấy danh mục loại cây trồng đang hoạt động theo tên tăng dần. */
	List<ProductCategory> findByIsActiveTrueOrderByNameAsc();

	/** Kiểm tra danh mục loại cây trồng theo tên đã tồn tại hay chưa. */
	boolean existsByNameIgnoreCase(String name);

	/** Kiểm tra danh mục theo tên khác ID cho trước đã tồn tại hay chưa. */
	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

	/** Tìm kiếm danh mục loại cây trồng theo tên, nhóm và trạng thái. */
	@Query("""
			    SELECT c FROM ProductCategory c
			    WHERE (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
			      AND (:group IS NULL OR LOWER(c.group) = LOWER(:group))
			      AND (:isActive IS NULL OR c.isActive = :isActive)
			    ORDER BY c.name ASC
			""")
	List<ProductCategory> search(
			@Param("name") String name,
			@Param("group") String group,
			@Param("isActive") Boolean isActive);
}