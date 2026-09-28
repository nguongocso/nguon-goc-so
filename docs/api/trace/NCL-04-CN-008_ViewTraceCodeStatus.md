# NCL-04-CN-008 — Xem và tra cứu trạng thái từng mã tem trong lô hàng

| **Mã nghiệp vụ** | NCL-04-CN-008 |
|---|---|
| **Vai trò áp dụng** | `VT-02` (Quản lý Hợp tác xã) |
| **Quy tắc nghiệp vụ** | QTN-01 (Cô lập dữ liệu theo tổ chức), QTN-02 (Tính duy nhất của mã truy xuất) |

---

## 1. Mô tả tổng quan

Tính năng cho phép Quản lý Hợp tác xã (`VT-02`) theo dõi chi tiết trạng thái của từng mã tem truy xuất trong lô hàng:
- Trạng thái mã: `INACTIVE` (Chưa kích hoạt / Chưa in), `ACTIVE` (Đã kích hoạt), `LOCKED` (Đang bị khóa), `CANCELLED` (Đã hủy), `RECALLED` (Thu hồi theo lô).
- Lọc theo trạng thái và tìm kiếm theo tiền tố/chuỗi ký tự mã (`codeValue`).
- Quét mã QR bằng camera để tìm kiếm và định vị nhanh mã tem.
- Xem dòng thời gian lịch sử chi tiết của từng mã: Thời điểm tạo, Thời điểm in, Thời điểm kích hoạt, Thời điểm hủy/khóa/mở khóa (kèm lý do và người thực hiện), Lượt quét người tiêu dùng và 5 lần quét gần nhất (thời gian, vị trí, IP).
- Xuất danh sách mã tem ra file CSV/Excel phục vụ đối soát kho và dán nhãn.

---

## 2. Chi tiết các Endpoint

### 2.1 Danh sách mã tem theo lô hàng (Phân trang & Lọc)

- **HTTP Method:** `GET`
- **URL:** `/api/v1/shipments/{shipmentId}/trace-codes`
- **Phân quyền:** `hasRole('VT-02')`

#### Path Parameter
| Tên | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `shipmentId` | UUID | ✅ | ID của lô hàng cần tra cứu |

#### Query Parameters
| Tên | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `status` | String | ❌ | — | Lọc theo trạng thái: `INACTIVE`, `ACTIVE`, `LOCKED`, `CANCELLED`, `RECALLED` |
| `search` | String | ❌ | — | Tìm kiếm gần đúng theo chuỗi ký tự mã tem (`codeValue`) |
| `page` | Integer | ❌ | `0` | Chỉ mục trang (0-based) |
| `size` | Integer | ❌ | `20` | Số lượng bản ghi trên một trang |

#### Response (`200 OK`)
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "content": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "codeValue": "HTX01-LOT2026-000001",
        "status": "ACTIVE",
        "activatedAt": "2026-09-07T08:30:00",
        "printedAt": "2026-09-06T15:00:00",
        "cancelledAt": null,
        "lockedAt": null,
        "scanCount": 5,
        "createdAt": "2026-09-05T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

---

### 2.2 Xem lịch sử chi tiết của một mã tem

- **HTTP Method:** `GET`
- **URL:** `/api/v1/trace-codes/{codeValue}/history`
- **Phân quyền:** `hasRole('VT-02')`

#### Path Parameter
| Tên | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `codeValue` | String | ✅ | Giá trị mã tem cần xem lịch sử (VD: `HTX01-LOT2026-000001`) |

#### Response (`200 OK`)
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "codeValue": "HTX01-LOT2026-000001",
    "status": "ACTIVE",
    "shipmentId": "123e4567-e89b-12d3-a456-426614174001",
    "shipmentName": "Lô hàng bưởi Phúc Trạch đợt 1",
    "scanCount": 5,
    "createdAt": "2026-09-05T10:00:00",
    "events": [
      {
        "type": "CREATED",
        "timestamp": "2026-09-05T10:00:00",
        "details": "Mã tem được khởi tạo cùng lô hàng",
        "actorName": "Nguyễn Văn Quản Lý"
      },
      {
        "type": "PRINTED",
        "timestamp": "2026-09-06T15:00:00",
        "details": "Xuất file in tem QR khổ 40x30",
        "actorName": "Nguyễn Văn Quản Lý"
      },
      {
        "type": "ACTIVATED",
        "timestamp": "2026-09-07T08:30:00",
        "details": "Kích hoạt tem cho lô hàng xuất bán",
        "actorName": "Nguyễn Văn Quản Lý"
      },
      {
        "type": "SCANNED",
        "timestamp": "2026-09-07T09:15:20",
        "details": "Quét tra cứu tại TP. Hà Tĩnh (IP: 14.162.180.12)",
        "actorName": "Khách hàng"
      }
    ]
  }
}
```

---

### 2.3 Xuất danh sách mã tem ra file

- **HTTP Method:** `POST`
- **URL:** `/api/v1/shipments/{shipmentId}/trace-codes/export`
- **Phân quyền:** `hasRole('VT-02')`
- **Response:** File CSV (`text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="Danh_sach_ma_tem_...csv"`) kèm UTF-8 BOM hiển thị chuẩn trong Microsoft Excel.

#### Body (`application/json`)
```json
{
  "status": "ACTIVE",
  "search": "00000"
}
```

#### Cột trong file xuất
1. STT
2. Mã tem (`codeValue`)
3. Trạng thái
4. Ngày tạo
5. Ngày in
6. Ngày kích hoạt
7. Lượt quét

---

## 3. Mã lỗi nghiệp vụ

| HTTP Status | Message | Giải thích |
|---|---|---|
| `403 FORBIDDEN` | `Bạn không có quyền xem mã tem của tổ chức khác.` | Vi phạm quy tắc cô lập tổ chức QTN-01 |
| `403 FORBIDDEN` | `Chỉ Quản lý hợp tác xã (VT-02) mới có quyền truy cập chức năng này.` | Truy cập sai vai trò |
| `404 NOT_FOUND` | `Không tìm thấy lô hàng.` | `shipmentId` không tồn tại |
| `404 NOT_FOUND` | `Không tìm thấy mã tem truy xuất.` | `codeValue` không tồn tại |
