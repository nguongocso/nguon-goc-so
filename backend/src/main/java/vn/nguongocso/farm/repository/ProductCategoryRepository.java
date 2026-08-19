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
	/**
	 * Lấy danh sách danh mục loại cây trồng đang hoạt động, sắp xếp theo tên tăng
	 * dần.
	 *
	 * @return danh sách danh mục loại cây trồng
	 */
	List<ProductCategory> findByIsActiveTrueOrderByNameAsc();

	/**
	 * Kiểm tra xem có tồn tại danh mục loại cây trồng nào theo tên (không phân biệt
	 * hoa thường) hay không.
	 *
	 * @param name tên của danh mục loại cây trồng
	 * @return true nếu tồn tại, false nếu không tồn tại
	 */
	boolean existsByNameIgnoreCase(String name);

	/**
	 * Kiểm tra xem có tồn tại danh mục loại cây trồng nào theo tên (không phân biệt
	 * hoa thường) và ID khác ID cho trước hay không.
	 *
	 * @param name tên của danh mục loại cây trồng
	 * @param id   ID của danh mục loại cây trồng
	 * @return true nếu tồn tại, false nếu không tồn tại
	 */
	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

	@Query("""
			    SELECT c FROM ProductCategory c
			    WHERE (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
			      AND (:group IS NULL OR LOWER(c.group) = LOWER(:group))
			      AND (:isActive IS NULL OR c.isActive = :isActive)
			    ORDER BY c.name ASC
			""")

	/**
	 * Tìm kiếm danh mục loại cây trồng theo tên, nhóm và trạng thái hoạt động.
	 *
	 * @param name     tên của danh mục loại cây trồng
	 * @param group    nhóm của danh mục loại cây trồng
	 * @param isActive trạng thái hoạt động của danh mục loại cây trồng
	 * @return danh sách danh mục loại cây trồng phù hợp với điều kiện tìm kiếm
	 */
	List<ProductCategory> search(
			@Param("name") String name,
			@Param("group") String group,
			@Param("isActive") Boolean isActive);
}