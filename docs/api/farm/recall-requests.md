# API Docs – Yêu cầu thu hồi lô hàng (2 bước)

**Tên nhánh gốc:** `feature/NCL-08-CN-008-two-step-recall`
**Nhánh tích hợp với phản ánh:** `feature/NCL-08-CN-009-feedback-processing`

## 1. Phạm vi nghiệp vụ

Quy trình thu hồi gồm hai bước:

1. Người có quyền tạo đề nghị chọn **một lô hàng** (`Shipment`).
2. Quản lý hợp tác xã (`VT-02`) duyệt hoặc từ chối.

Khi duyệt, hệ thống chỉ:

- chuyển `Shipment` được chọn sang `RECALLED`;
- chuyển toàn bộ `TraceCode` thuộc shipment đó sang `RECALLED`;
- tạo lịch sử thu hồi lô hàng và hiển thị cảnh báo khi tra cứu các tem thuộc shipment;
- thông báo cho người dùng nội bộ và bên thu mua có sự kiện `PROCUREMENT` trên shipment đó.

`ProductionLot` chỉ là thông tin nguồn/ngữ cảnh. Hệ thống **không** chuyển cả lô sản xuất sang
`RECALLED` và **không** thu hồi các shipment khác trong cùng lô sản xuất.

Quy tắc `QTN-22`: người tạo yêu cầu không được tự duyệt yêu cầu của mình.

Tất cả API danh sách, chi tiết và xử lý đều giới hạn theo tổ chức hiện tại; quản lý của tổ chức
khác nhận kết quả không tìm thấy.

## 2. Mô hình response

```json
{
  "id": "uuid",
  "shipmentId": "uuid",
  "shipmentName": "Lô hàng ngô số 01",
  "lotId": "uuid",
  "lotName": "Lô ngô Công Nghệ",
  "sourceFeedbackId": "uuid-or-null",
  "requestedBy": { "userId": "uuid", "fullName": "Nguyễn Văn A" },
  "requestedAt": "2026-09-07T10:00:00",
  "status": "PENDING",
  "reason": "Nghi ngờ sản phẩm không bảo đảm chất lượng",
  "evidence": "Kết quả đối chiếu và nội dung phản ánh",
  "approvedBy": null,
  "approvedAt": null,
  "approvalRemarks": null,
  "rejectedBy": null,
  "rejectedAt": null,
  "rejectionReason": null,
  "notifiedBuyerCount": 0
}
```

`lotId` và `lotName` được giữ để hiển thị nguồn sản xuất, không phải phạm vi thu hồi.

## 3. Tạo yêu cầu

```http
POST /api/v1/recall-requests
Authorization: Bearer <token>
```

Quyền: `VT-03` theo controller hiện hành.

```json
{
  "shipmentId": "uuid",
  "reason": "Phát hiện dấu hiệu không bảo đảm chất lượng",
  "evidence": "Biên bản kiểm tra"
}
```

| Trường | Bắt buộc | Quy tắc |
|---|---:|---|
| `shipmentId` | Có | Shipment phải thuộc tổ chức hiện tại và chưa `RECALLED` |
| `reason` | Có | Không rỗng, tối đa 1.000 ký tự |
| `evidence` | Không | Tối đa 2.000 ký tự |

Lô sản xuất cha phải ở một trong các trạng thái `APPROVED`, `HARVESTED`, `PACKAGED`. Không được
tạo thêm yêu cầu `PENDING` cho cùng shipment.

Response: `201 Created`, `ApiResult<RecallRequestResponse>`.

## 4. Danh sách và chi tiết

```http
GET /api/v1/recall-requests?status=PENDING&page=0&size=20
GET /api/v1/recall-requests/{id}
```

Quyền: `VT-02`. `status` nhận `PENDING`, `APPROVED`, `REJECTED`. Dữ liệu luôn được giới hạn theo
tổ chức của người đăng nhập.

## 5. Duyệt

```http
PUT /api/v1/recall-requests/{id}/approve
```

```json
{
  "remarks": "Đã xác minh phạm vi lô hàng"
}
```

Điều kiện:

- yêu cầu thuộc tổ chức hiện tại và đang `PENDING`;
- người duyệt không trùng người tạo;
- yêu cầu phải xác định được `shipmentId`;
- shipment chưa được thu hồi.

Tác động duyệt:

```text
Shipment được chọn -> RECALLED
└── toàn bộ TraceCode của shipment -> RECALLED

ProductionLot -> giữ nguyên
Các Shipment khác cùng ProductionLot -> giữ nguyên
```

Response: `200 OK`, `ApiResult<RecallRequestResponse>`.

Yêu cầu dữ liệu cũ không thể tự ánh xạ sang shipment sẽ trả `409` với hướng dẫn từ chối và tạo lại.

## 6. Từ chối

```http
PUT /api/v1/recall-requests/{id}/reject
```

```json
{
  "rejectionReason": "Chưa đủ bằng chứng xác thực"
}
```

`rejectionReason` bắt buộc. Nếu yêu cầu sinh từ phản ánh, phản ánh chuyển từ
`ESCALATED_TO_RECALL` về `IN_PROGRESS` để tiếp tục xử lý.

## 7. Đề nghị từ phản ánh người tiêu dùng

Endpoint và payload được mô tả chi tiết tại
[`ProductFeedbackProcessing.md`](./ProductFeedbackProcessing.md). Quy tắc xác định shipment:

- phản ánh có mã tem: backend tự lấy shipment chứa mã tem; client không thể chọn shipment khác;
- phản ánh không có mã tem: `shipmentId` bắt buộc và phải thuộc lô sản xuất của phản ánh;
- khi duyệt chỉ shipment đã xác định bị thu hồi.

## 8. Migration dữ liệu cũ

Migration `V20260907120000__scope_recall_requests_to_shipments.sql` thêm `shipment_id` và tự ánh xạ:

1. qua `source_feedback_id -> trace_code_id -> shipment_id` nếu phản ánh có mã tem;
2. qua lô sản xuất nếu lô sản xuất chỉ có đúng một shipment.

Trường hợp cũ có nhiều shipment nhưng không có mã tem được giữ `shipment_id = NULL` để tránh tự chọn
sai phạm vi. Yêu cầu đó không được duyệt và phải được tạo lại.
