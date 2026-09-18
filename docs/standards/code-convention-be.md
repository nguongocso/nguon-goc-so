# Quy Ước Lập Trình Backend (Java 21 / Spring Boot 3) - Dự Án Nguồn Gốc Số

Tài liệu này định nghĩa bộ quy chuẩn lập trình, định dạng mã nguồn và tổ chức kiến trúc áp dụng bắt buộc cho toàn bộ mã nguồn Backend của dự án **Nguồn Gốc Số**. Mọi thành viên và AI Agent khi viết hoặc review code phải tuân thủ nghiêm ngặt các quy định này.

---

## 1. Triết Lý Clean Code & Nguyên Tắc Chung

1. **Code tự mô tả (Self-documenting Code)**: Viết code hướng đến tính dễ đọc, tường minh và trong sáng trước khi tối ưu hóa. Tên biến, phương thức và lớp phải tự nói lên mục đích của nó.
2. **Không bẩn mã nguồn**: Nghiêm cấm để lại mã chết (dead code), code bị comment lại, mã thừa, hoặc các lệnh debug trong mã nguồn khi gửi Pull Request.
3. **Tuân thủ SOLID & SRP (Single Responsibility Principle)**: Mỗi hàm, mỗi lớp chỉ đảm nhiệm đúng một trách nhiệm duy nhất.
4. **Không duplicate code (DRY)**: Tái sử dụng logic dùng chung qua service hoặc utility chuyên biệt.
5. **Đồng nhất Formatter**: Sử dụng UTF-8, thụt lề 4 khoảng trắng (Spaces), không dùng Tab.

---

## 2. Quy Chuẩn Xuống Dòng (Line Wrapping) & Cách Dòng Trống (Vertical Blank Lines)

### 2.1. Quy tắc giới hạn độ dài & Xuống dòng (Line Wrapping)
* **Độ dài tối đa**: Tối đa **120 ký tự/dòng**. Bất kỳ câu lệnh nào vượt quá 120 ký tự đều phải xuống dòng.
* **Method Chaining / Stream API / Builder**:
  * Luôn xuống dòng **TRƯỚC dấu chấm `.`**, thụt lề thêm 1 cấp (4 spaces).
  ```java
  // ĐÚNG
  return userRepository.findById(userId)
          .map(userMapper::toResponse)
          .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

  // SAI (Dồn trên 1 dòng dài hoặc ngắt sau dấu chấm)
  return userRepository.findById(userId).map(userMapper::toResponse).orElseThrow(() -> ...);
  ```
* **Biểu thức điều kiện & Toán tử logic**:
  * Luôn xuống dòng **TRƯỚC toán tử** (`&&`, `||`, `+`, toán tử 3 ngôi `?` và `:`), thụt lề thêm 1 cấp.
  ```java
  // ĐÚNG
  if (order.getStatus() == OrderStatus.PENDING
          && order.getTotalAmount().compareTo(maxLimit) > 0
          && !user.isVip()) {
      applyVerification();
  }
  ```
* **Khai báo phương thức có nhiều tham số (Method Signature)**:
  * Xuống dòng **SAU dấu phẩy `,`**, mỗi tham số trên 1 dòng riêng và thụt lề thẳng hàng hoặc 8 spaces.
  ```java
  public ProductResponse createProduct(
          @Valid @RequestBody ProductCreateRequest request,
          @AuthenticationPrincipal CustomUserDetails userDetails,
          HttpServletRequest httpRequest) {
      // ...
  }
  ```
* **Annotations**:
  * Mỗi annotation có tham số hoặc nhiều thuộc tính phải nằm trên **1 dòng riêng biệt**.
  * Không viết gộp nhiều annotation phức tạp trên cùng một dòng với khai báo biến/phương thức.
  ```java
  // ĐÚNG
  @NotNull(message = "Danh mục nông sản không được để trống")
  @Min(value = 1, message = "Số lượt quét phải >= 1")
  private Integer maxScansPerHour;

  // SAI
  @NotNull(message = "...") @Min(value = 1, message = "...") private Integer maxScansPerHour;
  ```

### 2.2. Quy tắc Cách dòng trống (Vertical Blank Lines)
* **1 dòng trống** giữa:
  * Khai báo `package` và khối lệnh `import`.
  * Các nhóm import (nhóm `java.*`, nhóm thư viện ngoài `jakarta.*`/`org.springframework.*`, nhóm nội bộ `vn.nguongocso.*`).
  * Khai báo class và thuộc tính đầu tiên.
  * **Giữa các fields trong DTO / Entity có annotations**: Để tránh dính chùm khó đọc, bắt buộc có 1 dòng trống ngăn cách giữa các thuộc tính có kèm annotation validation/column.
  * Giữa các Constructors và Phương thức (Methods).
  * Giữa các khối logic riêng biệt trong một phương thức (Khối kiểm tra tham số -> *(1 dòng trống)* -> Khối xử lý tính toán -> *(1 dòng trống)* -> Khối gọi repository -> *(1 dòng trống)* -> Khối return).
* **Tuyệt đối CẤM**:
  * ❌ **Cấm 2 dòng trống liên tiếp** ở bất kỳ vị trí nào trong mã nguồn.
  * ❌ **Cấm dòng trống** ngay sau dấu mở ngoặc `{` hoặc ngay trước dấu đóng ngoặc `}`.
  * ❌ **Cấm dòng trống thừa** ở đầu file hoặc cuối file (kết thúc file bằng đúng 1 newline `\n` chuẩn POSIX).

---

## 3. Quy Chuẩn Vàng Về Javadoc & Comment (Clean Commenting)

> **Khẩu quyết**: *"Hãy để mã nguồn tự giải thích. Comment chỉ dùng để giải thích TẠI SAO (Why), tuyệt đối không giải thích LÀM GÌ (What)."*

### 3.1. Giới hạn độ dài và nội dung Javadoc
* **Class / Interface Javadoc (Tối đa 3 - 5 dòng)**:
  * Bắt buộc viết bằng **tiếng Việt có dấu**.
  * Nêu đúng vai trò nghiệp vụ của Class và kèm mã chức năng / User Story ID (nếu có, ví dụ `NCL-08-CN-014`).
  ```java
  /**
   * Xử lý cấu hình ghi đè ngưỡng cảnh báo theo danh mục nông sản (NCL-08-CN-014).
   * Cung cấp các thao tác tạo mới, cập nhật và tra cứu cấu hình ngưỡng.
   */
  ```
* **Method Javadoc (Tối đa 3 - 5 dòng)**:
  * 1 câu ngắn gọn mô tả mục đích hành vi nghiệp vụ.
  * Thẻ `@param`, `@return`, `@throws` ngắn gọn, đúng trọng tâm.
  * **Không viết Javadoc cho getter, setter** hoặc các phương thức Override/cú pháp hiển nhiên.
  ```java
  /**
   * Tính toán khoảng cách tọa độ địa lý giữa hai lần quét mã QR.
   *
   * @param prevLocation Tọa độ lần quét trước
   * @param currLocation Tọa độ lần quét hiện tại
   * @return Khoảng cách tính bằng kilomet (km)
   * @throws InvalidCoordinateException nếu tọa độ không hợp lệ
   */
  ```
* **Field Javadoc (Tối đa 1 dòng)**:
  * **KHÔNG COMMENT** nếu tên thuộc tính đã rõ ràng (như `email`, `createdAt`, `phoneNumber`).
  * Chỉ comment 1 dòng ngắn khi có thuật ngữ chuyên môn hoặc đơn vị tính (`kg`, `ms`, `vnd`).

### 3.2. Nghiêm cấm Noise Comments (Comment rác) & Dead Code
* ❌ **Cấm comment giải thích cú pháp hiển nhiên**:
  ```java
  // SAI: Comment rác làm bẩn mã nguồn
  // Tăng biến đếm
  count++;
  // Khởi tạo danh sách người dùng
  List<User> users = new ArrayList<>();
  // Kiểm tra nếu null
  if (user == null) { ... }
  ```
* ❌ **Cấm Dead Code**: Không được để lại các dòng code cũ bị comment (`// oldCalculation();`). Bắt buộc xóa bỏ hoàn toàn trước khi tạo PR vì Git đã lưu lại lịch sử.
* ❌ **Cấm TODO / FIXME sót lại**: Trước khi merge vào `develop`/`main`, mọi `// TODO:` phải được giải quyết dứt điểm.
* **Inline Comment hợp lệ**: Chỉ viết tối đa 1-2 dòng khi cần giải thích một quy tắc nghiệp vụ đặc thù hoặc ngoại lệ khó hiểu (VD: `// Theo quy định của Bộ NN&PTNT, mã truy xuất nguồn gốc phải có tiền tố XA-`).

---

## 4. Quy Chuẩn Đặt Tên (Naming Convention)

| Thành phần | Quy tắc | Ví dụ ĐÚNG | Ví dụ SAI |
|---|---|---|---|
| **Ngôn ngữ** | Tiếng Anh 100% | `totalPrice`, `isDeleted` | `tongTien`, `daXoa` |
| **Package** | Viết thường toàn bộ, danh từ | `vn.nguongocso.alert.controller` | `vn.nguongocso.alert.Controller` |
| **Class** | PascalCase, danh từ | `AlertThresholdService`, `ProductEntity` | `alertThresholdService`, `Alert_Threshold` |
| **Interface** | PascalCase, **không có tiền tố I** | `AlertService`, `PaymentStrategy` | `IAlertService`, `I_PaymentStrategy` |
| **Method** | camelCase, bắt đầu bằng động từ | `calculateDistance()`, `findUserById()` | `distance()`, `user()`, `doCheck()` |
| **Variable** | camelCase, danh từ rõ nghĩa | `pendingOrders`, `currentUser` | `data`, `temp`, `list`, `obj`, `item` |
| **Constant** | UPPER_SNAKE_CASE | `MAX_SCAN_PER_HOUR`, `DEFAULT_PAGE_SIZE` | `maxScanPerHour`, `default_page_size` |
| **Enum Type** | PascalCase | `AlertSeverity`, `OrderStatus` | `alert_severity`, `ORDER_STATUS` |
| **Enum Value**| UPPER_SNAKE_CASE | `CRITICAL`, `WARNING`, `INFO` | `Critical`, `Warning` |

---

## 5. Quy Chuẩn Chi Tiết Theo Từng Tầng Kiến Trúc (Layer-by-Layer)

### 5.1. Tầng Entity (`vn.nguongocso.<module>.entity.*Entity.java`)
* **Mục đích**: Ánh xạ bảng cơ sở dữ liệu MySQL.
* **Quy tắc**:
  * Phải có Javadoc tiếng Việt (1-3 dòng) mô tả bảng.
  * Sử dụng `@Getter`, `@Setter`, `@NoArgsConstructor`. **NGHIÊM CẤM sử dụng `@Data` trên JPA Entity** (gây lỗi `equals`/`hashCode`/`toString` trong quan hệ vòng lặp và Lazy Loading).
  * Kế thừa `BaseAuditEntity` để tự động ghi nhận `createdAt`, `updatedAt`, `createdBy`, `updatedBy` (nếu bảng có audit).
  * Đặt tên bảng rõ ràng bằng `@Table(name = "tbl_...")`.
  * Khai báo `@Id` rõ ràng (dùng `UUID` hoặc `Long`).
  * ❌ **Cấm**: Không chứa logic tính toán nghiệp vụ, không trả Entity trực tiếp ra ngoài Controller/API.

```java
package vn.nguongocso.alert.entity;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.common.entity.BaseAuditEntity;

/**
 * Thực thể lưu trữ cấu hình ngưỡng cảnh báo quét mã QR theo danh mục nông sản.
 */
@Entity
@Table(name = "tbl_category_threshold_override")
@Getter
@Setter
@NoArgsConstructor
public class CategoryThresholdOverrideEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_category_id", nullable = false)
    private UUID productCategoryId;

    @Column(name = "max_scans_per_hour", nullable = false)
    private Integer maxScansPerHour;

    @Column(name = "max_scans_per_day", nullable = false)
    private Integer maxScansPerDay;
}
```

---

### 5.2. Tầng DTO (`vn.nguongocso.<module>.dto.request.*` / `dto.response.*`)
* **Mục đích**: Nhận dữ liệu từ client (Request) hoặc định hình dữ liệu trả về cho client (Response).
* **Quy tắc**:
  * Phải có Javadoc tiếng Việt (1-2 dòng) nêu mục đích và mã User Story.
  * Sử dụng `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`.
  * **Validation bắt buộc**: Các trường dữ liệu đầu vào trong Request phải được kiểm tra bằng Jakarta Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern`).
  * **Thông điệp lỗi tiếng Việt**: Mọi annotation validation phải có thuộc tính `message` bằng tiếng Việt rõ ràng, thân thiện.
  * Bắt buộc có **1 dòng trống giữa các field có annotation** để code thoáng, dễ đọc.
  * ❌ **Cấm**: Không để lộ thông tin nhạy cảm (mật khẩu băm, token bí mật) trong Response DTO.

```java
package vn.nguongocso.alert.dto.request;

import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu tạo mới hoặc cập nhật cấu hình ghi đè ngưỡng theo danh mục nông sản (NCL-08-CN-014).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryThresholdOverrideRequest {

    @NotNull(message = "Danh mục nông sản không được để trống")
    private UUID productCategoryId;

    @NotNull(message = "Số lượt quét tối đa mỗi giờ không được để trống")
    @Min(value = 1, message = "Số lượt quét tối đa mỗi giờ phải lớn hơn hoặc bằng 1")
    private Integer maxScansPerHour;

    @NotNull(message = "Số lượt quét tối đa mỗi ngày không được để trống")
    @Min(value = 1, message = "Số lượt quét tối đa mỗi ngày phải lớn hơn hoặc bằng 1")
    private Integer maxScansPerDay;
}
```

---

### 5.3. Tầng Mapper (`vn.nguongocso.<module>.mapper.*Mapper.java`)
* **Mục đích**: Chuyển đổi qua lại giữa Entity và DTO.
* **Quy tắc**:
  * Dùng thư viện MapStruct với cấu hình `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`.
  * ❌ **Cấm**: Không viết code convert thủ công (getter/setter rải rác) lặp đi lặp lại trong Service khi đã có MapStruct.

```java
package vn.nguongocso.alert.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.response.CategoryThresholdOverrideResponse;
import vn.nguongocso.alert.entity.CategoryThresholdOverrideEntity;

/**
 * Mapper chuyển đổi dữ liệu cấu hình ngưỡng cảnh báo giữa Entity và DTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryThresholdOverrideMapper {

    CategoryThresholdOverrideEntity toEntity(CategoryThresholdOverrideRequest request);

    CategoryThresholdOverrideResponse toResponse(CategoryThresholdOverrideEntity entity);

    void updateEntityFromRequest(CategoryThresholdOverrideRequest request, @MappingTarget CategoryThresholdOverrideEntity entity);
}
```

---

### 5.4. Tầng Repository (`vn.nguongocso.<module>.repository.*Repository.java`)
* **Mục đích**: Thao tác truy vấn và tương tác với cơ sở dữ liệu.
* **Quy tắc**:
  * Kế thừa `JpaRepository<Entity, ID>` hoặc `JpaSpecificationExecutor<Entity>`.
  * Đặt tên method truy vấn theo chuẩn Spring Data JPA (`findBy...`, `existsBy...`, `countBy...`).
  * Nếu dùng `@Query`, phải truyền tham số bằng `@Param("...")`.
  * Các câu truy vấn trả về danh sách nhiều bản ghi bắt buộc nhận tham số `Pageable` để tránh quá tải bộ nhớ (OOM).
  * ❌ **Cấm**: Tuyệt đối không cộng chuỗi SQL trong Native Query (phòng chống lỗ hổng SQL Injection).

```java
package vn.nguongocso.alert.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.alert.entity.CategoryThresholdOverrideEntity;

/**
 * Repository quản lý truy vấn dữ liệu cấu hình ghi đè ngưỡng cảnh báo.
 */
@Repository
public interface CategoryThresholdOverrideRepository extends JpaRepository<CategoryThresholdOverrideEntity, UUID> {

    Optional<CategoryThresholdOverrideEntity> findByProductCategoryId(UUID productCategoryId);

    @Query("SELECT c FROM CategoryThresholdOverrideEntity c WHERE (:keyword IS NULL OR c.productCategoryId = :keyword)")
    Page<CategoryThresholdOverrideEntity> searchOverrides(@Param("keyword") UUID keyword, Pageable pageable);
}
```

---

### 5.5. Tầng Service (`*Service.java` & `*ServiceImpl.java`)
* **Mục đích**: Chứa toàn bộ nghiệp vụ xử lý logic (Business Logic) của hệ thống.
* **Quy tắc**:
  * Luôn tách bạch giữa Interface (`*Service`) và Implementation (`*ServiceImpl`).
  * Đặt `@Transactional(readOnly = true)` ở mức class, và đánh dấu `@Transactional` tại các method thực hiện ghi/sửa/xóa dữ liệu.
  * Giới hạn kích thước phương thức: **Không quá 30 dòng/hàm**. Nếu dài hơn, phải tách thành các hàm con private có tên rõ nghĩa.
  * Giới hạn kích thước lớp: **Khuyến nghị dưới 300 dòng, tối đa 500 dòng**.
  * Bắn ngoại lệ nghiệp vụ cụ thể (`BusinessException`, `ResourceNotFoundException`) kèm thông điệp tiếng Việt có dấu.
  * ❌ **Cấm**:
    * Không nuốt ngoại lệ (không dùng khối `catch (Exception e) {}` rỗng).
    * Không dùng `System.out.println()` để debug, bắt buộc dùng logging SLF4J (`log.info`, `log.warn`, `log.error`).

```java
package vn.nguongocso.alert.service.impl;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.response.CategoryThresholdOverrideResponse;
import vn.nguongocso.alert.entity.CategoryThresholdOverrideEntity;
import vn.nguongocso.alert.mapper.CategoryThresholdOverrideMapper;
import vn.nguongocso.alert.repository.CategoryThresholdOverrideRepository;
import vn.nguongocso.alert.service.CategoryThresholdOverrideService;
import vn.nguongocso.common.exception.ResourceNotFoundException;

/**
 * Triển khai xử lý nghiệp vụ cấu hình ghi đè ngưỡng cảnh báo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryThresholdOverrideServiceImpl implements CategoryThresholdOverrideService {

    private final CategoryThresholdOverrideRepository repository;
    private final CategoryThresholdOverrideMapper mapper;

    @Override
    @Transactional
    public CategoryThresholdOverrideResponse createOrUpdate(CategoryThresholdOverrideRequest request) {
        log.info("Bắt đầu cấu hình ngưỡng cho danh mục: {}", request.getProductCategoryId());

        CategoryThresholdOverrideEntity entity = repository.findByProductCategoryId(request.getProductCategoryId())
                .map(existing -> {
                    mapper.updateEntityFromRequest(request, existing);
                    return existing;
                })
                .orElseGet(() -> {
                    CategoryThresholdOverrideEntity newEntity = mapper.toEntity(request);
                    newEntity.setId(UUID.randomUUID());
                    return newEntity;
                });

        CategoryThresholdOverrideEntity savedEntity = repository.save(entity);
        return mapper.toResponse(savedEntity);
    }

    @Override
    public CategoryThresholdOverrideResponse getByCategoryId(UUID categoryId) {
        return repository.findByProductCategoryId(categoryId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình ngưỡng cho danh mục đã chọn"));
    }
}
```

---

### 5.6. Tầng Controller (`vn.nguongocso.<module>.controller.*Controller.java`)
* **Mục đích**: Tiếp nhận request HTTP, điều phối xác thực dữ liệu và trả kết quả phản hồi.
* **Quy tắc**:
  * Đánh dấu `@RestController`, tiền tố đường dẫn `@RequestMapping("/api/v1/...")`.
  * Có Javadoc tiếng Việt tóm tắt API, tài liệu hóa bằng Swagger/OpenAPI `@Operation`, `@ApiResponse`.
  * Tham số request body bắt buộc có `@Valid`.
  * Kết quả trả về phải bọc trong chuẩn chung `ResponseEntity<ApiResponse<T>>`.
  * ❌ **Cấm**: Tuyệt đối không chứa logic tính toán nghiệp vụ trong Controller (Controller chỉ gọi Service và return).

```java
package vn.nguongocso.alert.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.response.CategoryThresholdOverrideResponse;
import vn.nguongocso.alert.service.CategoryThresholdOverrideService;
import vn.nguongocso.common.response.ApiResponse;

/**
 * Controller cung cấp API quản lý cấu hình ngưỡng cảnh báo theo danh mục nông sản (NCL-08-CN-014).
 */
@Tag(name = "Cấu hình ngưỡng cảnh báo", description = "Quản lý ghi đè ngưỡng cảnh báo quét QR")
@RestController
@RequestMapping("/api/v1/alert-thresholds/categories")
@RequiredArgsConstructor
public class CategoryThresholdOverrideController {

    private final CategoryThresholdOverrideService service;

    @Operation(summary = "Lưu hoặc cập nhật cấu hình ngưỡng cho danh mục")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryThresholdOverrideResponse>> saveThreshold(
            @Valid @RequestBody CategoryThresholdOverrideRequest request) {
        CategoryThresholdOverrideResponse response = service.createOrUpdate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lưu cấu hình ngưỡng cảnh báo thành công", response));
    }

    @Operation(summary = "Lấy thông tin cấu hình ngưỡng theo mã danh mục")
    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryThresholdOverrideResponse>> getThreshold(
            @PathVariable UUID categoryId) {
        CategoryThresholdOverrideResponse response = service.getByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Truy vấn cấu hình thành công", response));
    }
}
```

---

## 6. Bảng Tóm Tắt Tiêu Chuẩn Nhanh Cho Developer & AI Reviewer

| Tiêu chí | Chuẩn quy định | Mức độ vi phạm nếu không đạt |
|---|---|---|
| Ngôn ngữ code & đặt tên | Tiếng Anh 100%, chuẩn camelCase/PascalCase | **Major** |
| Ngôn ngữ Javadoc & Message | Tiếng Việt có dấu, thông điệp thân thiện | **Major** |
| Giới hạn Javadoc | ≤ 3 - 5 dòng cho Class/Method, cấm getter/setter | **Minor / Clutter** |
| Noise comments / Dead code | Không có comment hiển nhiên, cấm code cũ bị comment | **Major / Clean Code** |
| Giới hạn độ dài dòng | ≤ 120 ký tự/dòng, xuống dòng trước toán tử / builder | **Minor** |
| Khoảng cách dòng trống | 1 dòng giữa methods, giữa fields có annotation; cấm 2 dòng trống liên tiếp | **Minor** |
| Kích thước phương thức | ≤ 30 dòng/hàm, 1 nhiệm vụ duy nhất (SRP) | **Major** |
| Jakarta Validation trong DTO | Có đầy đủ `@NotNull`, `@Min`,... kèm message tiếng Việt | **Blocker** |
| Entity & DTO | Không dùng `@Data` trên Entity; không trả Entity ra API | **Blocker** |
| Xử lý ngoại lệ | Bắn Exception cụ thể, cấm khối `catch` rỗng | **Blocker** |
| Logging | Sử dụng SLF4J `log.info`/`log.error`, cấm `System.out.println()` | **Blocker** |
| Bảo mật cơ sở dữ liệu | Cấm nối chuỗi SQL, dùng `@Param` | **Blocker** |
