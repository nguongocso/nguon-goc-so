# API Docs – Bảng điều khiển sản lượng và lô


**Tên nhánh:** `feature/production-lot-dashboard`


---


## 1. Lấy dữ liệu Bảng điều khiển sản lượng và số lô


### Thông tin API


| Thuộc tính   | Giá trị                             |
|--------------|-------------------------------------|
| **Method**   | `GET`                               |
| **Endpoint** | `/api/v1/production-lots/dashboard` |
| **Quyền**    | `VT-01`, `VT-02`                    |


* `VT-01`: Quản trị viên hệ thống (Admin) - Có quyền xem dữ liệu của bất kỳ tổ chức nào.
* `VT-02`: Quản lý tổ chức / Hợp tác xã - Chỉ có quyền xem dữ liệu của tổ chức mình.


---


### Request


**Query Parameters:**


| Parameter        | Kiểu   | Bắt buộc | Mặc định | Mô tả                                                                                           |
|------------------|--------|----------|----------|-------------------------------------------------------------------------------------------------|
| `startDate`      | Date   | Không    | Không    | Định dạng `yyyy-MM-dd`. Lọc lô có ngày xuống giống (`plantingDate`) từ ngày này.                |
| `endDate`        | Date   | Không    | Không    | Định dạng `yyyy-MM-dd`. Lọc lô có ngày xuống giống (`plantingDate`) đến ngày này.               |
| `organizationId` | UUID   | Không    | Không    | ID của tổ chức cần lấy báo cáo. Nếu trống, hệ thống tự động dùng ID tổ chức của user đăng nhập. |
| `groupBy`        | String | Không    | `MONTH`  | Nhóm dữ liệu theo thời gian: `DAY` (ngày), `WEEK` (tuần), `MONTH` (tháng), `YEAR` (năm).        |


---


### Response `200 OK`


```json
{
  "success": true,
  "status": 200,
  "data": {
    "summary": {
      "totalLots": 10,
      "totalExpectedYield": 1500.50,
      "totalActualYield": 1250.00
    },
    "byStatus": {
      "DRAFT": 1,
      "PENDING": 2,
      "APPROVED": 3,
      "REJECTED": 0,
      "HARVESTED": 2,
      "PACKAGED": 1,
      "CLOSED": 1
    },
    "timeSeries": [
      {
        "period": "2026-06",
        "lotCount": 4,
        "expectedYield": 600.00,
        "actualYield": 500.00
      },
      {
        "period": "2026-07",
        "lotCount": 6,
        "expectedYield": 900.50,
        "actualYield": 750.00
      }
    ]
  },
  "timestamp": "2026-07-28T09:00:00.000Z"
}
```


### Response `200 OK` – Khi chưa có dữ liệu (Dữ liệu rỗng)


```json
{
  "success": true,
  "status": 200,
  "data": {
    "summary": {
      "totalLots": 0,
      "totalExpectedYield": 0.0,
      "totalActualYield": 0.0
    },
    "byStatus": {
      "DRAFT": 0,
      "PENDING": 0,
      "APPROVED": 0,
      "REJECTED": 0,
      "HARVESTED": 0,
      "PACKAGED": 0,
      "CLOSED": 0
    },
    "timeSeries": []
  },
  "timestamp": "2026-07-28T09:00:00.000Z"
}
```


---


### Lỗi thường gặp


#### 1. Từ chối truy cập (Xem dữ liệu của tổ chức khác khi không có quyền)


**HTTP Status: 403 Forbidden**


```json
{
  "success": false,
  "status": 403,
  "message": "Từ chối truy cập: Bạn không có quyền truy cập dữ liệu của tổ chức này.",
  "path": "/api/v1/production-lots/dashboard",
  "timestamp": "2026-07-28T09:05:00.000Z"
}
```


> **Lưu ý:** Khi xảy ra lỗi này, hệ thống sẽ ghi nhận lịch sử truy cập trái phép vào bảng `report_access_log` với trạng thái `success = false`.


#### 2. Chưa đăng nhập hoặc Token hết hạn


**HTTP Status: 401 Unauthorized**


```json
{
  "success": false,
  "status": 401,
  "message": "Unauthorized",
  "path": "/api/v1/production-lots/dashboard",
  "timestamp": "2026-07-28T09:00:00.000Z"
}
```


---


## 2. Quy tắc nghiệp vụ (Business Rules)


* **QTN-01 – Cách ly dữ liệu giữa các tổ chức:**
    * Mỗi tổ chức chỉ được thấy và thao tác trên dữ liệu của mình.
    * Nếu người dùng có vai trò `VT-02` (Quản lý HTX) truyền vào `organizationId` khác với tổ chức của mình, hệ thống sẽ trả về lỗi `403 Forbidden` và lưu nhật ký truy cập trái phép.
    * Nếu người dùng có vai trò `VT-01` (Quản trị viên hệ thống), cho phép xem dữ liệu của bất kỳ tổ chức nào.
* **QTN-02 – Nhật ký truy cập báo cáo:**
    * Mọi lượt truy cập vào API bảng điều khiển (dù thành công hay thất bại) đều phải được hệ thống ghi nhận lại lịch sử bao gồm: User ID, Organization ID của User, Target Organization ID được yêu cầu, Tên báo cáo (`YIELD_AND_LOT_DASHBOARD`), Thời gian truy cập, IP Address, và Trạng thái thành công/thất bại.


---


## 3. Thiết kế Cơ sở dữ liệu


### Bảng `report_access_log` (Nhật ký truy cập báo cáo)


| Cột                      | Kiểu dữ liệu   | Nullable | Mô tả                                                                           |
|--------------------------|----------------|----------|---------------------------------------------------------------------------------|
| `id`                     | `CHAR(36)`     | NO       | Primary Key (UUID)                                                              |
| `user_id`                | `CHAR(36)`     | NO       | ID của người dùng truy cập (Khóa ngoại đến `users`)                             |
| `organization_id`        | `CHAR(36)`     | NO       | ID tổ chức của người dùng truy cập (Khóa ngoại đến `organizations`)             |
| `target_organization_id` | `CHAR(36)`     | NO       | ID tổ chức mà người dùng muốn truy cập dữ liệu (Khóa ngoại đến `organizations`) |
| `report_name`            | `VARCHAR(255)` | NO       | Tên báo cáo (`YIELD_AND_LOT_DASHBOARD`)                                         |
| `accessed_at`            | `DATETIME`     | NO       | Thời điểm truy cập                                                              |
| `success`                | `TINYINT(1)`   | NO       | Trạng thái truy cập (1: Thành công, 0: Thất bại/Trái phép)                      |
| `ip_address`             | `VARCHAR(45)`  | YES      | Địa chỉ IP của client gửi yêu cầu                                               |


---


## 4. Tiêu chí nghiệm thu (Acceptance Criteria)


* **AC-01:** Dashboard hiển thị đúng tổng số lượng lô, tổng sản lượng dự kiến và tổng sản lượng thực tế theo tổ chức.
* **AC-02:** Thống kê phân rã số lô theo từng trạng thái cụ thể (`DRAFT`, `PENDING`, `APPROVED`, v.v.).
* **AC-03:** Trả về chuỗi dữ liệu theo thời gian (timeSeries) được nhóm theo `DAY`, `WEEK`, `MONTH`, `YEAR` để phục vụ vẽ biểu đồ sản lượng và số lô.
* **AC-04:** Nếu chưa có dữ liệu, trả về đối tượng trống với các số liệu tổng hợp bằng `0`.
* **AC-05:** Cách ly dữ liệu: Người dùng thuộc tổ chức nào chỉ được xem dữ liệu tổ chức đó, ngoại trừ Admin hệ thống (`VT-01`).
* **AC-06:** Tự động lưu nhật ký mỗi lần API được gọi vào bảng `report_access_log`.



