package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.InputMaterial;
import vn.nguongocso.farm.enums.MaterialGroup;

/**
 * Repository thao tác dữ liệu danh mục vật tư đầu vào.
 */
public interface InputMaterialRepository extends JpaRepository<InputMaterial, UUID> {

	/**
	 * Kiểm tra xem đã tồn tại vật tư trùng cả tên và hoạt chất hay chưa.
	 */
	@Query("""
			SELECT COUNT(im) > 0 FROM InputMaterial im
			WHERE LOWER(TRIM(im.name)) = LOWER(TRIM(:name))
			AND ((:activeIngredient IS NULL AND im.activeIngredient IS NULL)
			     OR (LOWER(TRIM(im.activeIngredient)) = LOWER(TRIM(:activeIngredient))))
			""")
	boolean existsByNameAndActiveIngredient(
			@Param("name") String name,
			@Param("activeIngredient") String activeIngredient);

	/**
	 * Kiểm tra xem đã tồn tại vật tư trùng tên và hoạt chất (ngoại trừ ID hiện tại).
	 */
	@Query("""
			SELECT COUNT(im) > 0 FROM InputMaterial im
			WHERE im.id != :id
			AND LOWER(TRIM(im.name)) = LOWER(TRIM(:name))
			AND ((:activeIngredient IS NULL AND im.activeIngredient IS NULL)
			     OR (LOWER(TRIM(im.activeIngredient)) = LOWER(TRIM(:activeIngredient))))
			""")
	boolean existsByNameAndActiveIngredientExcludingId(
			@Param("id") UUID id,
			@Param("name") String name,
			@Param("activeIngredient") String activeIngredient);

	/**
	 * Tìm kiếm vật tư theo từ khóa, nhóm vật tư / danh sách nhóm và trạng thái active.
	 */
	@Query("""
			SELECT DISTINCT im FROM InputMaterial im
			LEFT JOIN FETCH im.applicableCropTypes
			WHERE (:keyword IS NULL OR LOWER(im.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(im.activeIngredient) LIKE LOWER(CONCAT('%', :keyword, '%')))
			AND (:group IS NULL OR im.materialGroup = :group)
			AND (:groups IS NULL OR im.materialGroup IN :groups)
			AND (:isActive IS NULL OR im.isActive = :isActive)
			""")
	Page<InputMaterial> searchMaterials(
			@Param("keyword") String keyword,
			@Param("group") MaterialGroup group,
			@Param("groups") List<MaterialGroup> groups,
			@Param("isActive") Boolean isActive,
			Pageable pageable);

	/**
	 * Lấy chi tiết vật tư kèm theo danh sách loại nông sản áp dụng.
	 */
	@Query("SELECT im FROM InputMaterial im LEFT JOIN FETCH im.applicableCropTypes WHERE im.id = :id")
	Optional<InputMaterial> findByIdWithCropTypes(@Param("id") UUID id);

	/**
	 * Tìm danh sách vật tư theo tên (không phân biệt hoa thường và khoảng trắng).
	 */
	@Query("""
			SELECT im FROM InputMaterial im
			WHERE LOWER(TRIM(im.name)) = LOWER(TRIM(:name))
			ORDER BY im.isActive DESC, im.quarantineDays DESC
			""")
	List<InputMaterial> findByNameNormalized(@Param("name") String name);
}
