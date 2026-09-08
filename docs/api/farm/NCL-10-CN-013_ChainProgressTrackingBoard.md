# API Documentation – NCL-10-CN-013: Bảng theo dõi tiến độ chuỗi của từng lô

**User Story:** `NCL-10-CN-013 Bảng theo dõi tiến độ chuỗi của từng lô`  
**Vai trò:** Quản lý hợp tác xã (`VT-02`), Quản trị viên (`VT-01`), Người ghi sự kiện (`VT-03`)  
**Tệp tài liệu API:** `docs/api/farm/NCL-10-CN-013_ChainProgressTrackingBoard.md`  

---

## 1. Tổng quan API

API cung cấp dữ liệu bảng tiến độ chuỗi sản xuất & lưu thông của các lô đang mở theo 9 giai đoạn quy chuẩn, kèm thông tin việc cần làm tiếp theo (next action required) và đánh dấu tồn đọng (stagnant/overdue).

| Thuộc tính   | Giá trị                                      |
|--------------|----------------------------------------------|
| **Method**   | `GET`                                        |
| **Endpoint** | `/api/v1/production-lots/chain-progress`     |
| **Auth**     | Bearer Token (`VT-01`, `VT-02`, `VT-03`)     |

---

## 2. Thông tin Request

### Query Parameters

| Parameter               | Kiểu    | Bắt buộc | Mặc định | Mô tả |
|-------------------------|---------|----------|----------|-------|
| `organizationId`        | `UUID`  | Không    | Token Org| Mã tổ chức cần truy vấn. Nếu để trống, lấy từ User đang đăng nhập (`QTN-01`). |
| `stagnantThresholdDays` | `Integer`| Không   | `10`     | Ngưỡng số ngày ở một giai đoạn để cảnh báo lô bị tồn đọng (`NCL-10-CN-013-TC-03`). |
| `search`                | `String`| Không    | Rỗng     | Tìm kiếm theo tên lô hoặc tên vùng trồng. |

---

## 3. Các giai đoạn trong chuỗi (9 Giai đoạn)

1. `DRAFT`: Nháp
2. `PENDING`: Chờ duyệt
3. `APPROVED`: Đã duyệt
4. `HARVESTED`: Đã thu hoạch
5. `PREPROCESSED`: Đã sơ chế
6. `WAITING_TEST_RESULT`: Chờ kết quả kiểm nghiệm
7. `PACKAGED`: Đã đóng gói
8. `TAG_ACTIVATED`: Đã kích hoạt tem
9. `IN_CIRCULATION`: Đang lưu thông

---

## 4. Response `200 OK` (Thành công)

```json
{
  "success": true,
  "status": 200,
  "data": {
    "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66a600",
    "organizationName": "Hợp tác xã Nông nghiệp Xanh",
    "totalOpenLots": 8,
    "stagnantLotsCount": 1,
    "stagnantThresholdDays": 10,
    "stages": [
      {
        "stage": "DRAFT",
        "stageName": "Nháp",
        "count": 1,
        "items": [
          {
            "id": "11111111-1111-1111-1111-111111111111",
            "name": "Lô dưa lưới vụ Thu 2026-01",
            "farmAreaId": "88888888-8888-8888-8888-888888888888",
            "farmAreaName": "Vùng trồng A1 - Nhà kính 01",
            "productCategoryId": "99999999-9999-9999-9999-999999999999",
            "productCategoryName": "Dưa lưới Huỳnh Long",
            "status": "DRAFT",
            "currentStage": "DRAFT",
            "daysInStage": 2,
            "isStagnant": false,
            "nextActionRequired": "Gửi yêu cầu duyệt lô sản xuất",
            "targetScreen": "/farm/production-lots/11111111-1111-1111-1111-111111111111",
            "createdAt": "2026-09-06T08:00:00.000Z",
            "updatedAt": "2026-09-06T08:00:00.000Z"
          }
        ]
      },
      {
        "stage": "PENDING",
        "stageName": "Chờ duyệt",
        "count": 1,
        "items": [
          {
            "id": "22222222-2222-2222-2222-222222222222",
            "name": "Lô xoài Cát Chu vụ 2",
            "farmAreaId": "88888888-8888-8888-8888-888888888888",
            "farmAreaName": "Vùng trồng B2",
            "productCategoryId": "77777777-7777-7777-7777-777777777777",
            "productCategoryName": "Xoài Cát Chu",
            "status": "PENDING",
            "currentStage": "PENDING",
            "daysInStage": 12,
            "isStagnant": true,
            "nextActionRequired": "Duyệt lô sản xuất",
            "targetScreen": "/farm/production-lots/22222222-2222-2222-2222-222222222222",
            "createdAt": "2026-08-27T08:00:00.000Z",
            "updatedAt": "2026-08-27T08:00:00.000Z"
          }
        ]
      },
      {
        "stage": "APPROVED",
        "stageName": "Đã duyệt",
        "count": 2,
        "items": []
      },
      {
        "stage": "HARVESTED",
        "stageName": "Đã thu hoạch",
        "count": 1,
        "items": [
          {
            "id": "44444444-4444-4444-4444-444444444444",
            "name": "Lô bưởi Da Xanh VietGAP",
            "farmAreaId": "66666666-6666-6666-6666-666666666666",
            "farmAreaName": "Vùng trồng C1",
            "productCategoryId": "55555555-5555-5555-5555-555555555555",
            "productCategoryName": "Bưởi Da Xanh",
            "status": "HARVESTED",
            "currentStage": "HARVESTED",
            "daysInStage": 3,
            "isStagnant": false,
            "nextActionRequired": "Chờ hoặc nhập kết quả kiểm nghiệm",
            "targetScreen": "/certification/inspection-requests?lotId=44444444-4444-4444-4444-444444444444",
            "createdAt": "2026-09-01T08:00:00.000Z",
            "updatedAt": "2026-09-05T08:00:00.000Z"
          }
        ]
      },
      {
        "stage": "PREPROCESSED",
        "stageName": "Đã sơ chế",
        "count": 1,
        "items": []
      },
      {
        "stage": "WAITING_TEST_RESULT",
        "stageName": "Chờ kết quả kiểm nghiệm",
        "count": 1,
        "items": []
      },
      {
        "stage": "PACKAGED",
        "stageName": "Đã đóng gói",
        "count": 1,
        "items": []
      },
      {
        "stage": "TAG_ACTIVATED",
        "stageName": "Đã kích hoạt tem",
        "count": 0,
        "items": []
      },
      {
        "stage": "IN_CIRCULATION",
        "stageName": "Đang lưu thông",
        "count": 0,
        "items": []
      }
    ]
  },
  "timestamp": "2026-09-08T11:00:00.000Z"
}
```

---

## 5. Xử lý Lỗi (Error Responses)

### 403 Forbidden – Không có quyền / Truy cập chéo tổ chức (`QTN-01`)

```json
{
  "success": false,
  "status": 403,
  "message": "Từ chối truy cập: Bạn không có quyền xem dữ liệu của tổ chức này.",
  "path": "/api/v1/production-lots/chain-progress",
  "timestamp": "2026-09-08T11:00:00.000Z"
}
```

### 401 Unauthorized – Chưa đăng nhập

```json
{
  "success": false,
  "status": 401,
  "message": "Unauthorized",
  "path": "/api/v1/production-lots/chain-progress",
  "timestamp": "2026-09-08T11:00:00.000Z"
}
```

---

## 6. Ma trận Quy tắc & Suy luận "Việc cần làm tiếp theo" (Next Action Mapping)

| Trạng thái / Điều kiện lô | Giai đoạn tiến độ | Việc cần làm tiếp theo (`nextActionRequired`) | Màn hình đích (`targetScreen`) |
|---------------------------|-------------------|------------------------------------------------|--------------------------------|
| Trạng thái `DRAFT` | `DRAFT` | Gửi yêu cầu duyệt lô sản xuất | `/farm/production-lots/{id}` |
| Trạng thái `PENDING` | `PENDING` | Duyệt lô sản xuất | `/farm/production-lots/{id}` |
| Trạng thái `APPROVED` (chưa thu hoạch) | `APPROVED` | Ghi nhật ký canh tác / Ghi nhận thu hoạch | `/farm/farm-logs?lotId={id}` |
| Trạng thái `HARVESTED` (chưa gửi kiểm nghiệm hoặc chờ kết quả) | `HARVESTED` / `WAITING_TEST_RESULT` | Chờ hoặc nhập kết quả kiểm nghiệm | `/certification/inspection-requests?lotId={id}` |
| Trạng thái `PREPROCESSED` (thiếu mốc canh tác - QTN-06) | `PREPROCESSED` | Bổ sung nhật ký canh tác trước khi đóng gói | `/farm/farm-logs?lotId={id}` |
| Trạng thái `PREPROCESSED` (chưa có KQ kiểm nghiệm đạt - QTN-21) | `PREPROCESSED` | Nhập kết quả kiểm nghiệm đạt để kích hoạt tem | `/certification/inspection-requests?lotId={id}` |
| Trạng thái `PREPROCESSED` / `PACKAGED` | `PACKAGED` | Đóng gói & Tạo lô hàng (Shipment) | `/trace/shipments?lotId={id}` |
| Lô hàng đã tạo, chưa kích hoạt tem | `TAG_ACTIVATED` | Kích hoạt tem QR cho lô hàng | `/trace/tags/activate?lotId={id}` |
| Tem đã kích hoạt, lô đang lưu thông | `IN_CIRCULATION` | Theo dõi lưu thông & quét tem | `/trace/shipments?id={id}` |

---

## 7. Tiêu chí nghiệm thu (Acceptance Criteria Mapping)

- **`NCL-10-CN-013-TC-01`**: Xếp đúng 8 lô vào 5 cột giai đoạn tương ứng trong 9 giai đoạn quy chuẩn.
- **`NCL-10-CN-013-TC-02`**: Lô đã thu hoạch nhưng chưa có kết quả kiểm nghiệm đạt -> hiển thị gợi ý "Chờ hoặc nhập kết quả kiểm nghiệm".
- **`NCL-10-CN-013-TC-03`**: Lô nằm ở 1 giai đoạn quá ngưỡng (ví dụ: `daysInStage` = 12 > 10 ngày) -> trả về `isStagnant: true` để frontend highlight tồn đọng.
- **`NCL-10-CN-013-TC-04`**: Phân lập dữ liệu theo `organizationId` (QTN-01), người dùng tổ chức nào chỉ nhìn thấy dữ liệu tổ chức đó.
