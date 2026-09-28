package vn.nguongocso.farm.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.CreateProductCategoryRequest;
import vn.nguongocso.farm.dto.request.UpdateProductCategoryRequest;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;

/**
 * Nghiệp vụ loại cây trồng.
*/
public interface ProductCategoryService {
    /** Lấy danh sách loại cây trồng. */
    List<ProductCategoryResponse> getAll();

    /** Tìm kiếm loại cây trồng. */
    List<ProductCategoryResponse> search(String name, String group, Boolean isActive, CustomUserDetails currentUser);

    /** Tạo loại cây trồng. */
    ProductCategoryResponse create(CreateProductCategoryRequest request);

    /** Cập nhật loại cây trồng. */
    ProductCategoryResponse update(UUID id, UpdateProductCategoryRequest request);
}