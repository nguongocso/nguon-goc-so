# API Docs – Xuất hồ sơ truy xuất nguồn gốc

**Tên nhánh:** `feature/export-traceability-dossier`

---

## 1. Kiểm tra điều kiện xuất hồ sơ truy xuất

### Thông tin API

| Thuộc tính   | Giá trị                                             |
|--------------|-----------------------------------------------------|
| **Method**   | `GET`                                               |
| **Endpoint** | `/api/v1/shipments/{shipmentId}/dossier/check`      |
| **Quyền**    | `VT-01`, `VT-02`, `VT-04`                           |

* `VT-01`: Quản trị viên hệ thống (Admin) - Có quyền kiểm tra bất kỳ lô hàng nào.
* `VT-02`: Quản lý hợp tác xã (Cooperative Manager) - Có quyền kiểm tra lô hàng thuộc tổ chức của mình.
* `VT-04`: Doanh nghiệp thu mua (Procurement) - Có quyền kiểm tra lô hàng mình đã ghi nhận thu mua.

---

### Request

**Path Parameter:**

| Parameter    | Kiểu   | Bắt buộc | Mô tả                                      |
|--------------|--------|----------|--------------------------------------------|
| `shipmentId` | UUID   | Có       | ID của lô hàng (Shipment) cần kiểm tra.     |

---

### Response `200 OK` – Đủ điều kiện xuất hồ sơ

```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
    "eligible": true,
    "missingDocuments": []
  },
  "timestamp": "2026-07-29T04:30:00.000Z"
}
```

### Response `400 Bad Request` – Lô hàng chưa hoàn tất hoặc thiếu chứng từ bắt buộc (NCL-07-CN-002-TC-02)

```json
{
  "success": false,
  "status": 400,
  "message": "Không đủ điều kiện xuất hồ sơ truy xuất: Lô hàng chưa hoàn tất hoặc thiếu chứng từ bắt buộc.",
  "errors": [
    "Lô sản xuất tương ứng chưa hoàn tất (Trạng thái yêu cầu: CLOSED hoặc PACKAGED)",
    "Thiếu chứng từ gieo cấy/xuống giống (PLANTING)",
    "Thiếu chứng từ bón phân (FERTILIZING)",
    "Thiếu chứng từ phun thuốc/phòng trừ sâu bệnh (PESTICIDE)",
    "Thiếu chứng từ thu hoạch (HARVESTING)"
  ],
  "timestamp": "2026-07-29T04:31:00.000Z"
}
```

---

## 2. Xuất và tải hồ sơ truy xuất nguồn gốc (PDF)

### Thông tin API

| Thuộc tính   | Giá trị                                             |
|--------------|-----------------------------------------------------|
| **Method**   | `GET`                                               |
| **Endpoint** | `/api/v1/shipments/{shipmentId}/dossier/export`     |
| **Quyền**    | `VT-01`, `VT-02`, `VT-04`                           |

---

### Request

**Path Parameter:**

| Parameter    | Kiểu   | Bắt buộc | Mô tả                                      |
|--------------|--------|----------|--------------------------------------------|
| `shipmentId` | UUID   | Có       | ID của lô hàng (Shipment) cần xuất hồ sơ.  |

---

### Response `200 OK` – Thành công (Tải file PDF)

* **Content-Type:** `application/pdf`
* **Content-Disposition:** `attachment; filename="Ho_so_truy_xuat_<shipment_name>_<yyyyMMdd>.pdf"`
* **Body:** Luồng dữ liệu nhị phân (Binary Stream) của file PDF chứa thông tin hồ sơ tổng hợp.

---

### Lỗi thường gặp

#### 1. Lô hàng không tồn tại
**HTTP Status: 400 Bad Request** / **404 Not Found**
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy thông tin lô hàng.",
  "path": "/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/dossier/export",
  "timestamp": "2026-07-29T04:32:00.000Z"
}
```

#### 2. Không có quyền truy cập lô hàng (NCL-07-CN-002-TC-03)
**HTTP Status: 403 Forbidden**
```json
{
  "success": false,
  "status": 403,
  "message": "Từ chối thao tác: Bạn không có quyền truy cập hoặc xuất hồ sơ cho lô hàng này.",
  "path": "/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/dossier/export",
  "timestamp": "2026-07-29T04:33:00.000Z"
}
```

#### 3. Chưa hoàn tất hoặc thiếu chứng từ (QTN-11)
**HTTP Status: 400 Bad Request**
```json
{
  "success": false,
  "status": 400,
  "message": "Không đủ điều kiện xuất hồ sơ truy xuất.",
  "errors": [
    "Thiếu chứng từ bón phân (FERTILIZING)"
  ],
  "path": "/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/dossier/export",
  "timestamp": "2026-07-29T04:34:00.000Z"
}
```

---

## 3. Quy tắc nghiệp vụ (Business Rules)

### 3.1 Quy tắc QTN-11 – Chỉ xuất hồ sơ khi đủ điều kiện
Hệ thống chỉ cho phép xuất hồ sơ truy xuất khi lô hàng đã hoàn tất sản xuất và có đầy đủ hồ sơ/chứng từ minh chứng:
* **Lô sản xuất đã hoàn tất:** Trạng thái của Lô sản xuất liên kết với lô hàng (`production_lot.status`) phải là `CLOSED` hoặc `PACKAGED`.
* **Đầy đủ chứng từ bắt buộc:** Nhật ký canh tác (`farm_logs`) của lô sản xuất tương ứng phải có tối thiểu 1 chứng từ đính kèm (`farm_log_attachments`) cho mỗi loại hoạt động chính sau:
  1. `PLANTING` (Gieo giống/Xuống giống)
  2. `FERTILIZING` (Bón phân)
  3. `PESTICIDE` (Phun thuốc/Bảo vệ thực vật)
  4. `HARVESTING` (Thu hoạch)
* Nếu thiếu bất kỳ điều kiện nào ở trên, hệ thống sẽ chặn xuất và trả về danh sách liệt kê chi tiết các phần còn thiếu.

### 3.2 Phân quyền cách ly dữ liệu (NCL-07-CN-002-TC-03)
Hệ thống đảm bảo tính bảo mật và cách ly thông tin giữa các đơn vị tham gia chuỗi cung ứng:
* **Quản trị viên (VT-01):** Có quyền xem và xuất hồ sơ cho toàn bộ lô hàng trên hệ thống.
* **Quản lý hợp tác xã (VT-02):** Chỉ được quyền xuất hồ sơ của lô hàng thuộc sở hữu của hợp tác xã mình (`shipment.organization_id` bằng ID tổ chức của user đăng nhập).
* **Doanh nghiệp thu mua (VT-04):** Chỉ được quyền xuất hồ sơ của lô hàng mà doanh nghiệp mình đã thực hiện tiếp nhận/thu mua (tồn tại sự kiện `ChainEventType.PROCUREMENT` tương ứng của lô hàng đó được ghi nhận bởi người dùng thuộc cùng tổ chức của doanh nghiệp thu mua).
* Các vai trò còn lại (`VT-03`, `VT-05`, `VT-06`) không được phép thực hiện thao tác này.

### 3.3 Lưu nhật ký xuất hồ sơ (NCL-07-CN-002-TC-04)
Khi xuất hồ sơ thành công, hệ thống bắt buộc tự động ghi nhận nhật ký xuất hồ sơ vào bảng `dossier_export_history` bao gồm:
* ID người xuất (`exporter_id`)
* ID tổ chức của người xuất (`organization_id`)
* ID lô hàng được xuất (`shipment_id`)
* Thời điểm xuất (`exported_at`)
* Tên tệp hồ sơ xuất ra (`file_name`)
* Địa chỉ IP client thực hiện (`ip_address`)
* Trạng thái xuất (`status` = `SUCCESS` hoặc `FAILED`)

---

## 4. Thiết kế Cơ sở dữ liệu

### Bảng `dossier_export_history` (Lịch sử xuất hồ sơ truy xuất)

| Tên trường           | Kiểu dữ liệu   | Nullable | Khóa | Mô tả                                                                    |
|----------------------|----------------|----------|------|--------------------------------------------------------------------------|
| `id`                 | `CHAR(36)`     | NO       | PK   | Khóa chính (UUID tự sinh)                                                |
| `shipment_id`        | `CHAR(36)`     | NO       | FK   | ID lô hàng được xuất hồ sơ (Khóa ngoại đến `shipments.id`)               |
| `exporter_id`        | `CHAR(36)`     | NO       | FK   | ID người dùng thực hiện xuất hồ sơ (Khóa ngoại đến `users.user_id`)       |
| `organization_id`    | `CHAR(36)`     | NO       | FK   | ID tổ chức của người xuất (Khóa ngoại đến `organizations.organization_id`)|
| `exported_at`        | `DATETIME`     | NO       | –    | Thời điểm thực hiện xuất hồ sơ                                           |
| `file_name`          | `VARCHAR(255)` | NO       | –    | Tên file hồ sơ xuất ra (VD: `Ho_so_truy_xuat_Che_Long_Coc_20260729.pdf`) |
| `file_size`          | `BIGINT`       | YES      | –    | Dung lượng file xuất ra (tính bằng byte)                                 |
| `status`             | `VARCHAR(50)`  | NO       | –    | Trạng thái xuất (`SUCCESS` hoặc `FAILED`)                                |
| `ip_address`         | `VARCHAR(45)`  | YES      | –    | Địa chỉ IP của client gửi yêu cầu                                        |

#### Mã SQL Migration (`V11__create_dossier_export_history.sql`)

```sql
CREATE TABLE dossier_export_history (
    id CHAR(36) NOT NULL,
    shipment_id CHAR(36) NOT NULL,
    exporter_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    exported_at DATETIME NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NULL,
    status VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_dossier_export_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_dossier_export_exporter FOREIGN KEY (exporter_id) REFERENCES users(user_id),
    CONSTRAINT fk_dossier_export_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id)
);

CREATE INDEX idx_dossier_export_shipment ON dossier_export_history(shipment_id);
CREATE INDEX idx_dossier_export_exporter ON dossier_export_history(exporter_id);
```

---

## 5. Tiêu chí nghiệm thu (Acceptance Criteria)

* **AC-01 (Kiểm tra điều kiện):** API `/check` kiểm tra đúng trạng thái của Lô sản xuất (`CLOSED` hoặc `PACKAGED`) và sự tồn tại chứng từ bắt buộc cho các hoạt động: `PLANTING`, `FERTILIZING`, `PESTICIDE`, `HARVESTING`.
* **AC-02 (Liệt kê chứng từ thiếu):** Nếu thiếu bất kỳ chứng từ bắt buộc nào, API trả về mã lỗi `400 Bad Request` và chứa danh sách cụ thể các loại chứng từ còn thiếu.
* **AC-03 (Kiểm soát quyền truy cập):** 
  * Từ chối truy cập và trả về lỗi `403 Forbidden` đối với các vai trò không được cấu hình quyền (`VT-03`, `VT-05`, `VT-06`).
  * Trả về lỗi `403 Forbidden` nếu tài khoản doanh nghiệp thu mua (`VT-04`) hoặc quản lý HTX (`VT-02`) cố gắng xem hoặc xuất lô hàng không thuộc phạm vi sở hữu/quản lý của đơn vị mình.
* **AC-04 (Tổng hợp & Xuất tệp PDF):** Tệp PDF xuất ra phải chứa đầy đủ thông tin:
  * Thông tin chi tiết lô sản xuất (Tên, ngày xuống giống, ngày thu hoạch, sản lượng thực tế, giống cây trồng).
  * Lịch trình nhật ký canh tác (Các hoạt động sản xuất đã ghi nhận kèm vật tư, số lượng, thời gian).
  * Danh sách tài liệu/chứng từ minh chứng (Farm Log Attachments) đã tải lên.
  * Lịch sử sự kiện chuỗi cung ứng (Chain Events) từ khi thu hoạch, đóng gói, vận chuyển đến khi thu mua.
* **AC-05 (Lưu nhật ký xuất):** Mọi lượt xuất hồ sơ thành công đều tự động tạo một bản ghi lưu trữ thông tin chi tiết vào bảng `dossier_export_history`.
