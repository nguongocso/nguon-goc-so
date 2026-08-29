# NCL-04-CN-005 — Xuất tem QR cho lô hàng (Export QR Labels)

| **Mã nghiệp vụ** | NCL-04-CN-005 |
|---|---|
| **HTTP Method** | `POST` |
| **Endpoint** | `/api/v1/shipments/{shipmentId}/labels/export` |
| **Phân quyền** | JWT — chỉ `VT-02` (Quản lý Hợp tác xã) |
| **Response** | File PDF nhị phân (`application/pdf`, `Content-Disposition: attachment`) |

## 1. Mô tả

Xuất file PDF chứa các tem QR để in dán lên bao bì sản phẩm. Mỗi tem gồm:

- Mã truy xuất duy nhất (`codeValue`).
- URL tra cứu công khai: `{FRONTEND_URL}/public/trace/{codeValue}` được mã hóa trong ảnh QR.
- Các trường tùy chọn: tên sản phẩm, tên hợp tác xã, mã lô (lot code), ngày đóng gói.

Toàn bộ lượt xuất được ghi vào lịch sử (`label_export_history`): ai xuất, khi nào,
khoảng mã nào (startIndex → endIndex), số lượng và khổ tem.

### Quy tắc nghiệp vụ

| Quy tắc | Ý nghĩa |
|---|---|
| QTN-23 | Tổng số tem xuất trong một lần phải nằm trong phạm vi số mã đã sinh của lô hàng; mọi lượt xuất đều được ghi log |
| QTN-01 | Cô lập dữ liệu theo tổ chức — VT-02 chỉ xuất tem lô hàng thuộc tổ chức của mình |
| QTN-02 | Mã truy xuất duy nhất (đảm bảo ở bước sinh mã) |
| QTN-03 | Tổng mã mỗi lô không vượt sản lượng kê khai (đảm bảo ở bước sinh mã) |

## 2. Request

### Path parameter

| Tên | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `shipmentId` | UUID | ✅ | ID lô hàng cần xuất tem |

### Body (`application/json`)

```json
{
  "startIndex": 0,
  "count": 100,
  "labelSize": "40x30",
  "includeFields": {
    "productName": true,
    "cooperativeName": true,
    "lotCode": true,
    "packagingDate": true
  }
}
```

| Trường | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
|---|---|---|---|---|
| `startIndex` | int | ❌ | `0` | `>= 0` |
| `count` | int | ✅ | — | `>= 1`; `startIndex + count <= tổng số mã đã sinh` của lô hàng |
| `labelSize` | string | ✅ | — | Một trong các khổ đã cấu hình (xem mục 4) |
| `includeFields` | object | ❌ | tất cả `true` | Cờ bật/tắt từng trường thông tin trên tem |

## 3. Response

### Thành công — `200 OK`

File PDF nhiều trang, mỗi trang sắp xếp lưới tem theo khổ đã chọn (giấy A4).
Header:

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="Tem_QR_{shipmentId}_{timestamp}.pdf"
```

### Lỗi

| Mã | Điều kiện |
|---|---|
| `400 BAD_REQUEST` | Lô hàng không có mã truy xuất nào; `count` vượt số mã khả dụng (`startIndex + count > tổng số mã`); khổ tem không hợp lệ; lô hàng đã bị thu hồi (`RECALLED`) |
| `403 FORBIDDEN` | Người dùng không có vai trò VT-02, hoặc lô hàng thuộc tổ chức khác (QTN-01), hoặc chưa đăng nhập |
| `404 NOT_FOUND` | Không tìm thấy lô hàng |

Ví dụ body lỗi:

```json
{
  "success": false,
  "message": "Số lượng tem xuất vượt quá số mã đã sinh cho lô hàng."
}
```

## 4. Khổ tem (Label size templates)

Giấy nền A4 (210 × 297 mm), lề ~5 mm, khoảng cách giữa các tem ~2 mm.

| `labelSize` | Kích thước tem (mm) | Số cột × hàng / trang A4 | Ghi chú |
|---|---|---|---|
| `40x30` | 40 × 30 | 5 × 9 = 45 tem/trang | Tem nhỏ, QR + mã + 1 dòng thông tin rút gọn |
| `50x40` | 50 × 40 | 4 × 7 = 28 tem/trang | Khổ phổ biến cho bao bì hộp/túi |
| `70x50` | 70 × 50 | 3 × 5 = 15 tem/trang | Tem lớn, hiển thị đầy đủ các trường tùy chọn |

## 5. Bố cục một tem

```
┌──────────────────────────────┐
│  ┌────────┐  MÃ TRUY XUẤT   │
│  │   QR   │  NGO-250819-... │
│  │ (URL   │  Tên sản phẩm   │
│  │ tra    │  HTX ABC        │
│  │ cứu)   │  Lot: ...       │
│  └────────┘  ĐG: 25/08/2026 │
└──────────────────────────────┘
```

- QR luôn chứa URL tra cứu công khai `{FRONTEND_URL}/public/trace/{codeValue}`.
- Mã truy xuất luôn hiển thị dưới dạng chữ.
- Các trường còn lại chỉ in khi cờ tương ứng trong `includeFields` là `true`.
