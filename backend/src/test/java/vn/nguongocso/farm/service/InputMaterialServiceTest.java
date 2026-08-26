package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.DuplicateResourceException;

import vn.nguongocso.farm.dto.request.CreateInputMaterialRequest;
import vn.nguongocso.farm.dto.request.UpdateInputMaterialRequest;
import vn.nguongocso.farm.dto.response.InputMaterialResponse;
import vn.nguongocso.farm.entity.InputMaterial;
import vn.nguongocso.farm.enums.MaterialGroup;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.InputMaterialRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.impl.InputMaterialServiceImpl;

/**
 * Unit Test kiểm thử các kịch bản nghiệp vụ quản lý danh mục vật tư (US NCL-09-CN-010).
 */
@ExtendWith(MockitoExtension.class)
class InputMaterialServiceTest {

	@Mock
	private InputMaterialRepository inputMaterialRepository;

	@Mock
	private ProductCategoryRepository productCategoryRepository;

	@Mock
	private FarmLogRepository farmLogRepository;

	@InjectMocks
	private InputMaterialServiceImpl inputMaterialService;

	private InputMaterial pesticideMaterial;
	private UUID currentUserId;

	@BeforeEach
	void setUp() {
		currentUserId = UUID.randomUUID();
		pesticideMaterial = InputMaterial.builder()
				.id(UUID.randomUUID())
				.name("Brightin 4.0EC")
				.materialGroup(MaterialGroup.PESTICIDE)
				.activeIngredient("Abamectin")
				.unit("ml")
				.quarantineDays(7)
				.applyToAllCrops(true)
				.applicableCropTypes(Collections.emptySet())
				.referenceSource("Thông tư 10/2020/TT-BNNPTNT")
				.isActive(true)
				.build();
	}

	@Test
	@DisplayName("TC-01: Thêm một loại thuốc BVTV kèm thời gian cách ly 7 ngày thành công")
	void NCL_09_CN_010_TC_01_create_shouldSuccess_whenValidPesticide() {
		// Given
		CreateInputMaterialRequest request = CreateInputMaterialRequest.builder()
				.name("Brightin 4.0EC")
				.materialGroup(MaterialGroup.PESTICIDE)
				.activeIngredient("Abamectin")
				.unit("ml")
				.quarantineDays(7)
				.applyToAllCrops(true)
				.referenceSource("Thông tư 10/2020/TT-BNNPTNT")
				.build();

		when(inputMaterialRepository.existsByNameAndActiveIngredient("Brightin 4.0EC", "Abamectin"))
				.thenReturn(false);
		when(inputMaterialRepository.save(any(InputMaterial.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When
		InputMaterialResponse response = inputMaterialService.createInputMaterial(request, currentUserId);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getName()).isEqualTo("Brightin 4.0EC");
		assertThat(response.getMaterialGroup()).isEqualTo(MaterialGroup.PESTICIDE);
		assertThat(response.getQuarantineDays()).isEqualTo(7);
		assertThat(response.getIsActive()).isTrue();
		verify(inputMaterialRepository, times(1)).save(any(InputMaterial.class));
	}

	@Test
	@DisplayName("TC-02: Chọn nhóm Thuốc BVTV nhưng bỏ trống thời gian cách ly -> Hệ thống chặn và yêu cầu nhập")
	void NCL_09_CN_010_TC_02_create_shouldThrowBusinessException_whenPesticideQuarantineDaysIsNull() {
		// Given
		CreateInputMaterialRequest request = CreateInputMaterialRequest.builder()
				.name("Brightin 4.0EC")
				.materialGroup(MaterialGroup.PESTICIDE)
				.activeIngredient("Abamectin")
				.unit("ml")
				.quarantineDays(null)
				.build();

		// When & Then
		assertThatThrownBy(() -> inputMaterialService.createInputMaterial(request, currentUserId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("bắt buộc phải có thời gian cách ly");

		verify(inputMaterialRepository, never()).save(any());
	}

	@Test
	@DisplayName("TC-02: Thời gian cách ly là số âm -> Hệ thống chặn và báo lỗi")
	void NCL_09_CN_010_TC_02_create_shouldThrowBusinessException_whenQuarantineDaysIsNegative() {
		// Given
		CreateInputMaterialRequest request = CreateInputMaterialRequest.builder()
				.name("Phân NPK 16-16-8")
				.materialGroup(MaterialGroup.FERTILIZER)
				.unit("kg")
				.quarantineDays(-5)
				.build();

		// When & Then
		assertThatThrownBy(() -> inputMaterialService.createInputMaterial(request, currentUserId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("không âm");

		verify(inputMaterialRepository, never()).save(any());
	}

	@Test
	@DisplayName("TC-03: Thêm vật tư trùng tên và cùng hoạt chất -> Hệ thống báo trùng và yêu cầu đổi thông tin")
	void NCL_09_CN_010_TC_03_create_shouldThrowDuplicate_whenNameAndActiveIngredientExist() {
		// Given
		CreateInputMaterialRequest request = CreateInputMaterialRequest.builder()
				.name("Brightin 4.0EC")
				.materialGroup(MaterialGroup.PESTICIDE)
				.activeIngredient("Abamectin")
				.unit("ml")
				.quarantineDays(7)
				.build();

		when(inputMaterialRepository.existsByNameAndActiveIngredient("Brightin 4.0EC", "Abamectin"))
				.thenReturn(true);

		// When & Then
		assertThatThrownBy(() -> inputMaterialService.createInputMaterial(request, currentUserId))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining("Vật tư đã tồn tại với cùng tên và hoạt chất này");

		verify(inputMaterialRepository, never()).save(any());
	}

	@Test
	@DisplayName("TC-04: Xóa vật tư đã được dùng trong nhật ký canh tác -> Chặn xóa và báo lỗi chỉ cho phép ngừng sử dụng")
	void NCL_09_CN_010_TC_04_delete_shouldThrowBusinessException_whenMaterialIsReferencedInFarmLog() {
		// Given
		UUID materialId = pesticideMaterial.getId();
		when(inputMaterialRepository.findById(materialId)).thenReturn(Optional.of(pesticideMaterial));
		when(farmLogRepository.existsByMaterialIgnoreCase("Brightin 4.0EC")).thenReturn(true);

		// When & Then
		assertThatThrownBy(() -> inputMaterialService.deleteInputMaterial(materialId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Vật tư đã được dùng trong nhật ký canh tác");

		verify(inputMaterialRepository, never()).delete(any());
	}

	@Test
	@DisplayName("TC-04 Thành công: Xóa vật tư chưa từng được dùng trong nhật ký canh tác -> Xóa thành công")
	void NCL_09_CN_010_TC_04_delete_shouldSuccess_whenMaterialIsNotReferencedInFarmLog() {
		// Given
		UUID materialId = pesticideMaterial.getId();
		when(inputMaterialRepository.findById(materialId)).thenReturn(Optional.of(pesticideMaterial));
		when(farmLogRepository.existsByMaterialIgnoreCase("Brightin 4.0EC")).thenReturn(false);

		// When
		inputMaterialService.deleteInputMaterial(materialId);

		// Then
		verify(inputMaterialRepository, times(1)).delete(pesticideMaterial);
	}
}
