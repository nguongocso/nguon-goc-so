# API: Gửi phản ánh sản phẩm

*NCL-06-CN-003 — Epic NCL-06: Phản ánh và giám sát chất lượng*

## 1. Thông tin chung

**Mục tiêu**

Cho phép Người tiêu dùng tra cứu (không cần đăng nhập) gửi phản ánh khi nghi ngờ sản phẩm hoặc phát hiện bất thường (ví dụ: nghi ngờ tem giả, sai lệch thông tin) từ trang tra cứu công khai. Phản ánh này sẽ được liên kết trực tiếp với Lô sản xuất (ProductionLot) tương ứng, được lưu trữ vào hệ thống và gửi cảnh báo/thông báo cho Hợp tác xã (chủ sở hữu lô sản xuất) cùng Quản trị viên hệ thống để kiểm tra và xử lý.

**Nhật ký này phục vụ:**

- Thu thập và lưu trữ thông tin phản hồi của người tiêu dùng về chất lượng và tính xác thực của sản phẩm.
- Kịp thời cảnh báo cho Hợp tác xã (tổ chức liên quan) để kiểm tra các hành vi giả mạo hoặc lỗi kỹ thuật trong sản xuất.
- Giúp Quản trị viên (Admin) giám sát chất lượng và tính an toàn của các lô hàng được cấp mã trên hệ thống.

---

## 2. Endpoint

| **Thuộc tính** | **Giá trị** |
| --- | --- |
| **Method** | `POST` |
| **URL** | `/public/api/v1/production-lots/{productionLotId}/feedbacks` |
| **Quyền** | Không yêu cầu đăng nhập (Public access) |

**Path Parameter:**

- `productionLotId` (UUID): ID của lô sản xuất mà người tiêu dùng đang xem và muốn gửi phản ánh.

---

## 3. Request Body

**DTO: CreateProductFeedbackRequest**

| **Trường** | **Kiểu dữ liệu** | **Bắt buộc** | **Ràng buộc / Mô tả** |
| --- | --- | --- | --- |
| `content` | String | Có | `@NotBlank` – "Nội dung phản ánh không được để trống"<br>`@Size(max=1000)` – "Nội dung phản ánh không được vượt quá 1000 ký tự" |

**Ví dụ Request:**

```json
{
  "content": "Tôi quét mã QR trên sản phẩm chè Long Cốc nhưng thấy thông tin ngày thu hoạch không khớp với thực tế bao bì, nghi ngờ sản phẩm bị làm giả."
}
```

---

## 4. Quy tắc nghiệp vụ (Business Rules)

### 4.1 Quy tắc chung (QTN-12)
Người tiêu dùng tra cứu công khai không cần đăng nhập và chỉ được xem thông tin, không có quyền sửa đổi bất kỳ dữ liệu hiện có nào trên hệ thống (chỉ hiển thị chế độ xem). API này chỉ cho phép **tạo mới (gửi)** phản ánh dưới dạng dữ liệu độc lập, không cho phép cập nhật hoặc xóa thông tin lô sản xuất/lô hàng hiện tại.

### 4.2 Kiểm tra tính hợp lệ của dữ liệu (Bean Validation)
- Nội dung phản ánh (`content`) không được phép để trống hoặc chỉ chứa ký tự khoảng trắng. Nếu rỗng, hệ thống sẽ trả về lỗi `400 Bad Request` kèm thông điệp: `"Nội dung phản ánh không được để trống"`.
- Độ dài nội dung phản ánh không vượt quá 1000 ký tự. Nếu vượt quá, hệ thống sẽ trả về lỗi `400 Bad Request`.

### 4.3 Kiểm tra sự tồn tại của Lô sản xuất
- Hệ thống sẽ tìm kiếm lô sản xuất theo `productionLotId` được truyền vào path.
- Nếu không tìm thấy lô sản xuất tương ứng trong hệ thống, trả về lỗi `404 Not Found` kèm thông điệp: `"Không tìm thấy lô sản xuất"`.

### 4.4 Lưu dữ liệu phản ánh
Khi dữ liệu hợp lệ, hệ thống sẽ lưu thông tin phản ánh vào bảng `product_feedbacks` với các giá trị tự động sinh:
- `id`: UUID tự sinh ngẫu nhiên.
- `createdAt`: Thời gian hệ thống ghi nhận (`LocalDateTime.now()`).

### 4.5 Gửi thông báo đến các bên liên quan
- Ngay sau khi phản ánh được lưu thành công, hệ thống sẽ phát đi sự kiện `ProductFeedbackSubmittedEvent` chứa thông tin về phản ánh và lô sản xuất liên quan.
- Listener (`ProductFeedbackListener`) lắng nghe sự kiện này và thực hiện thông báo:
  - Ghi log cảnh báo mức `WARN` chi tiết về nội dung phản ánh, tên lô sản xuất và ID tổ chức sở hữu lô sản xuất (đại diện cho việc thông báo tới hợp tác xã và quản trị viên).

---

## 5. Response

**HTTP 200 OK**

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "e2f1837d-2b47-495e-990a-f3c552084c01",
    "productionLotId": "f86a4deb-0ba3-4f36-9f52-7c8de90a854b",
    "productionLotName": "Lô chè xuân 2026",
    "content": "Tôi quét mã QR trên sản phẩm chè Long Cốc nhưng thấy thông tin ngày thu hoạch không khớp với thực tế bao bì, nghi ngờ sản phẩm bị làm giả.",
    "createdAt": "2026-07-26T15:25:00.123456"
  },
  "timestamp": "2026-07-26T08:25:00.123456000Z"
}
```

---

## 6. Lỗi thường gặp (Error Response)

### 400 Bad Request – Thiếu/Rỗng dữ liệu (NCL-06-CN-003-TC-02)

Trường hợp nội dung phản ánh để trống hoặc chỉ có khoảng trắng:

```json
{
  "success": false,
  "status": 400,
  "message": "Nội dung phản ánh không được để trống"
}
```

### 404 Not Found – Không tìm thấy lô sản xuất

Trường hợp ID lô sản xuất không tồn tại trong hệ thống:

```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy lô sản xuất"
}
```

---

## 7. Backend xử lý

```
Người tiêu dùng (Public)
       │
       ▼
POST /public/api/v1/production-lots/{productionLotId}/feedbacks
       │
       ▼
Bean Validation (@NotBlank, @Size) ----------> 400 Bad Request nếu rỗng/quá dài
       │
       ▼
Tìm kiếm ProductionLot bằng ID ---------------> 404 Not Found nếu không tồn tại
       │
       ▼
Tạo thực thể ProductFeedback
       │
       ▼
Lưu Database (bảng `product_feedbacks`)
       │
       ▼
Phát sự kiện ProductFeedbackSubmittedEvent
       │
       ├──────────────────────────────────────────┐
       ▼ (Xử lý bất đồng bộ)                       ▼ (Trả kết quả)
Lắng nghe sự kiện (ProductFeedbackListener)    Map thành ProductFeedbackResponse
       │                                          │
       ▼                                          ▼
Gửi thông báo (Log cảnh báo chi tiết          Trả Response 200 OK kèm data
HTX & Admin)
```

---

## 8. Thiết kế Cơ sở dữ liệu và Entity

### 8.1 Migration SQL (`V9__create_product_feedbacks_table.sql`)

```sql
CREATE TABLE product_feedbacks (
    id CHAR(36) NOT NULL,
    production_lot_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_feedbacks_production_lot 
        FOREIGN KEY (production_lot_id) REFERENCES production_lot(id)
);

CREATE INDEX idx_product_feedbacks_production_lot ON product_feedbacks(production_lot_id);
```

### 8.2 Entity `ProductFeedback.java`

```java
package vn.nguongocso.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_lot_id", nullable = false)
    private ProductionLot productionLot;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
```

---

## 9. Phạm vi của Story

**Bao gồm:**
- Thiết kế bảng `product_feedbacks` và thực thể JPA `ProductFeedback`.
- Xây dựng API công khai gửi phản ánh kèm ràng buộc validation nội dung phản ánh.
- Thực hiện kiểm tra sự tồn tại của lô sản xuất trước khi lưu phản ánh.
- Phát sự kiện ứng dụng Spring để kết nối thông báo phản ánh.
- Tạo Listener lắng nghe sự kiện để ghi nhận thông báo tới HTX & Admin thông qua log nghiệp vụ.

**Không bao gồm:**
- Xây dựng giao diện hiển thị phản ánh trên trang quản trị.
- Các tính năng duyệt hoặc xử lý trạng thái của phản ánh từ phía Admin/HTX.

---

## 10. Danh sách Test Cases

### TC-01: Gửi phản ánh thành công (NCL-06-CN-003-TC-01)
- **Điều kiện đầu vào:** Mã lô sản xuất hợp lệ, nội dung phản ánh đầy đủ thông tin.
- **Hành động:** Người tiêu dùng nhấn gửi phản ánh.
- **Kết quả mong đợi:** Hệ thống lưu phản ánh vào DB, trả về thông tin phản ánh kèm mã 200 OK. Đồng thời phát sự kiện thông báo.
- **Mức độ:** Cao

### TC-02: Thiếu nội dung phản ánh (NCL-06-CN-003-TC-02)
- **Điều kiện đầu vào:** Mã lô sản xuất hợp lệ, nội dung phản ánh rỗng hoặc chỉ toàn khoảng trắng.
- **Hành động:** Người tiêu dùng nhấn gửi phản ánh.
- **Kết quả mong đợi:** Hệ thống chặn lại, trả về mã 400 Bad Request kèm thông điệp `"Nội dung phản ánh không được để trống"`.
- **Mức độ:** Cao

### TC-03: Kích hoạt thông báo phản ánh (NCL-06-CN-003-TC-03)
- **Điều kiện đầu vào:** Phản ánh gửi thành công.
- **Hành động:** Hệ thống hoàn tất lưu cơ sở dữ liệu.
- **Kết quả mong đợi:** Phát sự kiện thành công và kích hoạt listener để thông báo (ghi log nghiệp vụ cảnh báo mức `WARN` tới HTX sở hữu lô sản xuất và Quản trị viên).
- **Mức độ:** Trung bình
