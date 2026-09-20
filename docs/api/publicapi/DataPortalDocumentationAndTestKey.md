# Tài liệu Cổng dữ liệu đối tác và Khóa thử nghiệm (Data Portal Documentation & Test Key)

> **User Story ID:** NCL-12-CN-004  
> **Epic:** NCL-12 — Cổng dữ liệu và hồ sơ theo lược đồ chuẩn  
> **Áp dụng quy tắc nghiệp vụ:** QTN-20 (Kiểm soát truy cập bên thứ ba & Rate Limiting), QTN-12 (Truy cập công khai chỉ đọc)  
> **Trạng thái hợp đồng:** Nguồn sự thật hợp đồng API (Single Source of Truth) cho NCL-12-CN-004  

---

## 1. Tổng quan Cổng dữ liệu đối tác (Data Portal Overview)

### 1.1. Mục đích và đối tượng sử dụng
Cổng dữ liệu đối tác (**Partner Data Portal**) của hệ thống Nguồn Gốc Số cung cấp giao diện lập trình ứng dụng (RESTful API) mở nhưng có kiểm soát, cho phép các bên thứ ba — đặc biệt là **Doanh nghiệp thu mua**, hệ thống siêu thị, đối tác logistics và sàn thương mại điện tử — kết nối tự động, tích hợp dữ liệu truy xuất nguồn gốc nông sản và hồ sơ lô hàng vào hệ thống quản lý nội bộ (ERP, WMS, SCM) mà không cần thao tác thủ công.

### 1.2. Base URL
- **Môi trường Production (Chính thức):** `https://agri-trace.online`
- **Môi trường Staging (Kiểm thử):** `https://staging.agri-trace.online`
- **Môi trường phát triển / thử nghiệm (Local / Dev):** `http://localhost` (hoặc `http://localhost:8080` khi gọi trực tiếp backend)


### 1.3. Cơ chế xác thực (Authentication Mechanism)
- Các endpoint cổng dữ liệu đối tác (`/api/v1/partner/**`) sử dụng cơ chế xác thực qua **Header `X-API-KEY`**.
- Đối tác không cần đăng nhập hay gửi kèm mã phiên/JWT Bearer Token, chỉ cần gửi khóa bí mật trong tiêu đề HTTP:
  ```http
  X-API-KEY: nks_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
  ```
- Đối với khóa thử nghiệm (Sandbox/Test Key), định dạng khóa luôn có tiền tố `nks_test_`:
  ```http
  X-API-KEY: nks_test_yyyyyyyyyyyyyyyyyyyyyyyyyyyy
  ```
- Nếu thiếu hoặc truyền sai API Key, hệ thống phản hồi mã lỗi `401 Unauthorized` kèm mô tả chi tiết bằng tiếng Việt.

### 1.4. Kiểm soát hạn mức gọi (Rate Limiting — QTN-20)
- Nhằm đảm bảo an toàn tài nguyên và tính sẵn sàng của hệ thống, mỗi khóa truy cập được cấu hình hạn mức số lượt gọi tối đa trong 1 giờ (`rate_limit_per_hour`).
- **Khóa thật (Live Key):** Hạn mức do Quản lý Hợp tác xã thiết lập khi cấp (ví dụ: 500 - 5.000 lượt/giờ tùy thỏa thuận hợp tác).
- **Khóa thử nghiệm (Test Key):** Luôn áp dụng **hạn mức thấp** (mặc định 60 lượt/giờ, tối đa không quá 100 lượt/giờ) và **thời hạn ngắn** (mặc định 7 ngày, tối đa không quá 30 ngày).
- Khi vượt quá hạn mức cho phép, hệ thống từ chối xử lý và phản hồi ngay lập tức mã lỗi `429 Too Many Requests` (QTN-20).
- Hệ thống gửi kèm các HTTP response headers để đối tác theo dõi hạn mức:
  - `X-RateLimit-Limit`: Hạn mức tối đa được phép gọi trong 1 giờ.
  - `X-RateLimit-Remaining`: Số lượt gọi còn lại trong khung giờ hiện tại.
  - `X-RateLimit-Reset`: Thời gian còn lại (tính bằng giây) trước khi bộ đếm hạn mức được đặt lại.

### 1.5. Bảng ánh xạ trường theo lược đồ mô phỏng GS1 (GS1 Simulated Schema Mapping)
Hệ thống Nguồn Gốc Số hỗ trợ xuất dữ liệu truy xuất và dòng sự kiện chuỗi cung ứng theo lược đồ mô phỏng hướng chuẩn GS1 (EPCIS / GS1 XML/JSON) nhằm hỗ trợ đối tác chuẩn hóa dữ liệu.

> **Ghi chú quan trọng:** Đây là lược đồ mô phỏng (Simulated Schema) phục vụ mục đích tích hợp kỹ thuật và chuẩn hóa dữ liệu giáo dục/thực nghiệm, không thay thế cho chứng nhận tuân thủ chính thức của tổ chức GS1 toàn cầu.

| Trường hệ thống Nguồn Gốc Số | Trường lược đồ GS1 mô phỏng | Kiểu dữ liệu | Ý nghĩa trong chuỗi cung ứng |
|:---|:---|:---|:---|
| `ChainEvent.id` | `eventIdentifier` | String (UUID) | Định danh duy nhất của sự kiện |
| `ChainEvent.eventType` | `eventTypeCode` | String | Mã loại sự kiện: `PLANTING`, `FERTILIZING`, `PESTICIDE`, `HARVESTING`, `PACKAGING`, `PROCUREMENT`, `TRANSPORT` |
| `ChainEvent.recordedAt` | `eventDateTime` | String (ISO-8601) | Thời điểm ghi nhận sự kiện (when) |
| `ChainEvent.recordedBy.fullName` | `actorName` | String | Người chịu trách nhiệm ghi nhận (who) |
| `ChainEvent.location.latitude` | `eventLocation.latitude` | Double | Tọa độ vĩ độ diễn ra sự kiện (where) |
| `ChainEvent.location.longitude` | `eventLocation.longitude` | Double | Tọa độ kinh độ diễn ra sự kiện (where) |
| `ChainEvent.eventData` | `details` | Object / Key-Value | Chi tiết kỹ thuật của sự kiện (why/what) |
| `Shipment.name` | `shipmentName` | String | Tên lô hàng thương mại |
| `Shipment.totalQuantity` | `declaredQuantity` | Long | Sản lượng / số lượng công bố |
| `Shipment.status` | `shipmentStatus` | String | Trạng thái lô hàng (`DRAFT`, `ACTIVATED`, `RECALLED`) |
| `ProductionLot.name` | `productionLotName` | String | Tên lô sản xuất tại nông trại |
| `InspectionRequest.inspectionUnit` | `inspections[].inspectionUnit` | String | Đơn vị thực hiện kiểm nghiệm chất lượng |
| `InspectionCriterion.criterionName` | `inspections[].criteria[].criterionName` | String | Tên chỉ tiêu kiểm nghiệm an toàn thực phẩm |
| `InspectionCriterionResult.passed` | `inspections[].criteria[].passed` | Boolean | Kết quả kiểm nghiệm đạt (`true`) hay không đạt (`false`) |

---

## 2. Danh sách Endpoints cổng dữ liệu đối tác (Partner Data Endpoints)

Cổng dữ liệu đối tác hiện bao gồm 3 điểm truy xuất chính dành cho bên thứ ba tích hợp:

```text
1. GET /api/v1/partner/production-lots/{lotId}/dossier  — Lấy hồ sơ truy xuất đầy đủ của Lô sản xuất
2. GET /api/v1/partner/trace/{codeValue}                — Tra cứu hành trình theo Mã tem truy xuất
3. GET /api/v1/partner/shipments/{shipmentId}/dossier/gs1 — Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1
```

---

### Endpoint 1: Lấy hồ sơ truy xuất Lô sản xuất

- **Method:** `GET`
- **Path:** `/api/v1/partner/production-lots/{lotId}/dossier`
- **Mô tả:** Trả về toàn bộ hồ sơ truy xuất nguồn gốc của một Lô sản xuất bao gồm thông tin chi tiết lô, hợp tác xã sở hữu, thông tin vùng trồng, chứng nhận chất lượng và tóm tắt lịch sử nhật ký canh tác.
- **Xác thực:** Bắt buộc Header `X-API-KEY`.
- **Phân quyền & Cách ly dữ liệu (Tenant Isolation):**
  - Khóa thật (Live Key): Chỉ truy xuất được các lô sản xuất thuộc sở hữu của Hợp tác xã đã cấp khóa đó. Truy xuất lô ngoài phạm vi sẽ bị từ chối `400 Bad Request`.
  - Khóa thử nghiệm (Test Key / Sandbox - tiền tố `nks_test_`): **Chỉ cho phép truy cập với mã lô `sample-lot-001`** (cổng `/api/publicapi/v1/lots/sample-lot-001`). Nếu gọi với bất kỳ mã lô nào khác, hệ thống sẽ trả về lỗi `403 Forbidden` kèm thông điệp: `"Khóa thử nghiệm chỉ được phép truy cập mã lô \"sample-lot-001\". Vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa API thật."`. Khi gọi đúng `sample-lot-001`, hệ thống trả về bộ dữ liệu mẫu Sandbox với cờ `is_test: true`.

#### Tham số (Parameters)
| Tên tham số | Vị trí | Kiểu | Bắt buộc | Mô tả |
|:---|:---|:---|:---:|:---|
| `X-API-KEY` | Header | String | Có | Khóa truy cập đối tác (Live hoặc Test) |
| `lotId` | Path | UUID | Có | Định danh UUID của Lô sản xuất cần truy xuất |

#### Phản hồi thành công (HTTP 200 OK — Khóa thật)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "lotInfo": {
      "lotId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "lotName": "Lô Xoài Cát Hòa Lộc Vụ Thu Đông 2026",
      "productCategoryName": "Xoài Cát Hòa Lộc",
      "expectedQuantity": 15000.0,
      "actualQuantity": 14200.0,
      "quantityUnit": "KG",
      "plantingDate": "2026-03-15",
      "harvestDate": "2026-08-20",
      "status": "HARVESTED"
    },
    "organizationInfo": {
      "organizationId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      "organizationName": "Hợp Tác Xã Nông Nghiệp Xanh Tiền Giang",
      "organizationCode": "HTX-TG-01",
      "address": "Xã Hòa Hưng, Huyện Cái Bè, Tiền Giang",
      "phone": "02733888999",
      "email": "contact@htxxanhtiengiang.vn"
    },
    "farmAreaInfo": {
      "farmAreaId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
      "farmAreaName": "Khu Vực Trồng Xoài Thửa 05",
      "area": 3.5,
      "areaUnit": "HECTARE"
    },
    "certifications": [
      {
        "certificationName": "Chứng nhận VietGAP Trồng Trọt",
        "standardName": "VietGAP",
        "certificateCode": "VG-2026-TG-089",
        "issueDate": "2026-01-10",
        "expiryDate": "2027-01-10",
        "issuedBy": "Trung tâm Giám định và Chứng nhận Nông nghiệp"
      }
    ],
    "farmLogSummary": {
      "totalLogsRecorded": 42,
      "lastActivityAt": "2026-08-20T16:30:00"
    },
    "isTest": false
  },
  "timestamp": "2026-09-14T10:00:00.000Z"
}
```

#### Phản hồi thành công trên Chế độ Thử nghiệm (HTTP 200 OK — Khóa thử nghiệm)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "lotInfo": {
      "lotId": "00000000-0000-0000-0000-000000000001",
      "lotName": "[DỮ LIỆU MẪU] Lô Xoài Cát Chu Thử Nghiệm",
      "productCategoryName": "Xoài Cát Chu",
      "expectedQuantity": 10000.0,
      "actualQuantity": 9800.0,
      "quantityUnit": "KG",
      "plantingDate": "2026-02-01",
      "harvestDate": "2026-07-15",
      "status": "HARVESTED"
    },
    "organizationInfo": {
      "organizationId": "00000000-0000-0000-0000-000000000002",
      "organizationName": "[DỮ LIỆU MẪU] Hợp Tác Xã Trái Cây Mẫu Nguồn Gốc Số",
      "organizationCode": "HTX-TEST-DEMO",
      "address": "Khu Thực Nghiệm Công Nghệ Nông Nghiệp Số",
      "phone": "0901234567",
      "email": "sandbox@nguongocso.vn"
    },
    "farmAreaInfo": {
      "farmAreaId": "00000000-0000-0000-0000-000000000003",
      "farmAreaName": "[DỮ LIỆU MẪU] Vùng Canh Tác Thực Nghiệm A1",
      "area": 2.0,
      "areaUnit": "HECTARE"
    },
    "certifications": [
      {
        "certificationName": "[DỮ LIỆU MẪU] Chứng nhận VietGAP Mẫu",
        "standardName": "VietGAP",
        "certificateCode": "VG-TEST-9999",
        "issueDate": "2026-01-01",
        "expiryDate": "2027-01-01",
        "issuedBy": "Hệ Thống Kiểm Nghiệm Thử Nghiệm"
      }
    ],
    "farmLogSummary": {
      "totalLogsRecorded": 25,
      "lastActivityAt": "2026-07-15T10:00:00"
    },
    "isTest": true,
    "testNotice": "Dữ liệu thử nghiệm (Sandbox Mode) - Không phải dữ liệu thực tế"
  },
  "timestamp": "2026-09-14T10:00:00.000Z"
}
```

---

### Endpoint 2: Tra cứu hành trình theo Mã tem truy xuất

- **Method:** `GET`
- **Path:** `/api/v1/partner/trace/{codeValue}`
- **Mô tả:** Tra cứu dữ liệu công khai theo mã tem truy xuất in trên bao bì sản phẩm (TraceCode), cung cấp dòng sự kiện chuỗi cung ứng (Chain Events) và kết quả kiểm nghiệm nếu có.
- **Xác thực:** Bắt buộc Header `X-API-KEY`.
- **Hành vi Sandbox:** Nếu gọi bằng khóa thử nghiệm, hệ thống luôn trả dữ liệu mẫu của hành trình thử nghiệm kèm cờ `isTest: true`.

#### Tham số (Parameters)
| Tên tham số | Vị trí | Kiểu | Bắt buộc | Mô tả |
|:---|:---|:---|:---:|:---|
| `X-API-KEY` | Header | String | Có | Khóa truy cập đối tác |
| `codeValue` | Path | String | Có | Mã truy xuất (ví dụ: `HX00000029` hoặc mã mẫu `TEST-TRACE-001`) |
| `latitude` | Query | Double | Không | Tọa độ vĩ độ của điểm quét |
| `longitude` | Query | Double | Không | Tọa độ kinh độ của điểm quét |

#### Phản hồi thành công (HTTP 200 OK — Khóa thử nghiệm)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "codeValue": "TEST-TRACE-001",
    "productName": "[DỮ LIỆU MẪU] Chè Xanh Long Cốc Thử Nghiệm",
    "shipmentCode": "LH-TEST-2026",
    "shipmentStatus": "ACTIVATED",
    "recalled": false,
    "recallMessage": null,
    "locked": false,
    "lockReason": null,
    "events": [
      {
        "eventType": "HARVESTING",
        "eventData": {
          "field": "Đồi chè Long Cốc Thử Nghiệm",
          "technique": "Hái thủ công 1 tôm 2 lá"
        },
        "recordedAt": "2026-07-20T08:00:00"
      },
      {
        "eventType": "PACKAGING",
        "eventData": {
          "packagingType": "Hút chân không túi thiếc 100g",
          "facility": "Xưởng chế biến chè thử nghiệm"
        },
        "recordedAt": "2026-07-21T14:30:00"
      },
      {
        "eventType": "TRANSPORT",
        "eventData": {
          "fromLocation": "Tân Sơn, Phú Thọ",
          "toLocation": "Kho trung chuyển Hà Nội"
        },
        "recordedAt": "2026-07-22T09:00:00"
      }
    ],
    "inspections": [
      {
        "criterionName": "Dư lượng thuốc bảo vệ thực vật",
        "passed": true,
        "resultDate": "2026-07-19T10:00:00"
      }
    ],
    "isTest": true,
    "testNotice": "Dữ liệu thử nghiệm (Sandbox Mode) - Không phải dữ liệu thực tế"
  },
  "timestamp": "2026-09-14T10:00:00.000Z"
}
```

---

### Endpoint 3: Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1

- **Method:** `GET`
- **Path:** `/api/v1/partner/shipments/{shipmentId}/dossier/gs1`
- **Mô tả:** Cho phép đối tác bên thứ ba trích xuất hồ sơ truy xuất lô hàng thương mại dưới dạng JSON hoặc XML theo lược đồ mô phỏng hướng chuẩn GS1 EPCIS.
- **Xác thực:** Bắt buộc Header `X-API-KEY`.
- **Hành vi Sandbox:** Nếu gọi bằng khóa thử nghiệm, hệ thống trả về hồ sơ GS1 mẫu với `isTest: true`.

#### Tham số (Parameters)
| Tên tham số | Vị trí | Kiểu | Bắt buộc | Mặc định | Mô tả |
|:---|:---|:---|:---:|:---|:---|
| `X-API-KEY` | Header | String | Có | - | Khóa truy cập đối tác |
| `shipmentId` | Path | UUID | Có | - | Định danh lô hàng cần xuất hồ sơ |
| `format` | Query | String | Không | `json` | Định dạng xuất: `json` hoặc `xml` |
| `includeMapping` | Query | Boolean | Không | `true` | Có kèm bảng ánh xạ chi tiết từng trường hay không |

#### Phản hồi thành công (HTTP 200 OK — Khóa thử nghiệm)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipment": {
      "shipmentId": "00000000-0000-0000-0000-000000000010",
      "shipmentCode": "LH-TEST-GS1",
      "shipmentName": "[DỮ LIỆU MẪU] Lô Hàng Xoài Cát Xuất Khẩu Thử Nghiệm",
      "declaredQuantity": 5000,
      "shipmentStatus": "ACTIVATED",
      "organization": {
        "organizationId": "00000000-0000-0000-0000-000000000002",
        "organizationCode": "HTX-TEST-DEMO",
        "organizationName": "[DỮ LIỆU MẪU] Hợp Tác Xã Trái Cây Mẫu Nguồn Gốc Số"
      }
    },
    "events": [
      {
        "eventIdentifier": "00000000-0000-0000-0000-000000000021",
        "eventTypeCode": "HARVESTING",
        "eventDateTime": "2026-07-20T08:00:00",
        "actorName": "Kỹ thuật viên Thử nghiệm",
        "eventLocation": {
          "latitude": 10.352,
          "longitude": 105.987,
          "address": null
        },
        "details": {
          "yield": "5000 KG"
        }
      }
    ],
    "inspections": [
      {
        "inspectionUnit": "Trung Tâm Kiểm Nghiệm Thực Nghiệm",
        "sampleSentDate": "2026-07-18",
        "status": "PASSED",
        "criteria": [
          {
            "criterionCode": "CT-TEST-01",
            "criterionName": "Dư lượng kim loại nặng",
            "passed": true,
            "resultDate": "2026-07-19",
            "expiryDate": "2027-07-19"
          }
        ]
      }
    ],
    "schemaMapping": {
      "standard": "GS1_SIMULATED_V1",
      "complianceNote": "Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ chính thức GS1"
    },
    "warnings": [],
    "isTest": true,
    "testNotice": "Dữ liệu thử nghiệm (Sandbox Mode) - Không phải dữ liệu thực tế"
  },
  "timestamp": "2026-09-14T10:00:00.000Z"
}
```

---

## 3. Cấp và quản lý Khóa thử nghiệm (Test Key Management)

Chức năng cấp khóa thử nghiệm được tích hợp trong phân hệ quản lý Hợp tác xã, đảm bảo quản lý chặt chẽ theo các quy tắc nghiệp vụ.

### 3.1. Endpoint cấp khóa thử nghiệm

- **Method:** `POST`
- **Path:** `/api/v1/organization/api-keys/test`
- **Quyền hạn (Authorization):**
  - Chỉ cho phép vai trò **Quản lý Hợp tác xã (`VT-02`)** hoặc **Quản trị viên nền tảng (`VT-01`)**.
  - **Từ chối tuyệt đối** với vai trò **Người ghi sự kiện (`VT-03`)**, Người tiêu dùng (`VT-06`) hoặc Doanh nghiệp thu mua (`VT-04`) mà không có quyền quản lý (`403 Forbidden` — NCL-12-CN-004-TC-04).
- **Yêu cầu bảo mật:** Người thực hiện phải đăng nhập và gửi kèm Header `Authorization: Bearer <ACCESS_TOKEN>`.

#### Request Body
```json
{
  "partnerName": "Công ty Cổ phần Nông sản Thực phẩm An Toàn",
  "rateLimitPerHour": 60,
  "expiresAt": "2026-09-28T23:59:59"
}
```

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc nghiệp vụ |
|:---|:---|:---:|:---|
| `partnerName` | String | Có | Không để trống, độ dài tối đa 255 ký tự. |
| `rateLimitPerHour` | Integer | Có | Tối thiểu 1, **tối đa không quá 100 lượt/giờ** (hạn mức thấp để tránh lạm dụng). Mặc định gợi ý: 60. |
| `expiresAt` | String (ISO-8601) | Có | Phải ở thời điểm tương lai và **không quá 30 ngày** kể từ ngày tạo (thời hạn ngắn). |

#### Phản hồi thành công (HTTP 201 Created)
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "e8a1b2c3-d4e5-4678-9abc-def012345678",
    "organizationId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    "partnerName": "Công ty Cổ phần Nông sản Thực phẩm An Toàn",
    "keyPrefix": "nks_test_e8a1b2c3",
    "rawApiKey": "nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1",
    "rateLimitPerHour": 60,
    "expiresAt": "2026-09-28T23:59:59",
    "status": "ACTIVE",
    "isTest": true,
    "totalCalls": 0,
    "failedCalls": 0,
    "createdByName": "Nguyễn Văn Quản Lý",
    "createdAt": "2026-09-14T10:00:00"
  },
  "timestamp": "2026-09-14T10:00:00.000Z"
}
```

> **Lưu ý bảo mật:** Trường `rawApiKey` chỉ được trả về **DUY NHẤT 1 LẦN** khi tạo mới thành công. Hệ thống chỉ lưu trữ chuỗi băm SHA-256 (`key_hash`) và tiền tố (`key_prefix`). Người quản lý cần sao chép và chuyển giao khóa ngay cho đối tác.

---

## 4. Hành vi Chế độ Thử nghiệm (Sandbox Mode Behavior)

Khi đối tác gửi request có Header `X-API-KEY` chứa khóa thử nghiệm (`is_test = true` hoặc tiền tố `nks_test_`):

1. **Cách ly dữ liệu thật tuyệt đối:** Hệ thống không truy vấn hay trả về dữ liệu nông trại/lô hàng thật của hợp tác xã. Mọi request đều được điều hướng trả về bộ dữ liệu mẫu chuẩn hóa (`SAMPLE_DATASET`).
2. **Đánh dấu rõ ràng:** Mọi response trả về đều chứa:
   - Thuộc tính boolean `"isTest": true`.
   - Thông báo ghi chú `"testNotice": "Dữ liệu thử nghiệm (Sandbox Mode) - Không phải dữ liệu thực tế"`.
3. **Gọi lấy lô thật bằng khóa thử nghiệm (NCL-12-CN-004-TC-02):** Dù đối tác truyền bất kỳ `lotId` hay `codeValue` nào (kể cả ID của một lô sản xuất thật đang có trong cơ sở dữ liệu), hệ thống vẫn **chỉ trả dữ liệu mẫu** và đánh dấu `isTest: true`.
4. **Hết hạn khóa thử nghiệm (NCL-12-CN-004-TC-03):** Khi thời điểm gọi vượt quá `expiresAt`, hệ thống từ chối ngay tại tầng filter với mã lỗi `HTTP 401 Unauthorized` và thông báo lỗi rõ ràng:
   ```json
   {
     "success": false,
     "status": 401,
     "message": "Khóa thử nghiệm đã hết hạn"
   }
   ```
5. **Khóa thử nghiệm không hợp lệ hoặc chưa được cấp:** Khi đối tác gửi Header `X-API-KEY` chứa khóa thử nghiệm không tồn tại hoặc không đúng, hệ thống từ chối truy cập với mã lỗi `HTTP 401 Unauthorized` kèm hướng dẫn:
   ```json
   {
     "success": false,
     "status": 401,
     "message": "Khóa thử nghiệm không đúng. Vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa."
   }
   ```

---

## 5. Ví dụ gọi thử nghiệm bằng cURL (Sandbox cURL Examples)

Dưới đây là các ví dụ cURL hoàn chỉnh. Đối tác thay thế `<YOUR_TEST_API_KEY>` bằng khóa thử nghiệm do Hợp tác xã cấp (tiền tố `nks_test_...`) để chạy thử trực tiếp từ terminal.

> **💡 Mẹo hiển thị dễ nhìn trên terminal:** Thêm cờ `-s` (silent - tắt thanh tiến trình tải) và nối ống dẫn `| jq .` ở cuối lệnh để terminal tự động thụt lề định dạng JSON và tô màu cú pháp trực quan (yêu cầu máy đã cài sẵn `jq`).

### 5.1. Gọi lấy dữ liệu hồ sơ lô mẫu Sandbox (`/api/publicapi/v1/lots/sample-lot-001`)

#### a) Môi trường Production (Chính thức):
```bash
curl -s -X GET "https://agri-trace.online/api/publicapi/v1/lots/sample-lot-001" \
  -H "Accept: application/json" \
  -H "X-API-KEY: <YOUR_TEST_API_KEY>" | jq .
```

#### b) Môi trường Staging (Kiểm thử trước phát hành):
```bash
curl -s -X GET "https://staging.agri-trace.online/api/publicapi/v1/lots/sample-lot-001" \
  -H "Accept: application/json" \
  -H "X-API-KEY: <YOUR_TEST_API_KEY>" | jq .
```

#### c) Môi trường Localhost (Phát triển cục bộ):
```bash
curl -s -X GET "http://localhost:8080/api/publicapi/v1/lots/sample-lot-001" \
  -H "Accept: application/json" \
  -H "X-API-KEY: <YOUR_TEST_API_KEY>" | jq .
```
*(Nếu gọi qua Nginx reverse proxy của frontend local: dùng `http://localhost:3000`)*.
*(Nếu gọi trực tiếp tới cổng backend Spring Boot: dùng `http://localhost:8080`)*.

---

### 5.2. Gọi lấy hồ sơ lô đối tác (`/api/v1/partner/production-lots/{lotId}/dossier`)
```bash
curl -s -X GET "https://agri-trace.online/api/v1/partner/production-lots/00000000-0000-0000-0000-000000000001/dossier" \
  -H "Accept: application/json" \
  -H "X-API-KEY: nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1" | jq .
```

### 5.3. Gọi tra cứu hành trình mã tem thử nghiệm (`/api/v1/partner/trace/{codeValue}`)
```bash
curl -s -X GET "https://agri-trace.online/api/v1/partner/trace/TEST-TRACE-001" \
  -H "Accept: application/json" \
  -H "X-API-KEY: nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1" | jq .
```

### 5.4. Gọi xuất hồ sơ GS1 mô phỏng (JSON & XML)
```bash
# Định dạng JSON
curl -s -X GET "https://agri-trace.online/api/v1/partner/shipments/00000000-0000-0000-0000-000000000010/dossier/gs1?format=json" \
  -H "Accept: application/json" \
  -H "X-API-KEY: nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1" | jq .

# Định dạng XML
curl -s -X GET "https://agri-trace.online/api/v1/partner/shipments/00000000-0000-0000-0000-000000000010/dossier/gs1?format=xml" \
  -H "Accept: application/xml" \
  -H "X-API-KEY: nks_test_e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1"
```

---

## 6. Bảng mã lỗi tổng hợp (Error Codes Summary)

Hệ thống tuân thủ cấu trúc phản hồi lỗi chuẩn của Nguồn Gốc Số với thông điệp tiếng Việt tường minh:

```json
{
  "success": false,
  "status": 401,
  "message": "Thông điệp lỗi chi tiết",
  "path": "/api/v1/partner/...",
  "timestamp": "2026-09-14T10:00:00.000Z"
}
```

| HTTP Status | Mã lỗi / Tình huống | Thông điệp phản hồi (`message`) | Giải pháp xử lý |
|:---|:---|:---|:---|
| **400 Bad Request** | Lô ngoài phạm vi (Live Key) | `Lô sản xuất nằm ngoài phạm vi truy xuất của khóa truy cập` | Chỉ truy xuất các lô hàng thuộc HTX đã cấp khóa. |
| **400 Bad Request** | Không tìm thấy lô (Live Key) | `Không tìm thấy thông tin lô sản xuất` | Kiểm tra lại tính chính xác của `lotId`. |
| **400 Bad Request** | Dữ liệu đầu vào sai | `Thời hạn khóa thử nghiệm không được vượt quá 30 ngày` | Điều chỉnh tham số đầu vào đúng quy định. |
| **401 Unauthorized** | Thiếu header API key | `Thiếu Header X-API-KEY` | Bổ sung header `X-API-KEY` vào request. |
| **401 Unauthorized** | Khóa không tồn tại | `Khóa truy cập không hợp lệ` | Kiểm tra lại chuỗi API key đã được cấp. |
| **401 Unauthorized** | Khóa đã bị thu hồi | `Khóa truy cập đã bị thu hồi và không còn hiệu lực` | Liên hệ Quản lý HTX để được cấp lại khóa mới. |
| **401 Unauthorized** | Khóa thật hết hạn | `Khóa truy cập đã hết thời gian hiệu lực` | Liên hệ Quản lý HTX để gia hạn hoặc cấp khóa mới. |
| **401 Unauthorized** | **Khóa thử nghiệm hết hạn (TC-03)** | `Khóa thử nghiệm đã hết hạn` | Tạo hoặc yêu cầu cấp lại khóa thử nghiệm mới. |
| **403 Forbidden** | **Sai vai trò cấp khóa (TC-04)** | `Bạn không có quyền thực hiện thao tác này` | Chỉ Quản lý HTX (`VT-02`) hoặc Admin (`VT-01`) được cấp khóa. |
| **422 Unprocessable** | Vi phạm validation cấp khóa | `Hạn mức số lượt gọi thử nghiệm không vượt quá 100 lượt/giờ` | Giảm `rateLimitPerHour` xuống dưới hoặc bằng 100. |
| **429 Too Many Requests** | **Vượt hạn mức giờ (QTN-20)** | `Khóa truy cập đã vượt quá hạn mức {limit} lượt gọi/giờ` | Chờ sang khung giờ tiếp theo hoặc yêu cầu nâng hạn mức. |
| **500 Internal Error** | Lỗi máy chủ nội bộ | `Đã xảy ra lỗi hệ thống, vui lòng thử lại sau` | Liên hệ đội ngũ quản trị kỹ thuật Nguồn Gốc Số. |
