# API Docs – Hủy lô sản xuất (NCL-02-CN-006)

**Tên nhánh:** `feature/NCL-02-CN-006-cancel-production-lot`

**Quy tắc nghiệp vụ:** QTN-01 (cách ly dữ liệu giữa các tổ chức), QTN-08 (sự kiện đã ghi chỉ thêm, không sửa/xóa).

---

## 1. Hủy lô sản xuất (`→ CANCELLED`)

### Thông tin API

| Thuộc tính   | Giá trị                                |
| ------------ | -------------------------------------- |
| **Method**   | `POST`                                 |
| **Endpoint** | `/api/v1/production-lots/{id}/cancel`  |
| **Quyền**    | `VT-02` (Quản lý hợp tác xã)           |

### Request

**Path parameter:**

- `id`: UUID của lô sản xuất.

**Request body:**

```json
{
  "reason": "Mất mùa do thời tiết",
  "note": "Mưa lớn kéo dài khiến toàn bộ diện tích bị ngập úng, không thể thu hoạch."
}
```

> **`reason` (bắt buộc):** lý do hủy, chọn 1 trong danh sách cố định (hiển thị trên UI viết hoa chữ đầu):
> - `Mất mùa do thời tiết`
> - `Sâu bệnh`
> - `Khai báo nhầm`
> - `Lý do khác`
>
> **`note` (không bắt buộc — nhãn "Tại sao?" trên UI):** diễn giải chi tiết lý do hủy — tối đa 1000 ký tự.
>
> Nếu `reason` bỏ trống, hệ thống từ chối (`NCL-02-CN-006-TC-03`). `note` không bắt buộc (quyết định người dùng 2026-09-04).

### Response `200 OK`

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "uuid",
    "name": "Lô lúa vụ hè",
    "status": "CANCELLED",
    "cancellationReason": "mất mùa do thời tiết",
    "cancellationNote": "Mưa lớn kéo dài khiến toàn bộ diện tích bị ngập úng, không thể thu hoạch.",
    "cancelledByName": "Trần Văn A",
    "cancelledAt": "2026-09-02T11:00:00"
  }
}
```

> Sau khi hủy:
> - Lô chuyển sang trạng thái `CANCELLED` (`NCL-02-CN-006-TC-01`).
> - Không còn hiển thị trong danh sách chọn để **ghi sự kiện** hay **tạo lô hàng**.
> - Bị loại khỏi số liệu sản lượng "đang canh tác" ở dashboard và báo cáo ngành, nhưng vẫn thống kê riêng ở mục **lô đã hủy**.
> - Nhật ký canh tác và sự kiện đã ghi trước đó được **giữ nguyên** và chỉ xem ở chế độ chỉ đọc (`NCL-02-CN-006-TC-04`).

### Lỗi thường gặp

| HTTP | Trường hợp | Ghi chú |
| ---- | ---------- | ------- |
| `400` | Bỏ trống `reason` | `NCL-02-CN-006-TC-03` — "Lý do hủy không được để trống" (`note` không bắt buộc) |
| `400` | Lô đã sinh mã truy xuất (đã có lô hàng/tem) | `NCL-02-CN-006-TC-02` — "Lô đã sinh mã truy xuất, không thể hủy. Vui lòng sử dụng luồng thu hồi lô" |
| `400` | Lô ở trạng thái cuối `CANCELLED` / `CLOSED` / `RECALLED` | "Lô đã ở trạng thái …, không thể hủy" |
| `400` | Lô không thuộc tổ chức của bạn (QTN-01) | "Lô sản xuất không thuộc tổ chức của bạn" |
| `403` | Không có quyền (không phải VT-02) | `@PreAuthorize("hasRole('VT-02')")` |
| `404` | Không tìm thấy lô | "Không tìm thấy lô sản xuất" |

### Ghi chú lưu vết

- Ghi nhật ký hoạt động (`activity_logs`) với `action = CANCEL`, `entityType = ProductionLot`.
- `@Auditable(action = "CANCEL_PRODUCTION_LOT", entityType = "PRODUCTION_LOT")` tương ứng mọi lần hủy.
- Các cột dữ liệu lưu trên `production_lot`: `cancellation_reason`, `cancellation_note`, `cancelled_by`, `cancelled_at` (migration `V62__add_cancellation_fields_to_production_lot.sql`).

---

## 2. Phạm vi của Story (liên quan)

- **Lô đã sinh mã** phải đi theo luồng thu hồi lô (`ShipmentRecall`) chứ **không** được hủy — xem `docs/api/trace/ShipmentRecall.md`.
- Điều kiện hủy **duy nhất** theo tài liệu gốc (mô tả story I77 / precondition J77 của `NCL-02-CN-006`): *"Hệ thống chỉ cho hủy khi lô chưa sinh mã truy xuất"* — **không giới hạn trạng thái**. Lô `PACKAGED` (đã đóng gói) nhưng chưa tạo lô hàng/sinh mã **vẫn được hủy**. Backend chỉ chặn thêm các trạng thái cuối `CANCELLED` / `CLOSED` / `RECALLED` (thực tế đều đã sinh mã nên bị chặn sẵn bởi gate mã truy xuất).