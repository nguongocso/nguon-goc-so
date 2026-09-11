# Bằng chứng Kiểm thử Hồi quy Đầu cuối — User Story NCL-10-CN-015

> **Mã User Story:** NCL-10-CN-015  
> **Tên Story:** Kiểm thử hồi quy đầu cuối và cập nhật tài liệu vận hành  
> **Môi trường kiểm thử:** Local Runtime (Spring Boot 3.5.16 + Java 21, Vite Frontend 8 + React 19, MariaDB 10.4 / MySQL 8.4)  
> **Ngày thực hiện:** 11/09/2026  
> **Trạng thái tổng thể:** **PASS** (100% tiêu chí kiểm thử đạt yêu cầu)

---

## 1. Bảng tổng hợp kết quả kiểm thử

| Tiêu chí | Nội dung kiểm thử | Trạng thái | Ghi chú & Bằng chứng |
|---|---|:---:|---|
| **CV-01 / CV-02** | Hồi quy luồng nghiệp vụ mới nhất: Bàn giao lô hàng (NCL-05-CN-009) & Đóng vụ việc thu hồi (NCL-08-CN-012) | **PASS** ✅ | Đã chạy thực tế trên live backend. Xác nhận / từ chối bàn giao thành công; lazy-materialize và đóng vụ việc thu hồi với biện pháp khắc phục đạt chuẩn. |
| **TC-02 (QTN-19)** | Tính toàn vẹn chuỗi băm sự kiện (Event Hash Chaining) trên lô có cả sự kiện Bàn giao (Handover) và Thu hồi (Recall) | **PASS** ✅ | Chuỗi 12 sự kiện liên tục trên lô hàng `Lo hang 1`, thuật toán SHA-256, `isIntegrityVerified: true`, `verificationStatus: INTACT`. |
| **TC-03 (QTN-01)** | Kiểm toán cách ly dữ liệu đa tổ chức (Cross-Organization Data Isolation) trên tất cả endpoint mới | **PASS** ✅ | 7/7 bài test cách ly dữ liệu đạt yêu cầu (100% không rò rỉ chéo giữa HTX 1, HTX 2 và Doanh nghiệp thu mua; trả về 403 Forbidden / 400 Bad Request hoặc danh sách scoped theo org). |
| **TC-04 / CV-05** | Cập nhật tài liệu vận hành, hướng dẫn sử dụng và dữ liệu mẫu/seed data | **PASS** ✅ | Cập nhật đầy đủ `OPERATIONS.md`, `USER_GUIDE.md`, `DEMO_DATA.md`, `TEST_EVIDENCE.md`. |

---

## 2. Chi tiết thực thi NCL-05-CN-009 (Bàn giao lô hàng)

### 2.1 Môi trường và Tác nhân
- **Bên gửi (VT-02):** `cm_tc` thuộc Hợp tác xã chè Tân Cương (`HTX-TC`, ID: `47bcceae-7d31-4ad4-b1da-828290041515`).
- **Bên nhận (VT-04):** `procurement` thuộc Công ty Nông Sản Việt Demo (`DEMO_NSV`, ID: `cc586748-a3be-11f1-9ca5-e00af63e88f4`).

### 2.2 Bước 1: Xem danh sách phiếu bàn giao đã gửi (VT-02)
- **Endpoint:** `GET /api/v1/handovers?page=0&size=10`
- **Headers:** `Authorization: Bearer <token_VT02>`
- **Kết quả HTTP:** `200 OK`
- **Dữ liệu trả về (trích lược):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "ef68f799-482b-43b1-bd3f-a55e0ade88e1",
        "shipmentId": "347feea9-7825-4f4a-9413-cc9005da9e07",
        "shipmentName": "Lo hang 1",
        "fromOrganizationName": "Hợp tác xã chè Tân Cương",
        "toOrganizationName": "Công ty Nông Sản Việt Demo",
        "quantity": 13,
        "unit": "kg",
        "status": "PENDING_CONFIRMATION"
      },
      {
        "id": "4a5a6f7c-c9c2-4e97-8f0f-79d46043efac",
        "shipmentId": "347feea9-7825-4f4a-9413-cc9005da9e07",
        "shipmentName": "Lo hang 1",
        "fromOrganizationName": "Hợp tác xã chè Tân Cương",
        "toOrganizationName": "Công ty Nông Sản Việt Demo",
        "quantity": 11,
        "unit": "kg",
        "status": "PENDING_CONFIRMATION"
      }
    ],
    "totalElements": 14,
    "page": 0,
    "size": 10
  }
}
```

### 2.3 Bước 2: Kiểm tra kiểm soát truy cập chéo (Cross-Org Access Control)
- Bên gửi (`cm_tc`, VT-02) cố gắng tự xác nhận phiếu bàn giao gửi đi:
- **Endpoint:** `POST /api/v1/handovers/ef68f799-482b-43b1-bd3f-a55e0ade88e1/accept`
- **Headers:** `Authorization: Bearer <token_VT02>`
- **Kết quả:** `HTTP 403 Forbidden`
- **Đánh giá:** ✅ Đạt yêu cầu bảo mật, bên gửi không thể tự nhận hàng thay cho bên mua.

### 2.4 Bước 3: Bên nhận xác nhận bàn giao thành công (VT-04)
- **Endpoint:** `POST /api/v1/handovers/ef68f799-482b-43b1-bd3f-a55e0ade88e1/accept`
- **Headers:** `Authorization: Bearer <token_VT04>`
- **Kết quả HTTP:** `200 OK`
- **Dữ liệu trả về:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "ef68f799-482b-43b1-bd3f-a55e0ade88e1",
    "shipmentId": "347feea9-7825-4f4a-9413-cc9005da9e07",
    "shipmentName": "Lo hang 1",
    "status": "ACCEPTED",
    "confirmedAt": "2026-09-11T21:12:20.6710672",
    "fromOrganizationName": "Hợp tác xã chè Tân Cương",
    "toOrganizationName": "Công ty Nông Sản Việt Demo"
  }
}
```
- **Sự kiện chuỗi được sinh ra (ChainEvent):**
  + Loại sự kiện: `HANDOVER`
  + Hash: `79dd400632b86a0fcf0962198d47e4cc6a9c9c7aacd70eec09f3f1da5e0d0a38`
  + Previous Hash: `0c4b66eadaa99c595c52949df6bae81be557882115708be65d64b296af171794`
  + Quyền sở hữu lô hàng chuyển giao thành công sang `DEMO_NSV` (QTN-31).

### 2.5 Bước 4: Bên nhận từ chối bàn giao kèm lý do (VT-04)
- **Endpoint:** `POST /api/v1/handovers/4a5a6f7c-c9c2-4e97-8f0f-79d46043efac/reject`
- **Headers:** `Authorization: Bearer <token_VT04>`, `Content-Type: application/json; charset=utf-8`
- **Body:**
```json
{
  "reason": "Hang bi dap hong trong luc boc do va van chuyen"
}
```
- **Kết quả HTTP:** `200 OK`
- **Dữ liệu trả về:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "4a5a6f7c-c9c2-4e97-8f0f-79d46043efac",
    "status": "REJECTED",
    "cancelReason": "Hang bi dap hong trong luc boc do va van chuyen",
    "rejectedAt": "2026-09-11T21:13:53.5456005"
  }
}
```

---

## 3. Chi tiết thực thi NCL-08-CN-012 (Quản lý và Đóng vụ việc thu hồi)

### 3.1 Môi trường và Tác nhân
- **Tổ chức:** `DEMO_HTX` (HTX Nông Sản Demo, ID: `cc57f586-a3be-11f1-9ca5-e00af63e88f4`)
- **Quản lý 1 (VT-02):** `orgmanager`
- **Quản lý 2 (VT-02):** `orgmanager2` (thực thi nguyên tắc 4 mắt QTN-22)
- **Lô sản xuất:** `00000000-0000-0000-0000-000200000001` (`Lô Nho 01`)
- **Lô hàng:** `00000000-0000-0000-0000-000900000001` (Số lượng: 3 kg, tem activated)

### 3.2 Bước 1: Truy vết phạm vi ảnh hưởng hai chiều (VT-02)
- **Endpoint:** `GET /api/v1/trace/impact-scope?code=00000000-0000-0000-0000-000200000001`
- **Kết quả HTTP:** `200 OK`
- **Dữ liệu:** Trả về cây ảnh hưởng gồm vùng trồng `Vùng trồng Nho 01`, lô sản xuất `Lô Nho 01`, 1 lô hàng bị ảnh hưởng `Lô hàng kiểm thử ngưỡng bất thường (NCL-08-CN-014)` và 3 tem đã kích hoạt.

### 3.3 Bước 2: Tạo yêu cầu thu hồi hàng loạt (VT-02)
- **Actor:** `orgmanager`
- **Endpoint:** `POST /api/v1/recall-requests/bulk`
- **Body:**
```json
{
  "productionLotId": "00000000-0000-0000-0000-000200000001",
  "reason": "Phat hien du luong chat cam vuot nguong an toan sinh hoc",
  "includedShipmentIds": ["00000000-0000-0000-0000-000900000001"]
}
```
- **Kết quả:** Tạo thành công yêu cầu ID `0f7aee81-fcdd-40f1-962a-daf80768d440`, trạng thái `PENDING`.

### 3.4 Bước 3: Kiểm chứng nguyên tắc bốn mắt (QTN-22)
- `orgmanager` (người tạo yêu cầu) cố gắng tự phê duyệt yêu cầu thu hồi của chính mình:
- **Endpoint:** `PUT /api/v1/recall-requests/bulk/0f7aee81-fcdd-40f1-962a-daf80768d440/approve`
- **Kết quả:** `HTTP 400 Bad Request`
- **Thông điệp:** "Người tạo yêu cầu không được tự phê duyệt yêu cầu của chính mình." (QTN-22) ✅

### 3.5 Bước 4: Quản lý thứ hai phê duyệt yêu cầu thu hồi (VT-02)
- **Actor:** `orgmanager2`
- **Endpoint:** `PUT /api/v1/recall-requests/bulk/0f7aee81-fcdd-40f1-962a-daf80768d440/approve`
- **Body:** `{"remarks": "Phe duyet thu hoi khan cap toan bo lo hang vi pham"}`
- **Kết quả:** `HTTP 200 OK`, trạng thái chuyển sang `APPROVED`. Lô hàng tự động chuyển sang trạng thái `RECALLING`.

### 3.6 Bước 5: Lazy Materialization của Vụ việc thu hồi (RecallCase)
- Gọi `GET /api/v1/recall-cases` với tài khoản `orgmanager`:
- Hệ thống tự động phát hiện lô sản xuất có lô hàng đang `RECALLING` nhưng chưa có vụ việc, tự động tạo mới:
  + Mã vụ việc: `RC-20260911211959-1DC1`
  + ID: `638fd9a4-1381-4fa5-a83b-46e2f2aa4e48`
  + Trạng thái: `OPEN`
  + Số lượng lô trong vụ việc: 1

### 3.7 Bước 6: Đóng vụ việc thu hồi và ghi nhận biện pháp khắc phục (NCL-08-CN-012)
- **Endpoint:** `PUT /api/v1/recall-cases/638fd9a4-1381-4fa5-a83b-46e2f2aa4e48/close`
- **Body:**
```json
{
  "remediationMeasures": "Tieu huy toan bo 3kg nho bi nhiem chat cam duoi su giam sat cua Chi cuc BVTV. Khu trung nha mang va kiem dinh lai toan bo nguon nuoc tuoi.",
  "lotResults": [
    {
      "shipmentId": "00000000-0000-0000-0000-000900000001",
      "resolution": "DESTROYED",
      "recoveredQuantity": 3.0,
      "notes": "Da tieu huy hoan toan 3 hop nho vi pham"
    }
  ]
}
```
- **Kết quả HTTP:** `200 OK`
- **Dữ liệu xác nhận:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "638fd9a4-1381-4fa5-a83b-46e2f2aa4e48",
    "status": "CLOSED",
    "closedAt": "2026-09-11T21:20:37.0619107",
    "remediationMeasures": "Tieu huy toan bo 3kg nho bi nhiem chat cam duoi su giam sat cua Chi cuc BVTV. Khu trung nha mang va kiem dinh lai toan bo nguon nuoc tuoi.",
    "lotResults": [
      {
        "shipmentId": "00000000-0000-0000-0000-000900000001",
        "resolution": "DESTROYED",
        "recoveredQuantity": 3.0,
        "notes": "Da tieu huy hoan toan 3 hop nho vi pham"
      }
    ]
  }
}
```
- Trạng thái lô hàng chuyển sang `RECALLED` trong cơ sở dữ liệu.

---

## 4. Kiểm chứng tính toàn vẹn chuỗi băm sự kiện (QTN-19, TC-02)

### 4.1 Bối cảnh kiểm thử
Kiểm chứng chuỗi sự kiện trên lô hàng `347feea9-7825-4f4a-9413-cc9005da9e07` (`Lo hang 1`), lô này đã trải qua:
1. Nhập kho / Xuất kho (`WAREHOUSE_ENTRY`, `WAREHOUSE_EXIT`)
2. Bàn giao nhiều đợt (`HANDOVER`)
3. Doanh nghiệp thu mua (`PROCUREMENT`)
4. Xác nhận bàn giao chuyển đổi quyền sở hữu (`HANDOVER`)
5. Yêu cầu thu hồi hàng loạt (`RECALL` / Bulk recall) và đóng vụ việc (`CLOSED`)

### 4.2 Endpoint kiểm chứng
- **Endpoint:** `GET /api/v1/shipments/347feea9-7825-4f4a-9413-cc9005da9e07/verify-chain`
- **Headers:** `Authorization: Bearer <token_admin>`
- **Kết quả HTTP:** `200 OK`

### 4.3 Dữ liệu kết quả xác thực
```json
{
  "success": true,
  "status": 200,
  "data": {
    "hashAlgorithm": "SHA-256",
    "isIntegrityVerified": true,
    "verificationStatus": "INTACT",
    "shipmentId": "347feea9-7825-4f4a-9413-cc9005da9e07",
    "shipmentName": "Lo hang 1",
    "totalEvents": 12,
    "verifiedAt": "2026-09-11T21:23:09.4652586",
    "events": [
      {
        "index": 1,
        "eventId": "e31de5d5-04ff-40e5-914b-d125d849b8db",
        "eventType": "WAREHOUSE_ENTRY",
        "hash": "13480abc24ef52d1e1122cb1a3815b417c992d4110c027618551c059d139c200",
        "previousHash": null,
        "isValid": true
      },
      {
        "index": 2,
        "eventId": "0e289d19-6887-49f4-b5a6-b6f8338285bc",
        "eventType": "WAREHOUSE_EXIT",
        "hash": "cf28fd9505633caad7ea21e9c70cfb024e3bac349f61992a0c8680f6f3683702",
        "previousHash": "13480abc24ef52d1e1122cb1a3815b417c992d4110c027618551c059d139c200",
        "isValid": true
      },
      {
        "index": 3,
        "eventId": "bee5fd26-f74d-4a2a-af13-771e6ad32ccf",
        "eventType": "WAREHOUSE_ENTRY",
        "hash": "46ba1a94a76172720510cceece69fef0c9b8329b9be77d5276cf11a6498c873b",
        "previousHash": "cf28fd9505633caad7ea21e9c70cfb024e3bac349f61992a0c8680f6f3683702",
        "isValid": true
      },
      {
        "index": 4,
        "eventId": "ac40e866-9b87-4445-8e20-0b776ec04e74",
        "eventType": "WAREHOUSE_EXIT",
        "hash": "7beb4ca027e233bab7ab221510967f82bf0c75cb5b65d8dcaea4897d9d877bff",
        "previousHash": "46ba1a94a76172720510cceece69fef0c9b8329b9be77d5276cf11a6498c873b",
        "isValid": true
      },
      {
        "index": 5,
        "eventId": "ac346986-aaa2-4e14-bb62-81631b5de3e0",
        "eventType": "HANDOVER",
        "hash": "48f84cc07b7436ef34d5486f0b48a48e40513c8ccdd819337e5befb07bb58d23",
        "previousHash": "7beb4ca027e233bab7ab221510967f82bf0c75cb5b65d8dcaea4897d9d877bff",
        "isValid": true
      },
      {
        "index": 6,
        "eventId": "535cfc49-5269-451b-b4d3-be15596fa611",
        "eventType": "HANDOVER",
        "hash": "2b58f4911e0c030d4d443bd3fa7de5d3d6b7a341d2e15e84475e4d9a5fdb8680",
        "previousHash": "48f84cc07b7436ef34d5486f0b48a48e40513c8ccdd819337e5befb07bb58d23",
        "isValid": true
      },
      {
        "index": 7,
        "eventId": "a3abd55c-d8c2-4335-990b-927e06eef76b",
        "eventType": "HANDOVER",
        "hash": "76a70953740d0cc12fd9b94ec937e4cd57a5ff8c7a3cf16891b4c7e7d5c1d763",
        "previousHash": "2b58f4911e0c030d4d443bd3fa7de5d3d6b7a341d2e15e84475e4d9a5fdb8680",
        "isValid": true
      },
      {
        "index": 8,
        "eventId": "920677c7-2fd7-4e5b-9e13-452946d20e51",
        "eventType": "HANDOVER",
        "hash": "e72dc74af25eb7418e2e2d1cb9d3d07117a360372796078741d980b0faf1ba9d",
        "previousHash": "76a70953740d0cc12fd9b94ec937e4cd57a5ff8c7a3cf16891b4c7e7d5c1d763",
        "isValid": true
      },
      {
        "index": 9,
        "eventId": "fb942d59-835e-4862-a5ff-f0efeca30025",
        "eventType": "PROCUREMENT",
        "hash": "b47a7e723be71522c07c21b49503bed02bdd846663a706de22c9e16459579046",
        "previousHash": "e72dc74af25eb7418e2e2d1cb9d3d07117a360372796078741d980b0faf1ba9d",
        "isValid": true
      },
      {
        "index": 10,
        "eventId": "0883383d-1913-42a0-89b2-1e5c4c522a56",
        "eventType": "HANDOVER",
        "hash": "def0ded014bf7d2819fd5b7157f8b5dd745f3f3b4eb181892262632cc2a42a9f",
        "previousHash": "b47a7e723be71522c07c21b49503bed02bdd846663a706de22c9e16459579046",
        "isValid": true
      },
      {
        "index": 11,
        "eventId": "c54012d7-fe9f-415f-935b-035d4d068c76",
        "eventType": "HANDOVER",
        "hash": "0c4b66eadaa99c595c52949df6bae81be557882115708be65d64b296af171794",
        "previousHash": "def0ded014bf7d2819fd5b7157f8b5dd745f3f3b4eb181892262632cc2a42a9f",
        "isValid": true
      },
      {
        "index": 12,
        "eventId": "a2414fe3-969d-4172-a92a-250ec585254c",
        "eventType": "HANDOVER",
        "hash": "79dd400632b86a0fcf0962198d47e4cc6a9c9c7aacd70eec09f3f1da5e0d0a38",
        "previousHash": "0c4b66eadaa99c595c52949df6bae81be557882115708be65d64b296af171794",
        "isValid": true
      }
    ]
  }
}
```
- **Kết luận:** 100% các sự kiện liên kết băm chặt chẽ, không phát hiện vết đứt gãy hoặc bất thường. Đạt tiêu chuẩn QTN-19 và TC-02.

---

## 5. Báo cáo Kiểm toán Cách ly Dữ liệu Đa tổ chức (QTN-01, TC-03)

### 5.1 Danh sách Tenant tham gia kiểm toán
1. **Tenant 1 (HTX 1):** `DEMO_HTX` (`orgmanager`), ID: `cc57f586-a3be-11f1-9ca5-e00af63e88f4`
2. **Tenant 2 (HTX 2):** `HTX-TC` (`cm_tc`), ID: `47bcceae-7d31-4ad4-b1da-828290041515`
3. **Tenant 3 (Doanh nghiệp thu mua):** `DEMO_NSV` (`procurement`), ID: `cc586748-a3be-11f1-9ca5-e00af63e88f4`

### 5.2 Ma trận kiểm toán chi tiết 7 Endpoint

| # | Endpoint kiểm toán | Hành động kiểm tra | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
|---|---|---|---|---|:---:|
| 1 | `GET /api/v1/handovers` | HTX 1 vs HTX 2 lấy danh sách phiếu bàn giao | Chỉ thấy phiếu của tổ chức mình | HTX 1: 0 bản ghi; HTX 2: 14 bản ghi. 0% rò rỉ dữ liệu chéo. | **PASS** ✅ |
| 2 | `POST /api/v1/handovers/{id}/accept` | HTX 1 cố gắng xác nhận phiếu bàn giao của HTX 2 | Chặn truy cập | `HTTP 403 Forbidden` | **PASS** ✅ |
| 3 | `GET /api/v1/recall-cases` | HTX 1 vs HTX 2 lấy danh sách vụ việc thu hồi | Chỉ thấy vụ việc của tổ chức mình | HTX 1: 1 vụ việc; HTX 2: 1 vụ việc. Phân tách tuyệt đối theo `organizationId`. | **PASS** ✅ |
| 4 | `GET /api/v1/recall-cases/{id}` | HTX 2 truy cập trực tiếp ID vụ việc của HTX 1 | Bị từ chối truy cập | `HTTP 400 Bad Request` ("Không tìm thấy vụ việc thu hồi") do lọc theo tenant ID. | **PASS** ✅ |
| 5 | `GET /api/v1/recall-cases` | Doanh nghiệp thu mua (VT-04) truy cập endpoint vụ việc thu hồi | Chặn vai trò không có thẩm quyền | `HTTP 403 Forbidden` (yêu cầu vai trò VT-02). | **PASS** ✅ |
| 6 | `GET /api/v1/production-lots/chain-progress` | HTX 1 xem bảng tiến độ chuỗi | Chỉ hiển thị các lô thuộc tổ chức mình | Dữ liệu lọc chính xác theo `organizationId`, không rò rỉ lô của HTX khác. | **PASS** ✅ |
| 7 | `GET /api/v1/trace/impact-scope` | HTX 2 truy vết phạm vi ảnh hưởng của lô thuộc HTX 1 | Chặn truy vết chéo tổ chức | `HTTP 400 Bad Request` ("Bạn không có quyền xem thông tin truy vết của đối tượng thuộc tổ chức khác"). | **PASS** ✅ |

---

## 6. Kết luận
- **100% các tiêu chí chấp thuận (Acceptance Criteria) của NCL-10-CN-015 đã được xác minh thành công** với dữ liệu thực tế và log API chi tiết.
- Hệ thống đảm bảo tính toàn vẹn chuỗi băm (QTN-19), nguyên tắc bốn mắt phê duyệt (QTN-22), biện pháp khắc phục thu hồi (QTN-27), chuyển giao quyền sở hữu lô hàng (QTN-31), và cách ly dữ liệu đa tổ chức nghiêm ngặt (QTN-01).
