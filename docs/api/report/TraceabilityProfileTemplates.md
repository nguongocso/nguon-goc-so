# 📘 API Docs: Cấu hình mẫu hồ sơ truy xuất nguồn gốc theo yêu cầu đối tác
## NCL-07-CN-007

---

## 1. Thông tin chung

| Thuộc tính | Giá trị |
|---|---|
| **Mã User Story** | `NCL-07-CN-007` |
| **Tên User Story** | Cấu hình trường dữ liệu trong hồ sơ truy xuất theo yêu cầu đối tác |
| **Epic** | NCL-07 — Báo cáo và xuất hồ sơ truy xuất (Quan trọng) |
| **Nhánh Git** | `feature/NCL-07-CN-007-traceability-profile-template-configuration` |
| **Vai trò được phép** | `VT-02` (Quản lý hợp tác xã) |
| **Phạm vi dữ liệu** | Cô lập theo tổ chức (`organization_id`) theo quy tắc `QTN-01` |
| **Quy tắc nghiệp vụ** | `QTN-01` (Cách ly dữ liệu tổ chức), `QTN-11` (Bộ trường bắt buộc không thể bỏ chọn) |

### Tóm tắt nghiệp vụ

Quản lý hợp tác xã (`VT-02`) cần xuất hồ sơ truy xuất nguồn gốc của lô hàng gửi tới các siêu thị (WinMart, Co.opmart, Bách Hóa Xanh...) hoặc các đối tác xuất khẩu (Nhật Bản, Hàn Quốc, EU...). Mỗi đối tác có biểu mẫu và yêu cầu trường thông tin khác nhau.

Tính năng này cung cấp:
1. **Quản lý mẫu hồ sơ (Profile Template):** Tạo, xem, cập nhật, xóa các mẫu hồ sơ với tên mẫu, tên đối tác áp dụng, cờ mẫu mặc định (`isDefault`) và danh sách các trường được chọn từ 7 nhóm dữ liệu hệ thống.
2. **Kiểm soát tính toàn vẹn (QTN-11):** Hệ thống định nghĩa danh mục các trường bắt buộc (`isMandatory = true`). Người dùng **không thể bỏ chọn** các trường này. Nếu yêu cầu tạo/cập nhật thiếu trường bắt buộc, hệ thống chặn lại và trả về lỗi `422 Unprocessable Entity`.
3. **Cách ly dữ liệu (QTN-01):** Các mẫu hồ sơ thuộc quyền sở hữu riêng của từng tổ chức. Tài khoản của tổ chức khác không thể xem hoặc sử dụng mẫu (TC-04).
4. **Xem trước hồ sơ (Dossier Preview):** Xem trước dữ liệu hồ sơ truy xuất theo mẫu đã cấu hình dưới dạng JSON trước khi thực hiện tải tệp PDF chính thức.
5. **Xuất hồ sơ theo mẫu (Dossier Export with Template):** Tích hợp chọn mẫu vào API xuất hồ sơ PDF (`/api/v1/shipments/{shipmentId}/dossier/export?templateId={id}`). Nếu không truyền `templateId`, hệ thống tự động áp dụng mẫu mặc định của tổ chức (TC-03).
6. **Lưu vết lịch sử (Audit Log):** Mọi lượt xuất hồ sơ đều ghi nhận `template_id` và `template_name` vào bảng `dossier_export_history`.

---

## 2. Danh mục trường dữ liệu hồ sơ truy xuất

Hệ thống cung cấp danh mục các trường dữ liệu được phân thành 8 nhóm:

### 2.1 Bảng danh mục trường đầy đủ

| Nhóm trường (`fieldGroup`) | Mã trường (`fieldKey`) | Tên hiển thị tiếng Việt | Bắt buộc (QTN-11) | Mô tả |
|:---|:---|:---|:---:|:---|
| `ORGANIZATION` | `organization.name` | Tên tổ chức / HTX | **Có** | Đơn vị sản xuất, chịu trách nhiệm pháp lý |
| `ORGANIZATION` | `organization.code` | Mã tổ chức | Không | Mã định danh HTX trên hệ thống |
| `ORGANIZATION` | `organization.type` | Loại hình tổ chức | Không | HTX, doanh nghiệp, nông hộ... |
| `ORGANIZATION` | `organization.status` | Trạng thái tổ chức | Không | Trạng thái hoạt động của tổ chức |
| `ORGANIZATION` | `organization.address` | Địa chỉ trụ sở | Không | Địa chỉ hành chính của tổ chức |
| `ORGANIZATION` | `organization.province` | Tỉnh / Thành phố | Không | Tỉnh/thành phố nơi tổ chức hoạt động |
| `ORGANIZATION` | `organization.phone` | Số điện thoại | Không | Số điện thoại liên hệ |
| `ORGANIZATION` | `organization.email` | Email | Không | Email giao dịch |
| `FARM_AREA` | `farmArea.name` | Tên vùng trồng | **Có** | Định danh vùng trồng nơi sản xuất |
| `FARM_AREA` | `farmArea.location` | Tọa độ địa lý | Không | Kinh độ / Vĩ độ vùng trồng |
| `FARM_AREA` | `farmArea.area` | Diện tích canh tác | Không | Diện tích vùng trồng |
| `FARM_AREA` | `farmArea.areaUnit` | Đơn vị diện tích | Không | Đơn vị tính (m2, ha...) |
| `FARM_AREA` | `farmArea.cropType` | Loại cây trồng | Không | Loại giống cây trồng chủ lực |
| `FARM_AREA` | `farmArea.isActive` | Trạng thái vùng trồng | Không | Trạng thái kích hoạt của vùng trồng |
| `PRODUCTION_LOT` | `productionLot.name` | Tên / Mã lô sản xuất | **Có** | Mã định danh lô sản xuất nguồn |
| `PRODUCTION_LOT` | `productionLot.productCategory` | Danh mục sản phẩm | **Có** | Loại sản phẩm nông sản |
| `PRODUCTION_LOT` | `productionLot.plantingDate` | Ngày xuống giống | Không | Ngày gieo trồng / xuống giống |
| `PRODUCTION_LOT` | `productionLot.harvestDate` | Ngày thu hoạch | Không | Ngày bắt đầu thu hoạch |
| `PRODUCTION_LOT` | `productionLot.expectedQuantity` | Sản lượng dự kiến | Không | Sản lượng ước tính ban đầu |
| `PRODUCTION_LOT` | `productionLot.expectedQuantityUnit` | Đơn vị tính sản lượng | Không | Đơn vị tính sản lượng dự kiến |
| `PRODUCTION_LOT` | `productionLot.actualQuantity` | Sản lượng thực tế | Không | Sản lượng thu hoạch thực tế |
| `PRODUCTION_LOT` | `productionLot.status` | Trạng thái lô SX | Không | CLOSED / PACKAGED... |
| `SHIPMENT` | `shipment.name` | Tên lô hàng vận chuyển | **Có** | Tên chuyến hàng / lô hàng giao |
| `SHIPMENT` | `shipment.totalQuantity` | Số lượng lô hàng | **Có** | Tổng số lượng sản phẩm xuất kho |
| `SHIPMENT` | `shipment.packagingInfo` | Quy cách đóng gói | Không | Thông tin bao bì, quy cách đóng gói |
| `SHIPMENT` | `shipment.status` | Trạng thái lô hàng | Không | Trạng thái vận hành của lô hàng |
| `SHIPMENT` | `shipment.createdAt` | Thời điểm tạo lô hàng | Không | Thời điểm lập lô hàng |
| `FARM_LOG` | `farmLog.activityType` | Loại hoạt động canh tác | **Có** | PLANTING, FERTILIZING, PESTICIDE, HARVESTING |
| `FARM_LOG` | `farmLog.executedDate` | Ngày thực hiện canh tác | Không | Thời điểm thực hiện hoạt động |
| `FARM_LOG` | `farmLog.material` | Vật tư nông nghiệp | Không | Tên phân bón, thuốc BVTV, hạt giống |
| `FARM_LOG` | `farmLog.quantity` | Liều lượng / Khối lượng | Không | Số lượng vật tư sử dụng |
| `FARM_LOG` | `farmLog.unit` | Đơn vị tính vật tư | Không | kg, lít, chai, bao... |
| `FARM_LOG` | `farmLog.notes` | Ghi chú kỹ thuật | Không | Hướng dẫn kỹ thuật, thời gian cách ly |
| `FARM_LOG` | `farmLog.attachments` | Chứng từ nhật ký | Không | Tệp hóa đơn, hình ảnh minh chứng |
| `INSPECTION` | `inspection.sampleSentDate` | Ngày gửi mẫu kiểm | Không | Thời điểm gửi mẫu phân tích |
| `INSPECTION` | `inspection.inspectionUnit` | Đơn vị kiểm nghiệm | Không | Tên trung tâm / phòng thí nghiệm |
| `INSPECTION` | `inspection.criterionName` | Chỉ tiêu kiểm nghiệm | Không | Tên chỉ tiêu hoặc quy chuẩn kiểm |
| `INSPECTION` | `inspection.passed` | Kết quả Đạt / Không đạt | Không | Đánh giá Đạt / Không đạt |
| `INSPECTION` | `inspection.resultDate` | Ngày cấp kết quả | Không | Ngày phòng kiểm nghiệm trả kết quả |
| `INSPECTION` | `inspection.expiryDate` | Hạn hiệu lực kiểm nghiệm | Không | Hạn hiệu lực của phiếu phân tích |
| `CERTIFICATION` | `certification.name` | Tên giấy chứng nhận | Không | Tên chứng chỉ hoặc chứng nhận |
| `CERTIFICATION` | `certification.standardName` | Tên tiêu chuẩn | Không | VietGAP, GlobalGAP, Hữu cơ... |
| `CERTIFICATION` | `certification.certificationCode` | Mã số chứng nhận | Không | Số hiệu chứng chỉ được cấp |
| `CERTIFICATION` | `certification.issueDate` | Ngày cấp chứng nhận | Không | Thời điểm chứng nhận có hiệu lực |
| `CERTIFICATION` | `certification.expiryDate` | Ngày hết hạn | Không | Thời điểm chứng nhận hết hiệu lực |
| `CERTIFICATION` | `certification.certifier` | Tổ chức chứng nhận | Không | Đơn vị đánh giá và cấp chứng chỉ |
| `TIMELINE` | `chainEvent.eventType` | Loại sự kiện chuỗi | **Có** | HARVEST, PACKAGING, TRANSPORT, PROCUREMENT |
| `TIMELINE` | `chainEvent.recordedAt` | Thời điểm sự kiện | Không | Thời điểm ghi nhận sự kiện |
| `TIMELINE` | `chainEvent.recordedBy` | Người ghi nhận sự kiện | Không | Họ tên nhân sự ghi nhận |
| `TIMELINE` | `chainEvent.location` | Tọa độ địa điểm sự kiện | Không | Vị trí địa lý nơi diễn ra sự kiện |
| `TIMELINE` | `chainEvent.eventData` | Chi tiết sự kiện | Không | Dữ liệu mở rộng của sự kiện |

### 2.2 Danh sách 8 trường bắt buộc QTN-11 (Không thể bỏ chọn)

```text
1. organization.name               (Tên tổ chức / HTX)
2. farmArea.name                   (Tên vùng trồng)
3. productionLot.name              (Tên / Mã lô sản xuất)
4. productionLot.productCategory   (Danh mục sản phẩm)
5. shipment.name                   (Tên lô hàng vận chuyển)
6. shipment.totalQuantity          (Số lượng lô hàng)
7. farmLog.activityType            (Loại hoạt động canh tác)
8. chainEvent.eventType            (Loại sự kiện chuỗi cung ứng)
```

> ⚠️ **Quy tắc QTN-11:** Mọi Profile Template phải chứa đầy đủ 8 trường trên. Nếu thiếu dù chỉ 1 trường, hệ thống sẽ từ chối tạo/cập nhật với mã lỗi `422 Unprocessable Entity` (TC-02).

---

## 3. Danh sách Endpoints

| STT | Tên API | Method | Endpoint Path | Quyền |
|:---:|:---|:---:|:---|:---:|
| 1 | Lấy danh mục trường hệ thống | `GET` | `/api/v1/organizations/{orgId}/profile-templates/catalog` | `VT-02` |
| 2 | Danh sách mẫu hồ sơ của tổ chức | `GET` | `/api/v1/organizations/{orgId}/profile-templates` | `VT-02` |
| 3 | Tạo mới mẫu hồ sơ | `POST` | `/api/v1/organizations/{orgId}/profile-templates` | `VT-02` |
| 4 | Xem chi tiết mẫu hồ sơ | `GET` | `/api/v1/organizations/{orgId}/profile-templates/{templateId}` | `VT-02` |
| 5 | Cập nhật mẫu hồ sơ | `PUT` | `/api/v1/organizations/{orgId}/profile-templates/{templateId}` | `VT-02` |
| 6 | Xóa mẫu hồ sơ | `DELETE` | `/api/v1/organizations/{orgId}/profile-templates/{templateId}` | `VT-02` |
| 7 | Xem trước hồ sơ theo mẫu | `GET` | `/api/v1/shipments/{shipmentId}/dossier/preview` | `VT-02` |
| 8 | Xuất hồ sơ PDF theo mẫu | `GET` | `/api/v1/shipments/{shipmentId}/dossier/export` | `VT-02` |

---

## 4. Chi tiết các Endpoints

### 4.1 Lấy danh mục trường hệ thống

Lấy toàn bộ các trường dữ liệu mà hệ thống hỗ trợ cấu hình, kèm trạng thái bắt buộc theo QTN-11 để giao diện hiển thị danh sách chọn và khóa các checkbox bắt buộc.

- **URL:** `GET /api/v1/organizations/{orgId}/profile-templates/catalog`
- **Quyền:** `VT-02` (yêu cầu `orgId == currentUser.organizationId`)

#### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "fieldGroup": "ORGANIZATION",
      "groupLabel": "Thông tin tổ chức / Hợp tác xã",
      "fields": [
        {
          "fieldKey": "organization.name",
          "displayName": "Tên tổ chức / HTX",
          "mandatory": true,
          "description": "Đơn vị sản xuất và chịu trách nhiệm pháp lý"
        },
        {
          "fieldKey": "organization.code",
          "displayName": "Mã tổ chức",
          "mandatory": false,
          "description": "Mã định danh nội bộ của tổ chức"
        },
        {
          "fieldKey": "organization.type",
          "displayName": "Loại hình tổ chức",
          "mandatory": false,
          "description": "Loại hình tổ chức (HTX, doanh nghiệp, nông hộ)"
        }
      ]
    },
    {
      "fieldGroup": "FARM_AREA",
      "groupLabel": "Thông tin vùng trồng",
      "fields": [
        {
          "fieldKey": "farmArea.name",
          "displayName": "Tên vùng trồng",
          "mandatory": true,
          "description": "Khu vực địa lý canh tác cây trồng"
        },
        {
          "fieldKey": "farmArea.location",
          "displayName": "Tọa độ địa lý",
          "mandatory": false,
          "description": "Tọa độ GPS vùng trồng"
        }
      ]
    },
    {
      "fieldGroup": "PRODUCTION_LOT",
      "groupLabel": "Thông tin lô sản xuất",
      "fields": [
        {
          "fieldKey": "productionLot.name",
          "displayName": "Tên / Mã lô sản xuất",
          "mandatory": true,
          "description": "Mã định danh lô sản xuất nguồn"
        },
        {
          "fieldKey": "productionLot.productCategory",
          "displayName": "Danh mục sản phẩm",
          "mandatory": true,
          "description": "Chủng loại nông sản"
        }
      ]
    },
    {
      "fieldGroup": "SHIPMENT",
      "groupLabel": "Thông tin lô hàng vận chuyển",
      "fields": [
        {
          "fieldKey": "shipment.name",
          "displayName": "Tên lô hàng",
          "mandatory": true,
          "description": "Tên chuyến hàng / lô hàng vận chuyển"
        },
        {
          "fieldKey": "shipment.totalQuantity",
          "displayName": "Số lượng lô hàng",
          "mandatory": true,
          "description": "Tổng sản lượng lô hàng xuất đi"
        }
      ]
    },
    {
      "fieldGroup": "FARM_LOG",
      "groupLabel": "Nhật ký canh tác",
      "fields": [
        {
          "fieldKey": "farmLog.activityType",
          "displayName": "Loại hoạt động canh tác",
          "mandatory": true,
          "description": "Hoạt động: gieo cấy, bón phân, phun thuốc, thu hoạch"
        },
        {
          "fieldKey": "farmLog.executedDate",
          "displayName": "Ngày thực hiện canh tác",
          "mandatory": false,
          "description": "Thời điểm nông hộ ghi nhận hoạt động"
        },
        {
          "fieldKey": "farmLog.material",
          "displayName": "Vật tư nông nghiệp",
          "mandatory": false,
          "description": "Tên thuốc / phân bón sử dụng"
        }
      ]
    },
    {
      "fieldGroup": "INSPECTION",
      "groupLabel": "Kết quả kiểm nghiệm",
      "fields": [
        {
          "fieldKey": "inspection.sampleSentDate",
          "displayName": "Ngày gửi mẫu",
          "mandatory": false,
          "description": "Thời điểm gửi mẫu kiểm nghiệm"
        },
        {
          "fieldKey": "inspection.inspectionUnit",
          "displayName": "Đơn vị kiểm nghiệm",
          "mandatory": false,
          "description": "Trung tâm phân tích kiểm nghiệm"
        },
        {
          "fieldKey": "inspection.criterionName",
          "displayName": "Chỉ tiêu kiểm nghiệm",
          "mandatory": false,
          "description": "Dư lượng thuốc BVTV, kim loại nặng..."
        },
        {
          "fieldKey": "inspection.passed",
          "displayName": "Kết quả Đạt / Không đạt",
          "mandatory": false,
          "description": "Đánh giá đạt quy chuẩn"
        }
      ]
    },
    {
      "fieldGroup": "CERTIFICATION",
      "groupLabel": "Chứng nhận chất lượng",
      "fields": [
        {
          "fieldKey": "certification.standardName",
          "displayName": "Tên tiêu chuẩn",
          "mandatory": false,
          "description": "VietGAP, GlobalGAP, Organic..."
        },
        {
          "fieldKey": "certification.certificationCode",
          "displayName": "Mã số chứng nhận",
          "mandatory": false,
          "description": "Số hiệu chứng nhận"
        }
      ]
    },
    {
      "fieldGroup": "TIMELINE",
      "groupLabel": "Dòng sự kiện chuỗi cung ứng",
      "fields": [
        {
          "fieldKey": "chainEvent.eventType",
          "displayName": "Loại sự kiện chuỗi",
          "mandatory": true,
          "description": "Thu hoạch, đóng gói, vận chuyển, thu mua"
        },
        {
          "fieldKey": "chainEvent.recordedAt",
          "displayName": "Thời điểm sự kiện",
          "mandatory": false,
          "description": "Thời gian ghi nhận sự kiện trên chuỗi"
        }
      ]
    }
  ],
  "timestamp": "2026-09-14T15:00:00.000Z"
}
```

---

### 4.2 Danh sách mẫu hồ sơ của tổ chức (TC-04)

- **URL:** `GET /api/v1/organizations/{orgId}/profile-templates`
- **Quyền:** `VT-02`
- **Quy tắc QTN-01:** Chỉ trả về danh sách các mẫu thuộc về `orgId` của người dùng. Tuyệt đối không trả về mẫu của tổ chức khác (TC-04).

#### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "e4f8c9b2-1111-4a2a-9f3d-1a2b3c4d5e6f",
      "organizationId": "d53ea88e-8837-4722-acf9-761f0d0c7a48",
      "name": "Mẫu hồ sơ chuẩn Siêu thị WinMart",
      "partnerName": "WinMart Retail",
      "description": "Biểu mẫu yêu cầu đầy đủ thông tin kiểm nghiệm và nhật ký canh tác",
      "isDefault": true,
      "totalFields": 10,
      "createdAt": "2026-09-10T08:30:00",
      "updatedAt": "2026-09-12T14:15:00"
    },
    {
      "id": "f5a9d0c3-2222-4a2a-9f3d-1a2b3c4d5e6f",
      "organizationId": "d53ea88e-8837-4722-acf9-761f0d0c7a48",
      "name": "Mẫu xuất khẩu Nhật Bản (JAS)",
      "partnerName": "Đối tác Nhật Bản - Nichirei Corp",
      "description": "Yêu cầu kiểm nghiệm dư lượng nghiêm ngặt và vị trí tọa độ vùng trồng",
      "isDefault": false,
      "totalFields": 15,
      "createdAt": "2026-09-11T10:00:00",
      "updatedAt": "2026-09-11T10:00:00"
    }
  ],
  "timestamp": "2026-09-14T15:00:00.000Z"
}
```

---

### 4.3 Tạo mới mẫu hồ sơ (TC-01, TC-02)

Tạo mẫu hồ sơ mới cho tổ chức. Hệ thống tự động kiểm tra xem danh sách trường gửi lên có chứa đủ 8 trường bắt buộc của QTN-11 hay không.

- **URL:** `POST /api/v1/organizations/{orgId}/profile-templates`
- **Quyền:** `VT-02`
- **Request Body:**

```json
{
  "name": "Mẫu giao hàng Co.opmart",
  "partnerName": "Saigon Co.op",
  "description": "Biểu mẫu tinh gọn 10 trường giao hàng siêu thị Co.opmart",
  "isDefault": false,
  "selectedFields": [
    { "fieldKey": "organization.name", "fieldGroup": "ORGANIZATION", "fieldOrder": 1 },
    { "fieldKey": "farmArea.name", "fieldGroup": "FARM_AREA", "fieldOrder": 2 },
    { "fieldKey": "productionLot.name", "fieldGroup": "PRODUCTION_LOT", "fieldOrder": 3 },
    { "fieldKey": "productionLot.productCategory", "fieldGroup": "PRODUCTION_LOT", "fieldOrder": 4 },
    { "fieldKey": "productionLot.harvestDate", "fieldGroup": "PRODUCTION_LOT", "fieldOrder": 5 },
    { "fieldKey": "shipment.name", "fieldGroup": "SHIPMENT", "fieldOrder": 6 },
    { "fieldKey": "shipment.totalQuantity", "fieldGroup": "SHIPMENT", "fieldOrder": 7 },
    { "fieldKey": "farmLog.activityType", "fieldGroup": "FARM_LOG", "fieldOrder": 8 },
    { "fieldKey": "inspection.passed", "fieldGroup": "INSPECTION", "fieldOrder": 9 },
    { "fieldKey": "chainEvent.eventType", "fieldGroup": "TIMELINE", "fieldOrder": 10 }
  ]
}
```

#### Response `201 Created` – Thành công (TC-01)

```json
{
  "success": true,
  "status": 201,
  "message": "Tạo mẫu hồ sơ truy xuất thành công.",
  "data": {
    "id": "a1b2c3d4-5555-4a2a-9f3d-1a2b3c4d5e6f",
    "organizationId": "d53ea88e-8837-4722-acf9-761f0d0c7a48",
    "name": "Mẫu giao hàng Co.opmart",
    "partnerName": "Saigon Co.op",
    "description": "Biểu mẫu tinh gọn 10 trường giao hàng siêu thị Co.opmart",
    "isDefault": false,
    "totalFields": 10,
    "selectedFields": [
      {
        "fieldKey": "organization.name",
        "fieldGroup": "ORGANIZATION",
        "displayName": "Tên tổ chức / HTX",
        "mandatory": true,
        "fieldOrder": 1
      },
      {
        "fieldKey": "farmArea.name",
        "fieldGroup": "FARM_AREA",
        "displayName": "Tên vùng trồng",
        "mandatory": true,
        "fieldOrder": 2
      },
      {
        "fieldKey": "productionLot.name",
        "fieldGroup": "PRODUCTION_LOT",
        "displayName": "Tên / Mã lô sản xuất",
        "mandatory": true,
        "fieldOrder": 3
      },
      {
        "fieldKey": "productionLot.productCategory",
        "fieldGroup": "PRODUCTION_LOT",
        "displayName": "Danh mục sản phẩm",
        "mandatory": true,
        "fieldOrder": 4
      },
      {
        "fieldKey": "productionLot.harvestDate",
        "fieldGroup": "PRODUCTION_LOT",
        "displayName": "Ngày thu hoạch",
        "mandatory": false,
        "fieldOrder": 5
      },
      {
        "fieldKey": "shipment.name",
        "fieldGroup": "SHIPMENT",
        "displayName": "Tên lô hàng",
        "mandatory": true,
        "fieldOrder": 6
      },
      {
        "fieldKey": "shipment.totalQuantity",
        "fieldGroup": "SHIPMENT",
        "displayName": "Số lượng lô hàng",
        "mandatory": true,
        "fieldOrder": 7
      },
      {
        "fieldKey": "farmLog.activityType",
        "fieldGroup": "FARM_LOG",
        "displayName": "Loại hoạt động canh tác",
        "mandatory": true,
        "fieldOrder": 8
      },
      {
        "fieldKey": "inspection.passed",
        "fieldGroup": "INSPECTION",
        "displayName": "Kết quả Đạt / Không đạt",
        "mandatory": false,
        "fieldOrder": 9
      },
      {
        "fieldKey": "chainEvent.eventType",
        "fieldGroup": "TIMELINE",
        "displayName": "Loại sự kiện chuỗi",
        "mandatory": true,
        "fieldOrder": 10
      }
    ],
    "createdAt": "2026-09-14T15:05:00",
    "updatedAt": "2026-09-14T15:05:00"
  },
  "timestamp": "2026-09-14T15:05:00.000Z"
}
```

#### Response `422 Unprocessable Entity` – Thiếu trường bắt buộc QTN-11 (TC-02)

Nếu người dùng cố tình bỏ chọn bất kỳ trường bắt buộc nào theo QTN-11 (ví dụ: thiếu `farmArea.name` và `productionLot.name`):

```json
{
  "success": false,
  "status": 422,
  "message": "Không thể lưu mẫu hồ sơ: Thiếu các trường bắt buộc theo quy tắc QTN-11.",
  "errors": [
    "Thiếu trường bắt buộc: farmArea.name (Tên vùng trồng)",
    "Thiếu trường bắt buộc: productionLot.name (Tên / Mã lô sản xuất)"
  ],
  "path": "/api/v1/organizations/d53ea88e-8837-4722-acf9-761f0d0c7a48/profile-templates",
  "timestamp": "2026-09-14T15:05:00.000Z"
}
```

---

### 4.4 Xem chi tiết mẫu hồ sơ

- **URL:** `GET /api/v1/organizations/{orgId}/profile-templates/{templateId}`
- **Quyền:** `VT-02`

#### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "a1b2c3d4-5555-4a2a-9f3d-1a2b3c4d5e6f",
    "organizationId": "d53ea88e-8837-4722-acf9-761f0d0c7a48",
    "name": "Mẫu giao hàng Co.opmart",
    "partnerName": "Saigon Co.op",
    "description": "Biểu mẫu tinh gọn 10 trường giao hàng siêu thị Co.opmart",
    "isDefault": false,
    "totalFields": 10,
    "selectedFields": [
      {
        "fieldKey": "organization.name",
        "fieldGroup": "ORGANIZATION",
        "displayName": "Tên tổ chức / HTX",
        "mandatory": true,
        "fieldOrder": 1
      },
      {
        "fieldKey": "farmArea.name",
        "fieldGroup": "FARM_AREA",
        "displayName": "Tên vùng trồng",
        "mandatory": true,
        "fieldOrder": 2
      },
      {
        "fieldKey": "productionLot.name",
        "fieldGroup": "PRODUCTION_LOT",
        "displayName": "Tên / Mã lô sản xuất",
        "mandatory": true,
        "fieldOrder": 3
      },
      {
        "fieldKey": "productionLot.productCategory",
        "fieldGroup": "PRODUCTION_LOT",
        "displayName": "Danh mục sản phẩm",
        "mandatory": true,
        "fieldOrder": 4
      },
      {
        "fieldKey": "productionLot.harvestDate",
        "fieldGroup": "PRODUCTION_LOT",
        "displayName": "Ngày thu hoạch",
        "mandatory": false,
        "fieldOrder": 5
      },
      {
        "fieldKey": "shipment.name",
        "fieldGroup": "SHIPMENT",
        "displayName": "Tên lô hàng",
        "mandatory": true,
        "fieldOrder": 6
      },
      {
        "fieldKey": "shipment.totalQuantity",
        "fieldGroup": "SHIPMENT",
        "displayName": "Số lượng lô hàng",
        "mandatory": true,
        "fieldOrder": 7
      },
      {
        "fieldKey": "farmLog.activityType",
        "fieldGroup": "FARM_LOG",
        "displayName": "Loại hoạt động canh tác",
        "mandatory": true,
        "fieldOrder": 8
      },
      {
        "fieldKey": "inspection.passed",
        "fieldGroup": "INSPECTION",
        "displayName": "Kết quả Đạt / Không đạt",
        "mandatory": false,
        "fieldOrder": 9
      },
      {
        "fieldKey": "chainEvent.eventType",
        "fieldGroup": "TIMELINE",
        "displayName": "Loại sự kiện chuỗi",
        "mandatory": true,
        "fieldOrder": 10
      }
    ],
    "createdAt": "2026-09-14T15:05:00",
    "updatedAt": "2026-09-14T15:05:00"
  },
  "timestamp": "2026-09-14T15:05:00.000Z"
}
```

---

### 4.5 Cập nhật mẫu hồ sơ (TC-02)

- **URL:** `PUT /api/v1/organizations/{orgId}/profile-templates/{templateId}`
- **Quyền:** `VT-02`
- **Request Body:**

```json
{
  "name": "Mẫu giao hàng Co.opmart (Cập nhật)",
  "partnerName": "Saigon Co.op - Miền Bắc",
  "description": "Bổ sung thêm trường địa chỉ trụ sở",
  "isDefault": true,
  "selectedFields": [
    { "fieldKey": "organization.name", "fieldGroup": "ORGANIZATION", "fieldOrder": 1 },
    { "fieldKey": "organization.address", "fieldGroup": "ORGANIZATION", "fieldOrder": 2 },
    { "fieldKey": "farmArea.name", "fieldGroup": "FARM_AREA", "fieldOrder": 3 },
    { "fieldKey": "productionLot.name", "fieldGroup": "PRODUCTION_LOT", "fieldOrder": 4 },
    { "fieldKey": "productionLot.productCategory", "fieldGroup": "PRODUCTION_LOT", "fieldOrder": 5 },
    { "fieldKey": "productionLot.harvestDate", "fieldGroup": "PRODUCTION_LOT", "fieldOrder": 6 },
    { "fieldKey": "shipment.name", "fieldGroup": "SHIPMENT", "fieldOrder": 7 },
    { "fieldKey": "shipment.totalQuantity", "fieldGroup": "SHIPMENT", "fieldOrder": 8 },
    { "fieldKey": "farmLog.activityType", "fieldGroup": "FARM_LOG", "fieldOrder": 9 },
    { "fieldKey": "inspection.passed", "fieldGroup": "INSPECTION", "fieldOrder": 10 },
    { "fieldKey": "chainEvent.eventType", "fieldGroup": "TIMELINE", "fieldOrder": 11 }
  ]
}
```

#### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "message": "Cập nhật mẫu hồ sơ thành công.",
  "data": {
    "id": "a1b2c3d4-5555-4a2a-9f3d-1a2b3c4d5e6f",
    "organizationId": "d53ea88e-8837-4722-acf9-761f0d0c7a48",
    "name": "Mẫu giao hàng Co.opmart (Cập nhật)",
    "partnerName": "Saigon Co.op - Miền Bắc",
    "description": "Bổ sung thêm trường mã số thuế",
    "isDefault": true,
    "totalFields": 11,
    "updatedAt": "2026-09-14T15:10:00"
  },
  "timestamp": "2026-09-14T15:10:00.000Z"
}
```

---

### 4.6 Xóa mẫu hồ sơ

- **URL:** `DELETE /api/v1/organizations/{orgId}/profile-templates/{templateId}`
- **Quyền:** `VT-02`

#### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "message": "Xóa mẫu hồ sơ thành công.",
  "timestamp": "2026-09-14T15:12:00.000Z"
}
```

---

### 4.7 Xem trước hồ sơ theo mẫu (Dossier Preview)

Cho phép người dùng xem trước hồ sơ truy xuất của lô hàng theo một mẫu hồ sơ đã chọn (dưới dạng JSON, không tải file). Dữ liệu trả về **chỉ bao gồm các trường được bật** trong mẫu đó (TC-01).

- **URL:** `GET /api/v1/shipments/{shipmentId}/dossier/preview`
- **Query Parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|:---:|---|---|
| `templateId` | UUID | Không | null | ID của mẫu hồ sơ cần áp dụng. Nếu không truyền, dùng mẫu mặc định của tổ chức (TC-03) |

#### Response `200 OK` (Mẫu 10 trường → trả về đúng 10 trường theo TC-01)

```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
    "appliedTemplate": {
      "templateId": "a1b2c3d4-5555-4a2a-9f3d-1a2b3c4d5e6f",
      "templateName": "Mẫu giao hàng Co.opmart",
      "isDefault": false
    },
    "organization": {
      "name": "HTX Nông nghiệp Tân Cương"
    },
    "farmArea": {
      "name": "Vùng chè Đồi 1 Tân Cương"
    },
    "productionLot": {
      "name": "Lô chè Tân Cương T8/2026",
      "productCategory": "Chè xanh búp nõn",
      "harvestDate": "2026-08-11"
    },
    "shipment": {
      "name": "Lô chè giao WinMart Miền Bắc",
      "totalQuantity": 500
    },
    "farmLogs": [
      {
        "activityType": "PLANTING"
      },
      {
        "activityType": "FERTILIZING"
      },
      {
        "activityType": "PESTICIDE"
      },
      {
        "activityType": "HARVESTING"
      }
    ],
    "inspections": [
      {
        "passed": true
      }
    ],
    "timelineEvents": [
      {
        "eventType": "HARVEST"
      },
      {
        "eventType": "PACKAGING"
      },
      {
        "eventType": "TRANSPORT"
      },
      {
        "eventType": "PROCUREMENT"
      }
    ]
  },
  "timestamp": "2026-09-14T15:15:00.000Z"
}
```

---

### 4.8 Cập nhật Endpoint Xuất hồ sơ PDF (TC-01, TC-03)

Mở rộng endpoint xuất hồ sơ hiện có `GET /api/v1/shipments/{shipmentId}/dossier/export` với tham số `templateId` tùy chọn.

- **URL:** `GET /api/v1/shipments/{shipmentId}/dossier/export`
- **Query Parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|:---:|---|---|
| `templateId` | UUID | Không | null | ID của mẫu hồ sơ áp dụng. Nếu không chọn, dùng mẫu mặc định của tổ chức (TC-03) |

#### Nguyên tắc hoạt động:
1. Nếu `templateId` được cung cấp:
   - Hệ thống tải template tương ứng, kiểm tra xem template có thuộc về tổ chức sở hữu lô hàng hay không (`QTN-01`).
   - Tạo file PDF chỉ render các bảng và trường dữ liệu đã chọn trong template (TC-01).
   - Ghi lại bản ghi vào `dossier_export_history` với `template_id` và `template_name`.
2. Nếu `templateId` không được cung cấp (`null`):
   - Hệ thống tự động tìm template có cờ `is_default = true` của tổ chức lô hàng.
   - Nếu tìm thấy: áp dụng template này, ghi nhận `template_id` và `template_name` vào `dossier_export_history` (TC-03).
   - Nếu không tìm thấy mẫu mặc định nào: áp dụng cấu hình mặc định đầy đủ (toàn bộ các trường) của hệ thống; ghi nhận `template_name = "Mặc định hệ thống"`.

#### Response `200 OK` (Tải tệp PDF)

- **Content-Type:** `application/pdf`
- **Content-Disposition:** `attachment; filename="Ho_so_truy_xuat_<shipment_name>_<yyyyMMdd>.pdf"`
- **Body:** Binary stream tệp PDF được lọc đúng các trường của template.

---

## 5. Xử lý lỗi (Error Handling)

### 5.1 Bỏ trường bắt buộc theo QTN-11 (TC-02)

- **HTTP Status:** `422 Unprocessable Entity`

```json
{
  "success": false,
  "status": 422,
  "message": "Không thể lưu mẫu hồ sơ: Thiếu các trường bắt buộc theo quy tắc QTN-11.",
  "errors": [
    "Thiếu trường bắt buộc: farmArea.name (Tên vùng trồng)",
    "Thiếu trường bắt buộc: productionLot.name (Tên / Mã lô sản xuất)"
  ],
  "path": "/api/v1/organizations/d53ea88e-8837-4722-acf9-761f0d0c7a48/profile-templates",
  "timestamp": "2026-09-14T15:20:00.000Z"
}
```

### 5.2 Mẫu hồ sơ không tồn tại

- **HTTP Status:** `404 Not Found`

```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy thông tin mẫu hồ sơ.",
  "path": "/api/v1/organizations/d53ea88e-8837-4722-acf9-761f0d0c7a48/profile-templates/999e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-09-14T15:21:00.000Z"
}
```

### 5.3 Truy cập mẫu hồ sơ của tổ chức khác (TC-04, QTN-01)

- **HTTP Status:** `403 Forbidden`

```json
{
  "success": false,
  "status": 403,
  "message": "Từ chối thao tác: Bạn không có quyền truy cập dữ liệu của tổ chức khác.",
  "path": "/api/v1/organizations/7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f/profile-templates",
  "timestamp": "2026-09-14T15:22:00.000Z"
}
```

### 5.4 Không có vai trò Quản lý HTX (VT-02)

- **HTTP Status:** `403 Forbidden`

```json
{
  "success": false,
  "status": 403,
  "message": "Từ chối thao tác: Chức năng cấu hình mẫu hồ sơ chỉ dành cho Quản lý hợp tác xã (VT-02).",
  "path": "/api/v1/organizations/d53ea88e-8837-4722-acf9-761f0d0c7a48/profile-templates",
  "timestamp": "2026-09-14T15:23:00.000Z"
}
```

---

## 6. Thiết kế Cơ sở dữ liệu (Database Schema)

### 6.1 Bảng `profile_templates` (Mẫu cấu hình hồ sơ truy xuất)

| Tên cột | Kiểu dữ liệu | Nullable | Khóa | Mô tả |
|---|---|:---:|:---:|---|
| `id` | `CHAR(36)` | NO | PK | Khóa chính (UUID tự sinh) |
| `organization_id` | `CHAR(36)` | NO | FK | ID tổ chức sở hữu mẫu (FK -> `organizations.organization_id`) |
| `name` | `VARCHAR(150)` | NO | – | Tên mẫu hồ sơ (ví dụ: Mẫu xuất khẩu Nhật Bản) |
| `partner_name` | `VARCHAR(150)` | YES | – | Tên đối tác / siêu thị áp dụng |
| `description` | `VARCHAR(500)` | YES | – | Mô tả mục đích sử dụng của mẫu |
| `is_default` | `BOOLEAN` | NO | – | Có phải mẫu mặc định của tổ chức không (mặc định: `false`) |
| `created_by` | `CHAR(36)` | YES | FK | Người tạo mẫu (FK -> `users.user_id`) |
| `created_at` | `DATETIME` | NO | – | Thời điểm tạo mẫu |
| `updated_at` | `DATETIME` | NO | – | Thời điểm cập nhật mẫu |

### 6.2 Bảng `profile_template_fields` (Các trường được chọn trong mẫu)

| Tên cột | Kiểu dữ liệu | Nullable | Khóa | Mô tả |
|---|---|:---:|:---:|---|
| `id` | `CHAR(36)` | NO | PK | Khóa chính (UUID tự sinh) |
| `template_id` | `CHAR(36)` | NO | FK | ID mẫu hồ sơ (FK -> `profile_templates.id` ON DELETE CASCADE) |
| `field_group` | `VARCHAR(50)` | NO | – | Nhóm trường (ORGANIZATION, FARM_AREA, PRODUCTION_LOT...) |
| `field_key` | `VARCHAR(100)` | NO | – | Khóa định danh trường (vd: `farmArea.name`) |
| `display_name` | `VARCHAR(150)` | NO | – | Tên hiển thị tiếng Việt của trường |
| `is_mandatory` | `BOOLEAN` | NO | – | Cờ bắt buộc theo QTN-11 (mặc định: `false`) |
| `field_order` | `INT` | NO | – | Thứ tự hiển thị trường |

### 6.3 Mở rộng bảng `dossier_export_history` (Ghi nhận mẫu đã dùng)

| Tên cột bổ sung | Kiểu dữ liệu | Nullable | Khóa | Mô tả |
|---|---|:---:|:---:|---|
| `template_id` | `CHAR(36)` | YES | FK | ID mẫu hồ sơ đã áp dụng (FK -> `profile_templates.id`) |
| `template_name` | `VARCHAR(150)` | YES | – | Tên mẫu hồ sơ lưu trữ tại thời điểm xuất |

#### Migration SQL dự kiến (`V20260914110000__create_profile_templates_tables.sql`)

```sql
-- 1. Bảng profile_templates
CREATE TABLE profile_templates (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    partner_name VARCHAR(150) NULL,
    description VARCHAR(500) NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_by CHAR(36) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_profile_template_org FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    CONSTRAINT fk_profile_template_creator FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE INDEX idx_profile_template_org ON profile_templates(organization_id);

-- 2. Bảng profile_template_fields
CREATE TABLE profile_template_fields (
    id CHAR(36) NOT NULL,
    template_id CHAR(36) NOT NULL,
    field_group VARCHAR(50) NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    field_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_template_field_template FOREIGN KEY (template_id) REFERENCES profile_templates(id) ON DELETE CASCADE
);

CREATE INDEX idx_template_field_template ON profile_template_fields(template_id);
CREATE UNIQUE INDEX uq_template_field_key ON profile_template_fields(template_id, field_key);

-- 3. Mở rộng bảng dossier_export_history
ALTER TABLE dossier_export_history
    ADD COLUMN template_id CHAR(36) NULL,
    ADD COLUMN template_name VARCHAR(150) NULL;

ALTER TABLE dossier_export_history
    ADD CONSTRAINT fk_dossier_export_template FOREIGN KEY (template_id) REFERENCES profile_templates(id) ON DELETE SET NULL;
```

---

## 7. Tiêu chí nghiệm thu (Acceptance Criteria Mapping)

| Mã AC | Mức độ | Tiêu chí chấp nhận | Giải pháp hiện thực hóa & Kiểm thử |
|:---:|:---:|:---|:---|
| **TC-01** | Cao | Mẫu cấu hình 10 trường → khi xuất hồ sơ ra đúng 10 trường. | - API preview và xuất hồ sơ nhận diện `templateId`.<br>- Thuật toán lọc trường loại bỏ các trường không thuộc `selectedFields`.<br>- Tệp xuất ra phản ánh chính xác 10 trường đã chọn. |
| **TC-02** | Cao | Bỏ trường bắt buộc theo QTN-11 → hệ thống chặn và báo lỗi. | - Backend validate `selectedFields` phải chứa đầy đủ 8 trường QTN-11.<br>- Nếu thiếu bất kỳ trường nào, trả về `422 Unprocessable Entity` kèm danh sách cụ thể.<br>- Frontend khóa checkbox không cho bỏ tick các trường này. |
| **TC-03** | Trung bình | Không chọn mẫu → dùng mẫu mặc định + ghi log. | - Nếu `templateId` là `null`, tìm mẫu có `isDefault = true` của tổ chức.<br>- Nếu không có mẫu `isDefault`, dùng bộ trường chuẩn đầy đủ.<br>- Luôn ghi nhận `template_id` / `template_name` vào `dossier_export_history`. |
| **TC-04** | Cao | Mẫu của tổ chức khác → không hiển thị trong danh sách. | - API `GET` danh sách luôn gắn điều kiện `WHERE organization_id = currentUser.organizationId`.<br>- API xem/sửa/xóa kiểm tra quyền sở hữu, trả về `403 Forbidden` nếu vi phạm ranh giới tổ chức (`QTN-01`). |

---

## 8. Hướng dẫn tích hợp Frontend

1. **Thư mục API:** `frontend/src/api/profileTemplateApi.ts`
   - `getProfileTemplates(orgId)`
   - `getProfileTemplateDetail(orgId, templateId)`
   - `createProfileTemplate(orgId, data)`
   - `updateProfileTemplate(orgId, templateId, data)`
   - `deleteProfileTemplate(orgId, templateId)`
   - `getDossierFieldCatalog(orgId)`
   - `previewDossier(shipmentId, templateId)`
2. **Types:** `frontend/src/types/profileTemplate.ts`
3. **Phân quyền giao diện (`roleAccess.ts`):**
   - `profileTemplateManage: ['VT-02'] as const`
4. **Trang chức năng:**
   - `/cooperative/profile-templates`: Trang danh sách và quản lý các mẫu hồ sơ của HTX.
   - Nút "Xuất hồ sơ theo mẫu" kèm dropdown chọn mẫu và nút "Xem trước" trong danh sách lô hàng (`ShipmentList.tsx`) và trang chi tiết lô hàng (`ShipmentDetailPage.tsx`).
