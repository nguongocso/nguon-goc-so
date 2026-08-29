package vn.nguongocso.farm.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.DuplicateResourceException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CreateInputMaterialRequest;
import vn.nguongocso.farm.dto.request.UpdateInputMaterialRequest;
import vn.nguongocso.farm.dto.response.InputMaterialResponse;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;
import vn.nguongocso.farm.entity.InputMaterial;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.MaterialGroup;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.InputMaterialRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.InputMaterialService;

/**
 * Hiện thực Service xử lý nghiệp vụ quản lý danh mục vật tư đầu vào.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InputMaterialServiceImpl implements InputMaterialService {

	private final InputMaterialRepository inputMaterialRepository;
	private final ProductCategoryRepository productCategoryRepository;
	private final FarmLogRepository farmLogRepository;

	@Override
	public InputMaterialResponse createInputMaterial(CreateInputMaterialRequest request, UUID currentUserId) {
		validateQuarantineDays(request.getMaterialGroup(), request.getQuarantineDays());

		if (inputMaterialRepository.existsByNameAndActiveIngredient(request.getName(), request.getActiveIngredient())) {
			throw new DuplicateResourceException("Vật tư đã tồn tại với cùng tên và hoạt chất này");
		}

		Integer quarantineDays = request.getQuarantineDays();
		if (quarantineDays == null) {
			quarantineDays = 0;
		}

		Boolean applyToAllCrops = request.getApplyToAllCrops() != null ? request.getApplyToAllCrops() : true;
		Set<ProductCategory> cropCategories = resolveCropCategories(applyToAllCrops, request.getApplicableCropTypeIds());

		String imageUrlsStr = (request.getImageUrls() != null && !request.getImageUrls().isEmpty())
				? String.join(";;;", request.getImageUrls())
				: null;

		InputMaterial material = InputMaterial.builder()
				.name(request.getName().trim())
				.materialGroup(request.getMaterialGroup())
				.activeIngredient(request.getActiveIngredient() != null ? request.getActiveIngredient().trim() : null)
				.unit(request.getUnit().trim())
				.quarantineDays(quarantineDays)
				.applyToAllCrops(applyToAllCrops)
				.applicableCropTypes(cropCategories)
				.referenceSource(request.getReferenceSource() != null ? request.getReferenceSource().trim() : null)
				.imageUrls(imageUrlsStr)
				.isActive(true)
				.createdBy(currentUserId)
				.build();

		InputMaterial saved = inputMaterialRepository.save(material);
		return mapToResponse(saved);
	}

	@Override
	public InputMaterialResponse updateInputMaterial(UUID id, UpdateInputMaterialRequest request, UUID currentUserId) {
		InputMaterial material = inputMaterialRepository.findByIdWithCropTypes(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư đầu vào với ID: " + id));

		validateQuarantineDays(request.getMaterialGroup(), request.getQuarantineDays());

		if (inputMaterialRepository.existsByNameAndActiveIngredientExcludingId(id, request.getName(), request.getActiveIngredient())) {
			throw new DuplicateResourceException("Vật tư đã tồn tại với cùng tên và hoạt chất này");
		}

		Integer quarantineDays = request.getQuarantineDays();
		if (quarantineDays == null) {
			quarantineDays = 0;
		}

		Boolean applyToAllCrops = request.getApplyToAllCrops() != null ? request.getApplyToAllCrops() : true;
		Set<ProductCategory> cropCategories = resolveCropCategories(applyToAllCrops, request.getApplicableCropTypeIds());

		material.setName(request.getName().trim());
		material.setMaterialGroup(request.getMaterialGroup());
		material.setActiveIngredient(request.getActiveIngredient() != null ? request.getActiveIngredient().trim() : null);
		material.setUnit(request.getUnit().trim());
		material.setQuarantineDays(quarantineDays);
		material.setApplyToAllCrops(applyToAllCrops);
		material.setApplicableCropTypes(cropCategories);
		material.setReferenceSource(request.getReferenceSource() != null ? request.getReferenceSource().trim() : null);
		
		if (request.getImageUrls() != null) {
			material.setImageUrls(request.getImageUrls().isEmpty() ? null : String.join(";;;", request.getImageUrls()));
		}
		
		if (request.getIsActive() != null) {
			material.setIsActive(request.getIsActive());
		}

		InputMaterial updated = inputMaterialRepository.save(material);
		return mapToResponse(updated);
	}

	@Override
	public InputMaterialResponse toggleActiveStatus(UUID id, Boolean isActive) {
		InputMaterial material = inputMaterialRepository.findByIdWithCropTypes(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư đầu vào với ID: " + id));

		material.setIsActive(isActive);
		InputMaterial updated = inputMaterialRepository.save(material);
		return mapToResponse(updated);
	}

	@Override
	public void deleteInputMaterial(UUID id) {
		InputMaterial material = inputMaterialRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư đầu vào với ID: " + id));

		// TC-04: Kiểm tra vật tư đã được dùng trong nhật ký canh tác chưa
		if (farmLogRepository.existsByMaterialIgnoreCase(material.getName())) {
			throw new BusinessException("Vật tư đã được dùng trong nhật ký canh tác. Hệ thống chặn xóa và chỉ cho phép ngừng sử dụng.");
		}

		inputMaterialRepository.delete(material);
	}

	@Override
	@Transactional(readOnly = true)
	public InputMaterialResponse getInputMaterialById(UUID id) {
		InputMaterial material = inputMaterialRepository.findByIdWithCropTypes(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư đầu vào với ID: " + id));
		return mapToResponse(material);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<InputMaterialResponse> searchMaterials(String keyword, MaterialGroup group, FarmActivityType activityType, Boolean isActive, Pageable pageable) {
		String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

		List<MaterialGroup> groups = null;
		if (activityType != null) {
			groups = getMaterialGroupsForActivity(activityType);
			if (groups != null && groups.isEmpty()) {
				return Page.empty(pageable);
			}
		}

		return inputMaterialRepository.searchMaterials(cleanKeyword, group, groups, isActive, pageable)
				.map(this::mapToResponse);
	}

	public List<MaterialGroup> getMaterialGroupsForActivity(FarmActivityType activityType) {
		if (activityType == null) {
			return null;
		}
		return switch (activityType) {
			case FERTILIZING -> List.of(MaterialGroup.FERTILIZER);
			case PESTICIDE -> List.of(MaterialGroup.PESTICIDE);
			case PLANTING -> List.of(MaterialGroup.FERTILIZER, MaterialGroup.BIOLOGICAL, MaterialGroup.OTHER);
			case WATERING -> List.of(MaterialGroup.OTHER);
			case WEEDING -> List.of(MaterialGroup.PESTICIDE, MaterialGroup.OTHER);
			case HARVESTING -> Collections.emptyList();
			case OTHER -> Arrays.asList(MaterialGroup.values());
		};
	}

	/**
	 * Kiểm tra điều kiện bắt buộc của thời gian cách ly theo nhóm vật tư.
	 */
	private void validateQuarantineDays(MaterialGroup group, Integer quarantineDays) {
		if (group == MaterialGroup.PESTICIDE) {
			if (quarantineDays == null) {
				throw new BusinessException("Nhóm thuốc bảo vệ thực vật bắt buộc phải có thời gian cách ly");
			}
		}

		if (quarantineDays != null && quarantineDays < 0) {
			throw new BusinessException("Thời gian cách ly phải là số nguyên không âm");
		}
	}

	/**
	 * Tra cứu danh mục loại nông sản từ danh sách ID.
	 */
	private Set<ProductCategory> resolveCropCategories(Boolean applyToAllCrops, Set<UUID> cropTypeIds) {
		if (Boolean.TRUE.equals(applyToAllCrops) || cropTypeIds == null || cropTypeIds.isEmpty()) {
			return new HashSet<>();
		}
		List<ProductCategory> categories = productCategoryRepository.findAllById(cropTypeIds);
		return new HashSet<>(categories);
	}

	/**
	 * Map Entity sang Response DTO.
	 */
	private InputMaterialResponse mapToResponse(InputMaterial entity) {
		Set<ProductCategoryResponse> cropResponses = entity.getApplicableCropTypes().stream()
				.map(c -> ProductCategoryResponse.builder()
						.id(c.getId())
						.name(c.getName())
						.group(c.getGroup())
						.description(c.getDescription())
						.isActive(c.getIsActive())
						.tempMin(c.getTempMin())
						.tempMax(c.getTempMax())
						.humidityMin(c.getHumidityMin())
						.humidityMax(c.getHumidityMax())
						.build())
				.collect(Collectors.toSet());

		List<String> imageUrlsList = (entity.getImageUrls() != null && !entity.getImageUrls().trim().isEmpty())
				? Arrays.asList(entity.getImageUrls().split(";;;"))
				: Collections.emptyList();

		return InputMaterialResponse.builder()
				.id(entity.getId())
				.name(entity.getName())
				.materialGroup(entity.getMaterialGroup())
				.materialGroupDisplayName(entity.getMaterialGroup().getDisplayName())
				.activeIngredient(entity.getActiveIngredient())
				.unit(entity.getUnit())
				.quarantineDays(entity.getQuarantineDays())
				.applyToAllCrops(entity.getApplyToAllCrops())
				.applicableCropTypes(cropResponses)
				.referenceSource(entity.getReferenceSource())
				.imageUrls(imageUrlsList)
				.isActive(entity.getIsActive())
				.createdBy(entity.getCreatedBy())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}
}
