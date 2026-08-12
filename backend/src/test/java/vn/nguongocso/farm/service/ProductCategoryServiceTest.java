package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.DuplicateResourceException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CreateProductCategoryRequest;
import vn.nguongocso.farm.dto.request.UpdateProductCategoryRequest;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.impl.ProductCategoryServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @InjectMocks
    private ProductCategoryServiceImpl productCategoryService;

    private CustomUserDetails adminDetails;
    private CustomUserDetails userDetails;
    private ProductCategory activeCategory;
    private ProductCategory inactiveCategory;

    @BeforeEach
    void setUp() {
        adminDetails = mock(CustomUserDetails.class);
        userDetails = mock(CustomUserDetails.class);

        activeCategory = new ProductCategory(UUID.randomUUID(), "Xoài Cát Chu", "Cây ăn quả", "Xoài chuẩn xuất khẩu", true);
        inactiveCategory = new ProductCategory(UUID.randomUUID(), "Cây Cỏ Ngọt", "Cây công nghiệp", "Bị ẩn do ngưng sản xuất", false);
    }

    @Test
    void search_shouldReturnAllIncludingInactive_whenUserIsAdmin() {
        // Given - Định nghĩa stubbing cục bộ cho test case này
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-01"))).when(adminDetails).getAuthorities();
        when(productCategoryRepository.search("Xoài", null, null))
                .thenReturn(List.of(activeCategory, inactiveCategory));

        // When
        List<ProductCategoryResponse> responses = productCategoryService.search("Xoài", null, null, adminDetails);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Xoài Cát Chu");
        assertThat(responses.get(1).getName()).isEqualTo("Cây Cỏ Ngọt");
        verify(productCategoryRepository, times(1)).search("Xoài", null, null);
    }

    @Test
    void search_shouldOverrideActiveToTrue_whenUserIsNotAdmin() {
        // Given - Định nghĩa stubbing cục bộ cho test case này
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-02"))).when(userDetails).getAuthorities();
        when(productCategoryRepository.search("Xoài", null, true))
                .thenReturn(List.of(activeCategory));

        // When
        List<ProductCategoryResponse> responses = productCategoryService.search("Xoài", null, null, userDetails);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Xoài Cát Chu");
        verify(productCategoryRepository, times(1)).search("Xoài", null, true);
    }

    @Test
    void search_shouldThrowAccessDenied_whenNonAdminSearchsInactive() {
        // Given - Định nghĩa stubbing cục bộ cho test case này
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-02"))).when(userDetails).getAuthorities();

        // When & Then
        assertThatThrownBy(() -> productCategoryService.search("Xoài", null, false, userDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền xem danh mục loại nông sản bị ẩn");

        verify(productCategoryRepository, never()).search(any(), any(), any());
    }

    @Test
    void create_shouldSuccess_whenNameIsNotDuplicated() {
        // Given
        CreateProductCategoryRequest request = new CreateProductCategoryRequest("Cam Sành", "Cây ăn quả", "Cam ngọt");
        when(productCategoryRepository.existsByNameIgnoreCase("Cam Sành")).thenReturn(false);
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductCategoryResponse response = productCategoryService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Cam Sành");
        assertThat(response.getIsActive()).isTrue();
        verify(productCategoryRepository, times(1)).save(any(ProductCategory.class));
    }

    @Test
    void create_shouldThrowDuplicate_whenNameExists() {
        // Given
        CreateProductCategoryRequest request = new CreateProductCategoryRequest("Xoài Cát Chu", "Cây ăn quả", "Trùng tên");
        when(productCategoryRepository.existsByNameIgnoreCase("Xoài Cát Chu")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productCategoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("đã tồn tại trong danh mục");

        verify(productCategoryRepository, never()).save(any());
    }

    @Test
    void update_shouldSuccess_whenValidRequest() {
        // Given
        UUID categoryId = activeCategory.getId();
        UpdateProductCategoryRequest request = new UpdateProductCategoryRequest("Xoài Cát Chu Cao Lãnh", "Cây ăn quả", "Mô tả mới", false);

        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory));
        when(productCategoryRepository.existsByNameIgnoreCaseAndIdNot("Xoài Cát Chu Cao Lãnh", categoryId)).thenReturn(false);
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductCategoryResponse response = productCategoryService.update(categoryId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Xoài Cát Chu Cao Lãnh");
        assertThat(response.getIsActive()).isFalse();
        verify(productCategoryRepository, times(1)).save(activeCategory);
    }

    @Test
    void update_shouldThrowNotFound_whenIdDoesNotExist() {
        // Given
        UUID randomId = UUID.randomUUID();
        UpdateProductCategoryRequest request = new UpdateProductCategoryRequest("Cam", "Cây ăn quả", "Mô tả", true);
        when(productCategoryRepository.findById(randomId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productCategoryService.update(randomId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy loại nông sản với ID");

        verify(productCategoryRepository, never()).save(any());
    }
}
