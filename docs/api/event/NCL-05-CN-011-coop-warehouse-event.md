# API Docs — Ghi sự kiện nhập kho và xuất kho tại hợp tác xã (Cập nhật đối tượng Lô hàng - Shipment)

*Mã User Story: NCL-05-CN-011 Ghi sự kiện nhập kho và xuất kho tại hợp tác xã*

---

## 1. Thông tin chung

**Mục tiêu**

Cho phép Người ghi sự kiện (`VT-03` / `EVENT_RECORDER`) hoặc Quản lý hợp tác xã (`VT-02` / `COOPERATIVE_MANAGER`) ghi nhận hoạt động cho Lô hàng (`Shipment`):
1. **Nhập kho HTX (`WAREHOUSE_ENTRY`)**: Ghi nhận thời điểm lô hàng (`Shipment`) vào kho HTX, tên kho và điều kiện bảo quản.
2. **Xuất kho HTX (`WAREHOUSE_EXIT`)**: Ghi nhận thời điểm lô hàng (`Shipment`) rời kho HTX để chuyển đi.

Hệ thống sẽ tự động tính toán **thời gian lưu kho (storage duration)** giữa thời điểm nhập kho và xuất kho. Nếu thời gian lưu kho vượt quá ngưỡng thời gian bảo quản tối đa quy định cho loại nông sản (`maxStorageDays` của `ProductCategory` thuộc Lô sản xuất tương ứng), hệ thống sẽ tự động đánh dấu cảnh báo vượt ngưỡng trên dòng sự kiện và trên trang tra cứu công khai tem QR.

**Ràng buộc nghiệp vụ:**
- **TC-01 (Luồng thành công)**: Ghi nhập kho Lô hàng, sau đó ghi xuất kho Lô hàng -> tính và hiển thị đúng thời gian lưu kho.
- **TC-02 (Sai trạng thái)**: Không cho ghi xuất kho khi Lô hàng chưa có sự kiện nhập kho.
- **TC-03 (Ngoại lệ / Cảnh báo)**: Đánh dấu cảnh báo khi thời gian lưu kho vượt ngưỡng bảo quản khai báo cho loại nông sản.
- **TC-04 (Dữ liệu trùng lặp)**: Chặn không cho ghi hai lần nhập kho liên tiếp cho cùng Lô hàng khi chưa xuất kho.
- **QTN-08**: Dòng sự kiện chỉ thêm không sửa, khi đính chính tạo sự kiện mới đính chính.
- **QTN-05**: Sự kiện phải gắn đúng Lô hàng còn hiệu lực (chưa bị thu hồi hay hủy).

---

## 2. API 1: Ghi sự kiện nhập kho HTX

Cho phép người ghi ghi nhận Lô hàng (`Shipment`) vừa vào kho HTX.

### 2.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/coop-warehouse/entry` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 2.2 Request Body

**DTO:** `RecordWarehouseEntryRequest`

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| --- | --- | --- | --- |
| `shipmentId` | UUID | ✓ | `@NotNull` - ID Lô hàng (`Shipment`). |
| `entryTime` | LocalDateTime | ✓ | `@NotNull` - Thời điểm nhập kho. Định dạng: `YYYY-MM-DDTHH:mm:ss`. Không vượt quá thời gian hiện tại. |
| `warehouseName` | String | ✓ | `@NotBlank` - Tên kho lưu trữ tại HTX (max 255 ký tự). |
| `storageCondition` | String | | Mô tả điều kiện bảo quản (VD: Nhiệt độ 5°C, độ ẩm 85%). |
| `notes` | String | | Ghi chú thêm. |
| `latitude` | Double | | Vĩ độ địa điểm kho. |
| `longitude` | Double | | Kinh độ địa điểm kho. |

**Ví dụ Request:**

```json
{
  "shipmentId": "00000000-0000-0000-0000-000900000001",
  "entryTime": "2026-09-01T08:00:00",
  "warehouseName": "Kho lạnh HTX Nông nghiệp Số 1",
  "storageCondition": "Nhiệt độ 4°C - 8°C, Độ ẩm 85%",
  "notes": "Nhập kho sau đóng gói",
  "latitude": 21.028512,
  "longitude": 105.854244
}
```

### 2.3 Response thành công (`201 Created`)

**DTO:** `ApiResult<CoopWarehouseEventResponse>`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "shipmentId": "00000000-0000-0000-0000-000900000001",
    "shipmentName": "Lô hàng Vải Thiều 1",
    "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
    "productionLotName": "Lô trồng Vải 10",
    "eventType": "WAREHOUSE_ENTRY",
    "warehouseName": "Kho lạnh HTX Nông nghiệp Số 1",
    "entryTime": "2026-09-01T08:00:00",
    "storageCondition": "Nhiệt độ 4°C - 8°C, Độ ẩm 85%",
    "notes": "Nhập kho sau đóng gói",
    "recordedAt": "2026-09-01T08:05:00",
    "recordedByName": "Nguyễn Văn Ghi"
  },
  "timestamp": "2026-09-01T08:05:00Z"
}
```

---

## 3. API 2: Ghi sự kiện xuất kho HTX

Cho phép người ghi ghi nhận sự kiện Lô hàng (`Shipment`) rời kho HTX để chuyển đi.

### 3.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/coop-warehouse/exit` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 3.2 Request Body

**DTO:** `RecordWarehouseExitRequest`

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| --- | --- | --- | --- |
| `shipmentId` | UUID | ✓ | `@NotNull` - ID Lô hàng (`Shipment`). |
| `exitTime` | LocalDateTime | ✓ | `@NotNull` - Thời điểm xuất kho. Định dạng: `YYYY-MM-DDTHH:mm:ss`. Phải >= `entryTime`. |
| `destination` | String | | Nơi chuyển đến / đơn vị tiếp nhận (max 255 ký tự). |
| `notes` | String | | Ghi chú thêm. |
| `latitude` | Double | | Vĩ độ địa điểm xuất kho. |
| `longitude` | Double | | Kinh độ địa điểm xuất kho. |

**Ví dụ Request:**

```json
{
  "shipmentId": "00000000-0000-0000-0000-000900000001",
  "exitTime": "2026-09-04T08:00:00",
  "destination": "Xe vận chuyển Công ty Thu Mua Chè Việt",
  "notes": "Xuất kho bàn giao vận chuyển"
}
```

### 3.3 Response thành công (`201 Created`)

**DTO:** `ApiResult<CoopWarehouseEventResponse>`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "f9e8d7c6-b5a4-3210-fedc-ba0987654321",
    "shipmentId": "00000000-0000-0000-0000-000900000001",
    "shipmentName": "Lô hàng Vải Thiều 1",
    "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
    "productionLotName": "Lô trồng Vải 10",
    "eventType": "WAREHOUSE_EXIT",
    "warehouseName": "Kho lạnh HTX Nông nghiệp Số 1",
    "entryTime": "2026-09-01T08:00:00",
    "exitTime": "2026-09-04T08:00:00",
    "storageDurationDays": 3,
    "storageDurationHours": 72,
    "maxAllowedStorageDays": 5,
    "isStorageExceeded": false,
    "warningMessage": null,
    "destination": "Xe vận chuyển Công ty Thu Mua Chè Việt",
    "recordedAt": "2026-09-04T08:10:00",
    "recordedByName": "Nguyễn Văn Ghi"
  },
  "timestamp": "2026-09-04T08:10:00Z"
}
```

### 3.4 Response Cảnh báo vượt ngưỡng lưu kho (`TC-03`)

Nếu `storageDurationDays` (3 ngày) vượt quá `maxAllowedStorageDays` (2 ngày):

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "f9e8d7c6-b5a4-3210-fedc-ba0987654321",
    "shipmentId": "00000000-0000-0000-0000-000900000001",
    "shipmentName": "Lô hàng Vải Thiều 1",
    "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
    "productionLotName": "Lô trồng Vải 10",
    "eventType": "WAREHOUSE_EXIT",
    "warehouseName": "Kho lạnh HTX Nông nghiệp Số 1",
    "entryTime": "2026-09-01T08:00:00",
    "exitTime": "2026-09-04T08:00:00",
    "storageDurationDays": 3,
    "storageDurationHours": 72,
    "maxAllowedStorageDays": 2,
    "isStorageExceeded": true,
    "warningMessage": "CẢNH BÁO: Thời gian lưu kho (3 ngày) vượt quá ngưỡng bảo quản cho phép (2 ngày) cho loại nông sản [Vải]",
    "destination": "Xe vận chuyển Công ty Thu Mua",
    "recordedAt": "2026-09-04T08:10:00",
    "recordedByName": "Nguyễn Văn Ghi"
  },
  "timestamp": "2026-09-04T08:10:00Z"
}
```

---

## 4. Danh sách các mã lỗi chính (Error Codes)

| HTTP Status | Message | Nguyên nhân |
| --- | --- | --- |
| `400 Bad Request` | `Lô hàng [Tên Lô] hiện đang trong kho HTX. Không thể ghi 2 lần nhập kho liên tiếp khi chưa xuất kho.` | **TC-04**: Đã có sự kiện nhập kho chưa xuất kho. |
| `400 Bad Request` | `Lô hàng [Tên Lô] chưa được ghi nhận nhập kho HTX. Vui lòng ghi sự kiện nhập kho trước khi xuất kho.` | **TC-02**: Thử ghi xuất kho khi chưa nhập kho. |
| `400 Bad Request` | `Thời điểm xuất kho không được trước thời điểm nhập kho.` | `exitTime < entryTime`. |
| `400 Bad Request` | `Thời điểm nhập kho / xuất kho không được ở tương lai.` | Thời gian > hiện tại. |
| `404 Not Found` | `Không tìm thấy lô hàng.` | `shipmentId` không tồn tại. |
| `403 Forbidden` | `Bạn không có quyền ghi sự kiện cho lô hàng của tổ chức này.` | Người dùng không thuộc HTX sở hữu lô hàng. |
