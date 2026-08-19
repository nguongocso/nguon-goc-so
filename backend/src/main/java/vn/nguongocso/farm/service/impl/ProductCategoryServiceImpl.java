package vn.nguongocso.farm.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.exception.DuplicateResourceException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CreateProductCategoryRequest;
import vn.nguongocso.farm.dto.request.UpdateProductCategoryRequest;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.ProductCategoryService;

/**
 * Triển khai nghiệp vụ danh mục loại cây trồng.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

	private final ProductCategoryRepository productCategoryRepository;

	/**
	 * Lấy danh sách loại cây trồng.
	 *
	 * @return danh sách loại cây trồng
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ProductCategoryResponse> getAll() {
		return productCategoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Tìm kiếm danh mục loại cây trồng theo tên, nhóm và trạng thái hoạt động.
	 *
	 * @param name        tên loại cây trồng
	 * @param group       nhóm loại cây trồng
	 * @param isActive    trạng thái hoạt động
	 * @param currentUser người dùng hiện tại
	 * @return danh sách loại cây trồng phù hợp với điều kiện tìm kiếm
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ProductCategoryResponse> search(String name, String group, Boolean isActive,
			CustomUserDetails currentUser) {
		log.info("Tìm kiếm danh mục loại nông sản với name={}, group={}, isActive={}", name, group, isActive);

		Boolean filterActive = isActive;
		boolean isAdmin = currentUser.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_VT-01"));

		if (!isAdmin) {
			if (Boolean.FALSE.equals(isActive)) {
				throw new AccessDeniedException("Bạn không có quyền xem danh mục loại nông sản bị ẩn");
			}
			filterActive = true;
		}

		return productCategoryRepository.search(name, group, filterActive).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Tạo mới loại cây trồng.
	 *
	 * @param request thông tin loại cây trồng cần tạo
	 * @return thông tin loại cây trồng sau khi tạo
	 */
	@Override
	@Transactional
	@Auditable(action = "CREATE_PRODUCT_CATEGORY", entityType = "PRODUCT_CATEGORY", description = "'Thêm mới loại nông sản: ' + #request.name + ', thuộc nhóm hàng: ' + #request.group")
	public ProductCategoryResponse create(CreateProductCategoryRequest request) {
		log.info("Bắt đầu xử lý thêm mới loại nông sản: {}", request.getName());

		if (productCategoryRepository.existsByNameIgnoreCase(request.getName().trim())) {
			throw new DuplicateResourceException(
					"Loại nông sản với tên '" + request.getName() + "' đã tồn tại trong danh mục");
		}

		ProductCategory category = new ProductCategory();
		category.setId(UUID.randomUUID());
		category.setName(request.getName().trim());
		category.setGroup(request.getGroup().trim());
		category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
		category.setIsActive(true);

		ProductCategory saved = productCategoryRepository.save(category);
		log.info("Thêm mới loại nông sản thành công, ID={}", saved.getId());
		return toResponse(saved);
	}

	/**
	 * Cập nhật thông tin loại cây trồng.
	 *
	 * @param id      ID của loại cây trồng cần cập nhật
	 * @param request thông tin mới của loại cây trồng
	 * @return thông tin loại cây trồng sau khi cập nhật
	 */
	@Override
	@Transactional
	@Auditable(action = "UPDATE_PRODUCT_CATEGORY", entityType = "PRODUCT_CATEGORY", description = "'Cập nhật loại nông sản ID: ' + #id + ', Tên mới: ' + #request.name + ', Trạng thái hoạt động: ' + #request.isActive")
	public ProductCategoryResponse update(UUID id, UpdateProductCategoryRequest request) {
		log.info("Bắt đầu xử lý cập nhật loại nông sản ID: {}", id);

		ProductCategory category = productCategoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại nông sản với ID: " + id));

		if (productCategoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
			throw new DuplicateResourceException(
					"Loại nông sản với tên '" + request.getName() + "' đã tồn tại trong danh mục");
		}

		category.setName(request.getName().trim());
		category.setGroup(request.getGroup().trim());
		category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
		category.setIsActive(request.getIsActive());

		ProductCategory updated = productCategoryRepository.save(category);
		log.info("Cập nhật loại nông sản thành công, ID={}", updated.getId());
		return toResponse(updated);
	}

	private ProductCategoryResponse toResponse(ProductCategory category) {
		return ProductCategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.group(category.getGroup())
				.description(category.getDescription())
				.isActive(category.getIsActive())
				.build();
	}
}
