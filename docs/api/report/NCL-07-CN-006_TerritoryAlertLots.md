# API Docs – NCL-07-CN-006: Danh sách lô có cảnh báo theo địa bàn cho cán bộ quản lý ngành

**Mã User Story:** `NCL-07-CN-006`  
**Tên User Story:** Danh sách lô có cảnh báo theo địa bàn cho cán bộ quản lý ngành  
**Epic:** `NCL-07` – Báo cáo và xuất hồ sơ truy xuất  
**Nhánh Git:** `feature/NCL-07-CN-006-alert-lots-by-territory`  
**Vai trò chính (Actor):** Cán bộ quản lý ngành (`VT-05` / `REGULATOR`)  
**Quy tắc nghiệp vụ tham chiếu:** `QTN-01`, `QTN-09`, `QTN-10`, `QTN-13`, `QTN-18`, `QTN-21`, `QTN-30`

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Tác giả |
| :--- | :---: | :--- | :--- |
| 2026-09-11 | `v1.0.0` | Khởi tạo tài liệu đặc tả API hợp đồng (API Contract) cho luồng Danh sách, Chi tiết (Read-only) và Xuất báo cáo danh sách lô có cảnh báo theo địa bàn quản lý của Cán bộ quản lý ngành (`VT-05`). Xử lý mâu thuẫn 5 vs 6 loại cảnh báo và đặc tả cơ chế kiểm soát địa bàn theo `AreaScopeService`. | Senior Software Architect + BA + API Designer |

---

## 1. Thông tin chung & Mục tiêu nghiệp vụ

### 1.1 Mục tiêu
Cho phép **Cán bộ quản lý ngành (`VT-05`)** tra cứu danh sách các lô sản xuất (`ProductionLot`) đang có cảnh báo bất thường hoặc vi phạm quy chuẩn an toàn/nghiệp vụ phát sinh trên địa bàn hành chính được phân công phụ trách. Tính năng này giúp cán bộ nhanh chóng nắm bắt các "điểm nóng" rủi ro cần kiểm tra thực địa, thay vì chỉ theo dõi số liệu tổng hợp sản lượng chung.

### 1.2 Tiêu chí nghiệm thu (Acceptance Criteria)
* **TC-01 – Luồng thành công:** Địa bàn được phân công của cán bộ có 2 lô đang thu hồi và 1 lô có tem bị khóa. Khi mở danh sách, hệ thống hiển thị đủ 3 lô, thể hiện rõ loại cảnh báo và thông tin tổ chức sở hữu.
* **TC-02 – Dữ liệu rỗng khi chưa được gán địa bàn:** Cán bộ chưa được phân công địa bàn nào trong hệ thống (`user_area_assignments` rỗng). API trả về `HTTP 200 OK` với danh sách dữ liệu rỗng (`items: []`, `totalElements: 0`) kèm thông điệp rõ ràng: `"Bạn chưa được phân công địa bàn quản lý nào."`. Tuyệt đối không trả về `HTTP 403` và không fallback sang hiển thị toàn bộ dữ liệu hệ thống.
* **TC-03 – Không có quyền sửa (Read-only Backend Enforcement):** Khi cán bộ mở xem chi tiết một lô cảnh báo, cán bộ chỉ được xem thông tin công khai của lô, loại cảnh báo, tổ chức sở hữu, bằng chứng cảnh báo và dòng sự kiện liên quan. Tầng backend bảo đảm quyền chỉ đọc (Read-only), từ chối mọi thao tác chỉnh sửa hoặc gọi các API mutation.
* **TC-04 – Bộ lọc loại cảnh báo:** Khi danh sách có nhiều lô với các cảnh báo khác nhau, người dùng lọc theo `inspection_failed` (hoặc enum `INSPECTION_FAILED`), API chỉ trả về các lô có cảnh báo "Kết quả kiểm nghiệm không đạt".

---

## 2. Phân tích nghiệp vụ chuyên sâu & Quyết định thiết kế

### 2.1 Mâu thuẫn Backlog (Discrepancy 5 vs 6 loại cảnh báo)

#### Vấn đề
* **Mô tả User Story** chỉ liệt kê **5 loại cảnh báo**:
  1. Lô đang thu hồi.
  2. Lô có tem bị khóa.
  3. Lô có kết quả kiểm nghiệm không đạt.
  4. Lô bị ghi đè thời gian cách ly.
  5. Lô có phản ánh mức nghiêm trọng chưa đóng.
* **Task `NCL-07-CN-006-CV-01`** ghi: *"Liệt kê đủ sáu loại cảnh báo."*

#### Bằng chứng khảo sát từ toàn bộ codebase và tài liệu
1. **Lô đang thu hồi (`RECALLING`):** Đã hiện hữu qua các module `trace`, `recall`, `farm` (NCL-08-CN-003, NCL-08-CN-008, NCL-08-CN-011, QTN-09, `ProductionLotStatus.RECALLED`, `ShipmentStatus.RECALLED`).
2. **Lô có tem bị khóa (`LOCKED_LABEL`):** Đã hiện hữu qua module `trace` (NCL-08-CN-007, QTN-10, `TraceCodeStatus.LOCKED`).
3. **Lô có kết quả kiểm nghiệm không đạt (`INSPECTION_FAILED`):** Đã hiện hữu qua module `certification` (NCL-11-CN-005, QTN-30, `InspectionBlockReasonCode.INSPECTION_FAILED`, `InspectionRequestStatus.FAILED`).
4. **Lô bị ghi đè thời gian cách ly (`QUARANTINE_OVERWRITTEN`):** Đã hiện hữu qua module `farm` (NCL-03-CN-005, NCL-681, cờ `earlyHarvest: true` kèm `earlyHarvestReason` khi thu hoạch trước PHI).
5. **Lô có phản ánh mức nghiêm trọng chưa đóng (`SERIOUS_FEEDBACK_OPEN`):** Đã hiện hữu qua module `farm` (NCL-08-CN-009, `ProductFeedback` có `severity` thuộc `QUALITY_SUSPECTED` hoặc `COUNTERFEIT_SUSPECTED`, trạng thái `status != CLOSED`).
6. **Xác minh loại cảnh báo thứ 6:**
   * Trong module `certification` và `alert`, hệ thống đã cài đặt chính thức enum `AlertType.INSPECTION_EXPIRED` (NCL-11-CN-004) và `InspectionBlockReasonCode.INSPECTION_EXPIRED`. Khi kết quả kiểm nghiệm hết hiệu lực, lô bị chặn kích hoạt tem và chặn xuất lô hàng theo `QTN-13` và `QTN-21`. Hệ thống tự động quét và sinh bản ghi `Alert` gắn trực tiếp với lô sản xuất (`relatedEntityId = lotId`).
   * Ngoài ra, module `alert` còn có `AlertType.SCAN_ANOMALY` (NCL-08-CN-001, QTN-10: tem quét bất thường nhiều nơi, ở trạng thái `SUSPECT` trước khi bị khóa `LOCKED`).

#### Quyết định hợp đồng API
* Định nghĩa Enum `LotAlertType` hỗ trợ đầy đủ **5 loại cảnh báo cốt lõi**:
  - `RECALLING`
  - `LOCKED_LABEL`
  - `INSPECTION_FAILED`
  - `QUARANTINE_OVERWRITTEN`
  - `SERIOUS_FEEDBACK_OPEN`
* Chuẩn hóa **`INSPECTION_EXPIRED`** (Kết quả kiểm nghiệm hết hiệu lực) là **ứng viên loại cảnh báo thứ 6** có căn cứ kỹ thuật đầy đủ nhất trong hệ thống.
* API Contract thiết kế dạng mở rộng (extensible): Enum chấp nhận cả 6 giá trị. Nếu hệ thống kích hoạt cả 6 loại, backend và frontend hoạt động liền mạch mà không cần đổi contract.

---

### 2.2 Quy tắc phân quyền & Kiểm soát địa bàn (Territory Scoping & Anti-Bypass)

Tuân thủ nghiêm ngặt **Quy tắc bảo mật địa bàn số 1** (theo `NCL-670`, `NCL-739`, `QTN-18`) thông qua dịch vụ `AreaScopeService`:

```text
Actor VT-05 (Cán bộ quản lý ngành)
      ↓ Gọi API GET /api/v1/reports/alert-lots
AreaScopeService.resolveOrganizationsForReports(currentUser, unitIds)
      ├── Trường hợp 1: Chưa gán địa bàn (assignments.isEmpty())
      │        → AreaScopeResult.emptyScope()
      │        → HTTP 200 OK, items = [], totalElements = 0
      │        → Message: "Bạn chưa được phân công địa bàn quản lý nào."
      │        → (TUYỆT ĐỐI KHÔNG 403, KHÔNG fallback toàn hệ thống)
      │
      └── Trường hợp 2: Đã được gán địa bàn (assignedUnitIds)
               → effectiveUnits = assignedUnitIds ∩ unitIds (nếu có)
               → Truy vấn organizations có province_id hoặc commune_id thuộc effectiveUnits
               → Lọc các ProductionLot thuộc danh sách organizationIds hợp lệ
```

#### Quy tắc chống gian lận tham số (Anti-Bypass Rules)
1. **Không cho phép bypass qua `unitIds`:** Nếu client truyền `unitIds` nằm ngoài danh sách địa bàn đã gán cho cán bộ, backend áp dụng phép giao tập hợp (`assignedUnits.retainAll(paramUnits)`). Cán bộ không bao giờ thấy được dữ liệu ngoài địa bàn phụ trách.
2. **Không cho phép bypass qua `organizationId`:** Nếu client truyền `organizationId` của một tổ chức không thuộc địa bàn được phân công, backend trả về danh sách rỗng (hoặc từ chối hợp lệ), không để lọt dữ liệu.
3. **Phân quyền chi tiết (Detail API):** Khi gọi `GET /api/v1/reports/alert-lots/{lotId}`, backend bắt buộc kiểm tra tổ chức sở hữu lô có thuộc địa bàn của cán bộ hay không. Nếu không thuộc, ném `BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thông tin lô ngoài địa bàn phụ trách.")`.

---

### 2.3 Quy ước bộ lọc khoảng thời gian (`fromDate`, `toDate`)

* **Quyết định nghiệp vụ:** `fromDate` và `toDate` áp dụng cho **thời điểm phát sinh cảnh báo** (`alertTriggeredAt` / thời điểm ghi nhận sự kiện cảnh báo của lô).
* **Căn cứ nghiệp vụ:** Cán bộ quản lý ngành sử dụng màn hình này để giám sát điểm nóng rủi ro trong kỳ thanh tra/báo cáo. Một lô có thể gieo trồng hoặc thu hoạch từ tháng trước, nhưng sự cố thu hồi (`RECALLING`) hoặc phản ánh nghiêm trọng (`SERIOUS_FEEDBACK_OPEN`) lại phát sinh trong tuần này. Việc lọc theo thời điểm phát sinh cảnh báo phản ánh chính xác các biến động thực tế cần can thiệp xử lý.
* Định dạng: ISO 8601 Date (`YYYY-MM-DD`). Validate: `fromDate <= toDate`.

---

## 3. Danh mục API Endpoints

| STT | Tên chức năng | Method | Endpoint Path | Quyền truy cập | Mô tả |
|:---:|:---|:---:|:---|:---:|:---|
| 1 | Danh sách lô có cảnh báo theo địa bàn | `GET` | `/api/v1/reports/alert-lots` | `VT-05` | Lấy danh sách phân trang các lô có cảnh báo trong địa bàn phụ trách |
| 2 | Chi tiết lô cảnh báo (Read-only) | `GET` | `/api/v1/reports/alert-lots/{lotId}` | `VT-05` | Xem chi tiết lô, thông tin tổ chức, bằng chứng cảnh báo và dòng sự kiện |
| 3 | Xuất danh sách lô có cảnh báo | `GET` | `/api/v1/reports/alert-lots/export` | `VT-05` | Xuất toàn bộ danh sách thỏa mãn bộ lọc ra file Excel hoặc PDF |

---

## 4. Đặc tả chi tiết Endpoint 1: Danh sách lô có cảnh báo (List API)

### 4.1 Thông tin Endpoint
* **URL:** `/api/v1/reports/alert-lots`
* **Method:** `GET`
* **Authentication:** Yêu cầu JWT Bearer Token trong Header: `Authorization: Bearer <token>`
* **Quyền truy cập (RBAC):** Cán bộ quản lý ngành (`VT-05` / `REGULATOR`). Quản trị viên (`VT-01`) có thể truy cập toàn hệ thống hoặc theo bộ lọc.

### 4.2 Query Parameters

| Tên tham số | Kiểu dữ liệu | Bắt buộc | Mặc định | Ràng buộc / Mô tả | Ví dụ |
|:---|:---:|:---:|:---:|:---|:---|
| `alertType` | `String` | Không | `null` | Lọc theo loại cảnh báo. Chấp nhận hoa/thường: `RECALLING`, `LOCKED_LABEL`, `INSPECTION_FAILED`, `QUARANTINE_OVERWRITTEN`, `SERIOUS_FEEDBACK_OPEN`, `INSPECTION_EXPIRED`. Hỗ trợ cả định dạng snake_case như `inspection_failed` (TC-04). | `INSPECTION_FAILED` |
| `organizationId` | `UUID` | Không | `null` | Lọc theo tổ chức sở hữu lô. Backend tự đối soát với địa bàn được phân công. | `8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c` |
| `unitIds` | `List<UUID>` | Không | `null` | Lọc theo đơn vị hành chính (tỉnh hoặc xã). Có thể lặp: `?unitIds=uuid1&unitIds=uuid2`. | `e2a3b4c5-5555-4a2a-9f3d-1a2b3c4d5e6f` |
| `fromDate` | `LocalDate` | Không | `null` | Ngày bắt đầu khoảng thời gian phát sinh cảnh báo (`YYYY-MM-DD`). | `2026-09-01` |
| `toDate` | `LocalDate` | Không | `null` | Ngày kết thúc khoảng thời gian phát sinh cảnh báo (`YYYY-MM-DD`). Ràng buộc `fromDate <= toDate`. | `2026-09-11` |
| `keyword` | `String` | Không | `null` | Tìm kiếm không phân biệt hoa thường theo mã lô (`lotCode`) hoặc tên lô (`lotName`). | `Bưởi da xanh` |
| `page` | `Integer` | Không | `0` | Chỉ số trang (0-indexed). Giá trị $\ge 0$. | `0` |
| `size` | `Integer` | Không | `10` | Số lượng phần tử trên mỗi trang. $1 \le \text{size} \le 100$. | `10` |
| `sortBy` | `String` | Không | `alertTriggeredAt` | Trường sắp xếp: `alertTriggeredAt`, `createdAt`, `lotName`, `organizationName`. | `alertTriggeredAt` |
| `sortDirection` | `String` | Không | `DESC` | Hướng sắp xếp: `ASC` hoặc `DESC`. | `DESC` |

---

### 4.3 Response Models (Schemas)

#### `ApiResult<PageResponse<AlertLotSummaryResponse>>`

```typescript
interface AlertLotSummaryResponse {
  lotId: string;                        // UUID định danh Lô sản xuất
  lotCode: string;                      // Mã lô sản xuất (VD: "LOT-202609-001")
  lotName: string;                      // Tên lô sản xuất
  organizationId: string;               // UUID tổ chức sở hữu
  organizationName: string;             // Tên tổ chức / HTX sở hữu lô
  productCategoryId: string;            // UUID loại nông sản
  productCategoryName: string;          // Tên loại nông sản
  farmAreaName: string;                 // Tên vùng trồng canh tác
  communeName: string | null;           // Tên Xã / Phường của tổ chức
  provinceName: string | null;          // Tên Tỉnh / Thành phố của tổ chức
  lotStatus: string;                    // Trạng thái lô (APPROVED, PACKAGED, RECALLED, DISPOSED...)
  alertTypes: LotAlertType[];           // Danh sách các loại cảnh báo đang tồn tại trên lô
  primaryAlertType: LotAlertType;       // Loại cảnh báo có mức độ ưu tiên cao nhất
  alertCount: number;                   // Tổng số lượng cảnh báo phát sinh trên lô
  latestAlertTriggeredAt: string;       // ISO-8601 Datetime thời điểm cảnh báo gần nhất phát sinh
  alertSummaries: AlertBadgeSummary[];  // Tóm tắt ngắn gọn các cảnh báo hiển thị dạng badge
  createdAt: string;                    // ISO-8601 Datetime thời điểm tạo lô
}

interface AlertBadgeSummary {
  alertType: LotAlertType;              // Mã enum cảnh báo
  alertName: string;                    // Tên tiếng Việt hiển thị trên giao diện
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  triggeredAt: string;                  // ISO-8601 Datetime
  briefNote: string;                    // Diễn giải ngắn gọn (VD: "Có 2 tem bị khóa", "Thu hoạch sớm 7 ngày")
}

type LotAlertType =
  | "RECALLING"
  | "LOCKED_LABEL"
  | "INSPECTION_FAILED"
  | "QUARANTINE_OVERWRITTEN"
  | "SERIOUS_FEEDBACK_OPEN"
  | "INSPECTION_EXPIRED";
```

---

### 4.4 Ví dụ Responses

#### A. Response `200 OK` – Thành công có dữ liệu (TC-01)
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy danh sách lô có cảnh báo thành công.",
  "data": {
    "items": [
      {
        "lotId": "5b050d90-be97-467b-a4cb-dcbf3db81dfd",
        "lotCode": "LOT-202609-001",
        "lotName": "Lô xoài Cát Chu xuất khẩu đợt 1",
        "organizationId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
        "organizationName": "Hợp tác xã Nông nghiệp Mỹ Xương",
        "productCategoryId": "c1a2b3c4-1111-4a2a-9f3d-1a2b3c4d5e6f",
        "productCategoryName": "Xoài Cát Chu",
        "farmAreaName": "Vùng trồng Xoài Xuất khẩu Tổ 1",
        "communeName": "Xã Mỹ Xương",
        "provinceName": "Tỉnh Đồng Tháp",
        "lotStatus": "RECALLED",
        "alertTypes": ["RECALLING"],
        "primaryAlertType": "RECALLING",
        "alertCount": 1,
        "latestAlertTriggeredAt": "2026-09-10T14:30:00Z",
        "alertSummaries": [
          {
            "alertType": "RECALLING",
            "alertName": "Lô đang thu hồi",
            "severity": "CRITICAL",
            "triggeredAt": "2026-09-10T14:30:00Z",
            "briefNote": "Lệnh thu hồi khẩn cấp: Phát hiện tồn dư thuốc BVTV vượt ngưỡng"
          }
        ],
        "createdAt": "2026-08-15T08:00:00Z"
      },
      {
        "lotId": "6c161e01-cf08-478c-b5dc-edca4ec92e0e",
        "lotCode": "LOT-202609-002",
        "lotName": "Lô xoài Cát Chu loại 2",
        "organizationId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
        "organizationName": "Hợp tác xã Nông nghiệp Mỹ Xương",
        "productCategoryId": "c1a2b3c4-1111-4a2a-9f3d-1a2b3c4d5e6f",
        "productCategoryName": "Xoài Cát Chu",
        "farmAreaName": "Vùng trồng Xoài Đồi Cao",
        "communeName": "Xã Mỹ Xương",
        "provinceName": "Tỉnh Đồng Tháp",
        "lotStatus": "RECALLED",
        "alertTypes": ["RECALLING"],
        "primaryAlertType": "RECALLING",
        "alertCount": 1,
        "latestAlertTriggeredAt": "2026-09-09T09:15:00Z",
        "alertSummaries": [
          {
            "alertType": "RECALLING",
            "alertName": "Lô đang thu hồi",
            "severity": "CRITICAL",
            "triggeredAt": "2026-09-09T09:15:00Z",
            "briefNote": "Thu hồi theo chuỗi liên đới với Lô LOT-202609-001"
          }
        ],
        "createdAt": "2026-08-16T09:30:00Z"
      },
      {
        "lotId": "7d272f12-da19-489d-c6ed-feb55fd03f1f",
        "lotCode": "LOT-202609-003",
        "lotName": "Lô xoài Cát Chu vụ Hè Thu",
        "organizationId": "9a4b2c3d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
        "organizationName": "Hợp tác xã Nông nghiệp Xanh Cao Lãnh",
        "productCategoryId": "c1a2b3c4-1111-4a2a-9f3d-1a2b3c4d5e6f",
        "productCategoryName": "Xoài Cát Chu",
        "farmAreaName": "Vườn Xoài Sinh Thái Mỹ Tân",
        "communeName": "Xã Mỹ Tân",
        "provinceName": "Tỉnh Đồng Tháp",
        "lotStatus": "PACKAGED",
        "alertTypes": ["LOCKED_LABEL"],
        "primaryAlertType": "LOCKED_LABEL",
        "alertCount": 1,
        "latestAlertTriggeredAt": "2026-09-08T16:45:00Z",
        "alertSummaries": [
          {
            "alertType": "LOCKED_LABEL",
            "alertName": "Tem bị khóa",
            "severity": "HIGH",
            "triggeredAt": "2026-09-08T16:45:00Z",
            "briefNote": "Khóa 1 mã tem quét bất thường tại 2 vị trí cách xa 500km"
          }
        ],
        "createdAt": "2026-08-20T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 3,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "errors": null,
  "path": "/api/v1/reports/alert-lots",
  "timestamp": "2026-09-11T02:00:00.000Z"
}
```

---

#### B. Response `200 OK` – Trường hợp Cán bộ chưa được phân công địa bàn (TC-02)
```json
{
  "success": true,
  "status": 200,
  "message": "Bạn chưa được phân công địa bàn quản lý nào.",
  "data": {
    "items": [],
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "errors": null,
  "path": "/api/v1/reports/alert-lots",
  "timestamp": "2026-09-11T02:00:00.000Z"
}
```

> **Ghi chú Frontend (TC-02):** Khi nhận được `data.items` rỗng kèm `message == "Bạn chưa được phân công địa bàn quản lý nào."`, Frontend hiển thị Empty State chuyên biệt: icon bản đồ/địa bàn kèm thông điệp hướng dẫn liên hệ Quản trị viên (`VT-01`) để được phân công địa bàn phụ trách.

---

#### C. Response `200 OK` – Khi filter `alertType=inspection_failed` (TC-04)
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy danh sách lô có cảnh báo thành công.",
  "data": {
    "items": [
      {
        "lotId": "8e383023-eb20-490e-d7fe-0fc660e14020",
        "lotCode": "LOT-202608-015",
        "lotName": "Lô Thanh long ruột đỏ VietGAP",
        "organizationId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
        "organizationName": "Hợp tác xã Nông nghiệp Mỹ Xương",
        "productCategoryId": "c2b3c4d5-2222-4a2a-9f3d-1a2b3c4d5e6f",
        "productCategoryName": "Thanh long ruột đỏ",
        "farmAreaName": "Vùng trồng Thanh long Chợ Gạo",
        "communeName": "Xã Mỹ Xương",
        "provinceName": "Tỉnh Đồng Tháp",
        "lotStatus": "HARVESTED",
        "alertTypes": ["INSPECTION_FAILED"],
        "primaryAlertType": "INSPECTION_FAILED",
        "alertCount": 1,
        "latestAlertTriggeredAt": "2026-09-05T11:20:00Z",
        "alertSummaries": [
          {
            "alertType": "INSPECTION_FAILED",
            "alertName": "Kết quả kiểm nghiệm không đạt",
            "severity": "HIGH",
            "triggeredAt": "2026-09-05T11:20:00Z",
            "briefNote": "Chỉ tiêu Vi sinh vật (E.coli) vượt ngưỡng cho phép: 120 CFU/g > 100 CFU/g"
          }
        ],
        "createdAt": "2026-08-10T07:15:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "errors": null,
  "path": "/api/v1/reports/alert-lots",
  "timestamp": "2026-09-11T02:00:00.000Z"
}
```

---

## 5. Đặc tả chi tiết Endpoint 2: Chi tiết lô cảnh báo (Detail API – Read-Only)

### 5.1 Thông tin Endpoint
* **URL:** `/api/v1/reports/alert-lots/{lotId}`
* **Method:** `GET`
* **Authentication:** Yêu cầu JWT Bearer Token trong Header: `Authorization: Bearer <token>`
* **Quyền truy cập (RBAC):** Cán bộ quản lý ngành (`VT-05`).
* **Ràng buộc an ninh (TC-03):** 
  - Chỉ trả về dữ liệu xem.
  - Tuyệt đối không cung cấp bất kỳ API mutation nào cho vai trò này trên giao diện hoặc controller.
  - Lô sản xuất phải thuộc tổ chức nằm trong địa bàn phụ trách của cán bộ.

### 5.2 Path Parameter
| Tên tham số | Kiểu dữ liệu | Bắt buộc | Mô tả | Ví dụ |
|:---|:---:|:---:|:---|:---|
| `lotId` | `UUID` | Có | ID định danh của Lô sản xuất cần xem chi tiết cảnh báo. | `5b050d90-be97-467b-a4cb-dcbf3db81dfd` |

---

### 5.3 Response Model

#### `ApiResult<AlertLotDetailResponse>`

```typescript
interface AlertLotDetailResponse {
  // 1. Khối thông tin định danh và hành chính của Lô
  lotInfo: {
    lotId: string;
    lotCode: string;
    lotName: string;
    status: string;                     // ProductionLotStatus
    expectedQuantity: number;
    actualQuantity: number | null;
    quantityUnit: string;
    plantingDate: string;               // YYYY-MM-DD
    harvestDate: string | null;         // YYYY-MM-DD
    productCategoryName: string;
    farmAreaName: string;
    farmAreaAddress: string;
    createdAt: string;
  };

  // 2. Khối thông tin tổ chức sở hữu lô
  organization: {
    organizationId: string;
    organizationName: string;
    taxCode: string | null;
    address: string;
    communeName: string;
    provinceName: string;
    representativeName: string | null;
    contactPhone: string | null;
  };

  // 3. Khối danh sách các cảnh báo chi tiết và bằng chứng xác minh
  activeAlerts: LotAlertEvidenceDetail[];

  // 4. Khối dòng sự kiện chuỗi cung ứng (Chế độ Read-only)
  timelineEvents: ReadonlyChainEventItem[];
}

interface LotAlertEvidenceDetail {
  alertType: LotAlertType;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  triggeredAt: string;
  title: string;
  message: string;
  evidenceData: Record<string, any>;   // Dữ liệu chứng cứ tương ứng với từng loại cảnh báo
}

interface ReadonlyChainEventItem {
  eventId: string;
  eventType: string;                   // HARVEST, PREPROCESSING, PACKAGING, TRANSPORT, PROCUREMENT...
  eventTypeName: string;
  recordedAt: string;
  recordedByName: string;
  location: string | null;
  earlyHarvest?: boolean;              // Đánh dấu nếu sự kiện thu hoạch bị ghi đè
  description: string;
}
```

---

### 5.4 Cấu trúc `evidenceData` theo từng loại cảnh báo

| Loại cảnh báo | Cấu trúc `evidenceData` | Giải thích nghiệp vụ |
|:---|:---|:---|
| `RECALLING` | `{"recallRequestId": "uuid", "reason": "Tồn dư thuốc BVTV", "recallScope": "Toàn bộ lô", "recalledAt": "...", "approvedByName": "..."}` | Thông tin quyết định thu hồi lô và lý do phê duyệt |
| `LOCKED_LABEL` | `{"lockedCount": 2, "lockedTraceCodes": ["TC-001***", "TC-002***"], "lockReason": "Quét bất thường", "lockedAt": "...", "lockedByName": "..."}` | Danh sách tem bị khóa, lý do khóa của Quản trị viên |
| `INSPECTION_FAILED` | `{"testRequestId": "uuid", "testingUnitName": "Trung tâm Quatest 3", "conclusion": "FAILED", "failedCriteria": [{"name": "E.coli", "measured": "120 CFU/g", "standardLimit": "<= 100 CFU/g"}], "conclusionDate": "..."}` | Phiếu kiểm nghiệm không đạt kèm chi tiết chỉ tiêu vi phạm |
| `QUARANTINE_OVERWRITTEN` | `{"eligibleHarvestDate": "2026-09-04", "actualHarvestDate": "2026-08-28", "daysEarly": 7, "earlyHarvestReason": "Thu hoạch tránh bão số 3", "pesticidesUsed": [{"name": "Anvil 5SC", "sprayedDate": "2026-08-21", "quarantineDays": 14}]}` | Chi tiết thuốc BVTV đã phun, thời gian cách ly PHI và lý do ghi đè thu hoạch sớm |
| `SERIOUS_FEEDBACK_OPEN` | `{"feedbackId": "uuid", "severity": "QUALITY_SUSPECTED", "status": "IN_PROGRESS", "feedbackContent": "Trái có mùi lạ và ủng nước", "consumerPhone": "0987***321", "submittedAt": "..."}` | Phản ánh nghiêm trọng chưa đóng của người tiêu dùng |
| `INSPECTION_EXPIRED` | `{"earliestExpiryDate": "2026-08-30", "daysOverdue": 12, "inactiveStampCount": 450, "validityStatus": "EXPIRED"}` | Kết quả kiểm nghiệm hết hạn trong khi lô vẫn còn tem chưa kích hoạt |

---

### 5.5 Ví dụ Response `200 OK` (Chi tiết lô cảnh báo)
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy chi tiết lô cảnh báo thành công.",
  "data": {
    "lotInfo": {
      "lotId": "5b050d90-be97-467b-a4cb-dcbf3db81dfd",
      "lotCode": "LOT-202609-001",
      "lotName": "Lô xoài Cát Chu xuất khẩu đợt 1",
      "status": "RECALLED",
      "expectedQuantity": 5000.0,
      "actualQuantity": 4850.0,
      "quantityUnit": "kg",
      "plantingDate": "2026-03-10",
      "harvestDate": "2026-08-28",
      "productCategoryName": "Xoài Cát Chu",
      "farmAreaName": "Vùng trồng Xoài Xuất khẩu Tổ 1",
      "farmAreaAddress": "Ấp Mỹ Hưng, Xã Mỹ Xương, Huyện Cao Lãnh, Tỉnh Đồng Tháp",
      "createdAt": "2026-08-15T08:00:00Z"
    },
    "organization": {
      "organizationId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
      "organizationName": "Hợp tác xã Nông nghiệp Mỹ Xương",
      "taxCode": "1401234567",
      "address": "Ấp Mỹ Hưng, Xã Mỹ Xương, Tỉnh Đồng Tháp",
      "communeName": "Xã Mỹ Xương",
      "provinceName": "Tỉnh Đồng Tháp",
      "representativeName": "Lê Văn Hùng",
      "contactPhone": "02773888999"
    },
    "activeAlerts": [
      {
        "alertType": "RECALLING",
        "severity": "CRITICAL",
        "triggeredAt": "2026-09-10T14:30:00Z",
        "title": "Lô hàng đang trong diện thu hồi khẩn cấp",
        "message": "Quản lý HTX đã phê duyệt yêu cầu thu hồi lô do phát hiện tồn dư hoạt chất BVTV.",
        "evidenceData": {
          "recallRequestId": "3a1b2c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
          "reason": "Phát hiện tồn dư thuốc BVTV vượt ngưỡng sau kiểm nghiệm đối chứng",
          "recallScope": "Toàn bộ lô sản xuất và tất cả lô hàng liên kết",
          "recalledAt": "2026-09-10T14:30:00Z",
          "approvedByName": "Lê Văn Hùng"
        }
      },
      {
        "alertType": "QUARANTINE_OVERWRITTEN",
        "severity": "MEDIUM",
        "triggeredAt": "2026-08-28T15:12:29Z",
        "title": "Ghi đè thu hoạch trước thời gian cách ly thuốc BVTV",
        "message": "Lô được thu hoạch sớm hơn 7 ngày so với ngày an toàn đủ điều kiện.",
        "evidenceData": {
          "eligibleHarvestDate": "2026-09-04",
          "actualHarvestDate": "2026-08-28",
          "daysEarly": 7,
          "earlyHarvestReason": "Thu hoạch sớm tránh bão số 3 gây ngập úng",
          "pesticidesUsed": [
            {
              "materialName": "Thuốc trừ nấm Anvil 5SC",
              "sprayedDate": "2026-08-21",
              "quarantineDays": 14
            }
          ]
        }
      }
    ],
    "timelineEvents": [
      {
        "eventId": "e1a2b3c4-0001-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "HARVEST",
        "eventTypeName": "Thu hoạch nông sản",
        "recordedAt": "2026-08-28T15:12:29Z",
        "recordedByName": "Nguyễn Văn Minh (VT-02)",
        "location": "Vùng trồng Xoài Xuất khẩu Tổ 1 (10.456, 105.678)",
        "earlyHarvest": true,
        "description": "Thu hoạch thực tế 4,850 kg. Ghi đè thời gian cách ly: Thu hoạch sớm tránh bão số 3"
      },
      {
        "eventId": "e1a2b3c4-0002-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "PREPROCESSING",
        "eventTypeName": "Sơ chế và phân loại",
        "recordedAt": "2026-08-29T08:30:00Z",
        "recordedByName": "Trần Thị Lan (VT-03)",
        "location": "Nhà sơ chế HTX Mỹ Xương",
        "description": "Rửa, xử lý nhiệt và đóng thùng tiêu chuẩn xuất khẩu"
      }
    ]
  },
  "errors": null,
  "path": "/api/v1/reports/alert-lots/5b050d90-be97-467b-a4cb-dcbf3db81dfd",
  "timestamp": "2026-09-11T02:05:00.000Z"
}
```

---

## 6. Đặc tả chi tiết Endpoint 3: Xuất danh sách lô cảnh báo (Export API)

### 6.1 Thông tin Endpoint
* **URL:** `/api/v1/reports/alert-lots/export`
* **Method:** `GET`
* **Authentication:** Yêu cầu JWT Bearer Token trong Header: `Authorization: Bearer <token>`
* **Quyền truy cập (RBAC):** Cán bộ quản lý ngành (`VT-05`).
* **Định dạng hỗ trợ:** `EXCEL` (file `.xlsx`, mặc định) hoặc `PDF` (file `.pdf`).

### 6.2 Query Parameters
Kế thừa toàn bộ các tham số lọc của List API (`alertType`, `organizationId`, `unitIds`, `fromDate`, `toDate`, `keyword`), **không truyền `page` và `size`** (xuất toàn bộ các lô thỏa mãn điều kiện lọc trong địa bàn phụ trách của cán bộ).

| Tên tham số | Kiểu dữ liệu | Bắt buộc | Mặc định | Mô tả |
|:---|:---:|:---:|:---:|:---|
| `format` | `String` | Không | `EXCEL` | Định dạng tệp xuất: `EXCEL` hoặc `PDF`. |
| *(các param lọc khác)* | — | Không | — | Giống mục 4.2 (`alertType`, `organizationId`, `unitIds`, `fromDate`, `toDate`, `keyword`). |

---

### 6.3 Response Headers & Content-Type

#### Trường hợp xuất EXCEL (Mặc định)
* **HTTP Status:** `200 OK`
* **Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
* **Content-Disposition:** `attachment; filename="danh-sach-lo-canh-bao-2026-09-11.xlsx"`

#### Trường hợp xuất PDF
* **HTTP Status:** `200 OK`
* **Content-Type:** `application/pdf`
* **Content-Disposition:** `attachment; filename="danh-sach-lo-canh-bao-2026-09-11.pdf"`

---

### 6.4 Nhật ký kiểm toán (Audit Trail)
Mỗi lượt xuất báo cáo thành công được hệ thống tự động ghi vết vào bảng `activity_logs` thông qua `ActivityLogEvent`:
* `action`: `"EXPORT_ALERT_LOTS"`
* `entityType`: `"REPORT"`
* `userId`: UUID cán bộ thực hiện xuất
* `details`: JSON chứa các tham số lọc `{ "format": "EXCEL", "alertType": "...", "unitIds": [...], "fromDate": "...", "toDate": "..." }`

---

## 7. Xử lý ngoại lệ & Bảng mã lỗi chuẩn (Error Contracts)

Mọi phản hồi lỗi tuân thủ định dạng chuẩn `ApiResult`:

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền xem thông tin lô ngoài địa bàn phụ trách.",
  "data": null,
  "errors": null,
  "path": "/api/v1/reports/alert-lots/5b050d90-be97-467b-a4cb-dcbf3db81dfd",
  "timestamp": "2026-09-11T02:00:00.000Z"
}
```

### Bảng chi tiết mã lỗi

| HTTP Status | Mã lỗi nghiệp vụ / Message tiếng Việt | Nguyên nhân phát sinh | Tiêu chí liên quan |
|:---:|:---|:---|:---:|
| `400 Bad Request` | `"Khoảng thời gian không hợp lệ. Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc."` | `fromDate > toDate`. | Validation |
| `400 Bad Request` | `"Loại cảnh báo không hợp lệ."` | Truyền `alertType` không nằm trong danh mục enum hỗ trợ. | TC-04 |
| `401 Unauthorized` | `"Phiên làm việc đã hết hạn hoặc không hợp lệ."` | Chưa truyền Bearer Token hoặc Token JWT không hợp lệ/hết hạn. | Security |
| `403 Forbidden` | `"Bạn không có quyền truy cập chức năng này."` | User không có vai trò `VT-05` (Cán bộ quản lý ngành). | TC-03 |
| `403 Forbidden` | `"Bạn không có quyền xem thông tin lô ngoài địa bàn phụ trách."` | Cán bộ cố tình truy cập `lotId` của một tổ chức không thuộc địa bàn được phân công. | TC-03, QTN-18 |
| `404 Not Found` | `"Không tìm thấy lô sản xuất."` | `lotId` không tồn tại trong hệ thống. | Detail API |
| `500 Internal Server Error` | `"Đã xảy ra lỗi trong quá trình xuất dữ liệu. Vui lòng thử lại sau."` | Lỗi trong quá trình sinh file Excel/PDF hoặc lỗi kết nối. | Export API |

---

## 8. Hướng dẫn kỹ thuật cho Backend và Frontend

### 8.1 Backend Implementation Guidelines
1. **Controller Layer (`ReportController.java`):**
   * Định nghĩa 3 phương thức tương ứng với 3 endpoint ở Mục 3.
   * Sử dụng `@PreAuthorize("hasRole('VT-05') or hasRole('VT-01')")`.
   * Đối với endpoint Export, trả về `ResponseEntity<byte[]>` với Content-Disposition header attachment tương tự `exportIndustrySummary`.
2. **Service Layer (`TerritoryLotAlertService.java`):**
   * Tiêm `AreaScopeService`: Gọi `areaScopeService.resolveOrganizationsForReports(currentUser, unitIds)`.
   * **Kiểm tra rỗng địa bàn (TC-02):** Nếu `scope.isEmptyScope() == true`, lập tức trả về `PageResponse` rỗng với `items = Collections.emptyList()`, `totalElements = 0`, gán thông điệp `AreaScopeService.UNASSIGNED_MESSAGE` vào `ApiResult`.
   * **Truy vấn tổng hợp cảnh báo (TC-01, TC-04):**
     - Thực hiện query `ProductionLot` có `organization.id IN (:orgIds)`.
     - Kết nối với các bảng cảnh báo: `recall_requests` / `shipment_recalls` (`RECALLING`), `trace_codes` có `status = 'LOCKED'` (`LOCKED_LABEL`), `inspection_requests` có kết luận `FAILED` (`INSPECTION_FAILED`), `chain_events` có `event_type = 'HARVEST'` và `early_harvest = true` (`QUARANTINE_OVERWRITTEN`), `product_feedbacks` có `severity IN ('QUALITY_SUSPECTED', 'COUNTERFEIT_SUSPECTED')` và `status != 'CLOSED'` (`SERIOUS_FEEDBACK_OPEN`).
     - Lọc theo `alertType` nếu có tham số (chấp nhận không phân biệt hoa thường và snake_case).
     - Áp dụng `alertTriggeredAt BETWEEN fromDate AND toDate` nếu có khoảng thời gian.
3. **Bảo toàn tính toàn vẹn Read-only (TC-03):**
   * Không cấu hình bất kỳ endpoint mutation nào trong Controller của chức năng này.
   * Tuyệt đối không cho phép gọi các API sửa dữ liệu lô của HTX từ tài khoản `VT-05`.

### 8.2 Frontend Integration Guidelines
1. **API Client (`reportApi.ts`):**
   * Bổ sung các hàm:
     - `getTerritoryAlertLots(params: TerritoryAlertLotParams): Promise<PageResponse<AlertLotSummaryResponse>>`
     - `getTerritoryAlertLotDetail(lotId: string): Promise<AlertLotDetailResponse>`
     - `exportTerritoryAlertLots(params: TerritoryAlertLotParams & { format?: 'EXCEL' | 'PDF' }): Promise<DownloadedReport>`
   * Xử lý serialization cho mảng `unitIds` theo dạng `unitIds=a&unitIds=b` (dùng `URLSearchParams`).
2. **Giao diện Danh sách (`TerritoryAlertLotsPage.tsx`):**
   * **Hiển thị Badge cảnh báo:** Mỗi lô hiển thị danh sách các badge màu sắc tương ứng:
     - Đỏ đậm (`destructive`): `RECALLING` (Thu hồi), `INSPECTION_FAILED` (Kiểm nghiệm không đạt).
     - Cam (`warning`): `LOCKED_LABEL` (Tem bị khóa), `SERIOUS_FEEDBACK_OPEN` (Phản ánh nghiêm trọng).
     - Vàng/Hổ phách: `QUARANTINE_OVERWRITTEN` (Ghi đè cách ly), `INSPECTION_EXPIRED` (Kiểm nghiệm hết hạn).
   * **Empty State chưa gán địa bàn (TC-02):** Khi `response.message` chứa nội dung *"chưa được phân công địa bàn"*, hiển thị giao diện thông báo thân thiện: "Tài khoản của bạn chưa được phân công địa bàn quản lý. Vui lòng liên hệ Quản trị viên để được hỗ trợ phân công."
   * **Bảo đảm chỉ đọc (TC-03):** Trang chi tiết lô cảnh báo không hiển thị bất kỳ nút "Chỉnh sửa", "Thu hồi", "Khóa tem" hay form nhập liệu nào đối với vai trò `VT-05`.
