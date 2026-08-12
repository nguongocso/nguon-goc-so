package vn.nguongocso.farm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.farm.dto.request.CreateProductCategoryRequest;
import vn.nguongocso.farm.dto.request.UpdateProductCategoryRequest;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;
import vn.nguongocso.farm.service.ProductCategoryService;
import vn.nguongocso.permission.service.PermissionChecker;

@WebMvcTest(ProductCategoryController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ProductCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductCategoryService productCategoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    private CustomUserDetails adminDetails;
    private CustomUserDetails userDetails;
    private ProductCategoryResponse activeResponse;

    @BeforeEach
    void setUp() {
        // Thiết lập Mock User Details
        adminDetails = mock(CustomUserDetails.class);
        when(adminDetails.getUsername()).thenReturn("admin");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-01"))).when(adminDetails).getAuthorities();

        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUsername()).thenReturn("user");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-02"))).when(userDetails).getAuthorities();

        activeResponse = ProductCategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Xoài Cát Chu")
                .group("Cây ăn quả")
                .description("Mô tả")
                .isActive(true)
                .build();
    }

    @Test
    void search_shouldReturnOk_whenUserIsAuthenticated() throws Exception {
        // Given
        when(productCategoryService.search(any(), any(), any(), any())).thenReturn(List.of(activeResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/product-categories")
                        .with(user(userDetails))
                        .param("name", "Xoài"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Xoài Cát Chu"));
    }

    @Test
    void create_shouldReturnCreated_whenUserIsAdmin() throws Exception {
        // Given
        CreateProductCategoryRequest request = CreateProductCategoryRequest.builder()
                .name("Mận An Phước")
                .group("Cây ăn quả")
                .description("Mận ngon")
                .build();
        ProductCategoryResponse response = ProductCategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Mận An Phước")
                .group("Cây ăn quả")
                .description("Mận ngon")
                .isActive(true)
                .build();

        when(productCategoryService.create(any(CreateProductCategoryRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/product-categories")
                        .with(csrf())
                        .with(user(adminDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Mận An Phước"));
    }

    @Test
    void create_shouldReturnForbidden_whenUserIsNotAdmin() throws Exception {
        // Given
        CreateProductCategoryRequest request = CreateProductCategoryRequest.builder()
                .name("Mận An Phước")
                .group("Cây ăn quả")
                .description("Mận ngon")
                .build();

        // When & Then (Sử dụng user thường VT-02)
        mockMvc.perform(post("/api/v1/product-categories")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productCategoryService, never()).create(any());
    }

    @Test
    void update_shouldReturnOk_whenUserIsAdmin() throws Exception {
        // Given
        UUID categoryId = activeResponse.getId();
        UpdateProductCategoryRequest request = UpdateProductCategoryRequest.builder()
                .name("Xoài Cao Lãnh")
                .group("Cây ăn quả")
                .description("Mô tả mới")
                .isActive(false)
                .build();
        ProductCategoryResponse response = ProductCategoryResponse.builder()
                .id(categoryId)
                .name("Xoài Cao Lãnh")
                .group("Cây ăn quả")
                .description("Mô tả mới")
                .isActive(false)
                .build();

        when(productCategoryService.update(eq(categoryId), any(UpdateProductCategoryRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/product-categories/{id}", categoryId)
                        .with(csrf())
                        .with(user(adminDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Xoài Cao Lãnh"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void update_shouldReturnForbidden_whenUserIsNotAdmin() throws Exception {
        // Given
        UUID categoryId = activeResponse.getId();
        UpdateProductCategoryRequest request = UpdateProductCategoryRequest.builder()
                .name("Xoài Cao Lãnh")
                .group("Cây ăn quả")
                .description("Mô tả mới")
                .isActive(false)
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/product-categories/{id}", categoryId)
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productCategoryService, never()).update(any(), any());
    }
}
