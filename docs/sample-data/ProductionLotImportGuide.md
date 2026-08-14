# Hướng dẫn Nhập Dữ liệu Lô Sản xuất Hàng loạt (Production Lot Import Guide)

## Mục đích

Tính năng nhập dữ liệu lô sản xuất hàng loạt cho phép người dùng tải lên một tệp CSV chứa danh sách lô sản xuất và nhật ký canh tác tương ứng. Hệ thống sẽ đọc, kiểm tra tính hợp lệ và lưu dữ liệu vào cơ sở dữ liệu trong một giao dịch duy nhất.

Tính năng này giúp:
- Tiết kiệm thời gian khi cần tạo nhiều lô sản xuất cùng lúc.
- Nhập đồng thời cả lô sản xuất và nhật ký canh tác (FarmLog) trong cùng một tệp.
- Phát hiện lỗi dữ liệu sớm, trước khi lưu vào hệ thống.
- Lưu lịch sử nhập để theo dõi và đối chiếu.

---

## Định dạng Tệp được Hỗ trợ

| Thuộc tính    | Giá trị          |
|---------------|------------------|
| Định dạng     | CSV              |
| Mã hóa        | UTF-8            |
| Dấu phân cách | Dấu phẩy (`,`)   |
| Dòng tiêu đề  | Bắt buộc (dòng đầu tiên) |

> **Lưu ý:** Hệ thống chỉ hỗ trợ tệp `.csv`. Không hỗ trợ `.xlsx`, `.xls`, `.tsv` hoặc các định dạng khác.

---

## Cột Dữ liệu

Dưới đây là danh sách tất cả các cột bắt buộc có trong mẫu CSV. **Tất cả 13 cột đều bắt buộc phải có trong dòng tiêu đề**, nhưng giá trị của một số cột có thể để trống tùy theo ngữ cảnh.

| #  | Tên cột                 | Bắt buộc | Mô tả                                                                  | Ví dụ                                           |
|----|-------------------------|----------|------------------------------------------------------------------------|-------------------------------------------------|
| 1  | `ten_lo`                | ✅       | Tên lô sản xuất. Dùng để nhóm các dòng cùng lô.                       | `Lúa Đông Xuân 2026 - Vụ 1`                     |
| 2  | `ma_loai_nong_san`      | ✅       | Mã UUID của loại nông sản (Product Category).                          | `ca8c8c3b-1234-4567-89ab-cdef01234567`           |
| 3  | `ma_vung_trong`         | ✅*      | Mã UUID của vùng trồng (Farm Area). Có thể để trống.                  | `fa8c8c3b-5678-90ab-cdef-1234567890ab`           |
| 4  | `san_luong_du_kien`     | ✅       | Sản lượng dự kiến (kg). Phải là số dương.                              | `1500`                                          |
| 5  | `san_luong_thuc_thu`    | ❌       | Sản lượng thực thu (kg). Có thể để trống nếu chưa thu hoạch.          | `1450` hoặc để trống                            |
| 6  | `ngay_gieo_trong`       | ✅       | Ngày gieo trồng. Định dạng `dd/MM/yyyy`.                              | `05/01/2026`                                    |
| 7  | `ngay_thu_hoach`        | ❌       | Ngày thu hoạch dự kiến. Định dạng `dd/MM/yyyy`. Có thể để trống.     | `15/05/2026`                                    |
| 8  | `hoat_dong_canh_tac`    | ✅*      | Mã hoạt động canh tác. Xem bảng Enum bên dưới. Có thể để trống.       | `PLANTING`                                      |
| 9  | `vat_tu`                | ❌       | Tên vật tư / nguyên liệu sử dụng. Có thể để trống.                    | `Phân NPK 16-16-8`                              |
| 10 | `so_luong`              | ❌       | Số lượng vật tư sử dụng (số thực). Có thể để trống.                   | `25`                                            |
| 11 | `don_vi`                | ❌       | Đơn vị tính cho vật tư (kg, lít, cây, lan, trụ…).                     | `kg`                                            |
| 12 | `ngay_thuc_hien`        | ❌       | Ngày thực hiện hoạt động canh tác. Định dạng `dd/MM/yyyy`.            | `20/01/2026`                                    |
| 13 | `ghi_chu`               | ❌       | Ghi chú cho hoạt động canh tác.                                        | `Bón thúc đợt 1`                                |

> \* Cột `ma_vung_trong` có thể để trống (null).  
> \* Cột `hoat_dong_canh_tac` nếu để trống thì sẽ không tạo nhật ký canh tác cho dòng đó.

---

## Enum: Hoạt động Canh tác (`hoat_dong_canh_tac`)

| Mã          | Ý nghĩa               |
|-------------|-----------------------|
| `PLANTING`  | Gieo trồng            |
| `WATERING`  | Tưới nước             |
| `FERTILIZING` | Bón phân            |
| `PESTICIDE` | Phun thuốc trừ sâu    |
| `WEEDING`   | Làm cỏ                |
| `HARVESTING`| Thu hoạch             |
| `OTHER`     | Hoạt động khác        |

> Các giá trị khác ngoài danh sách trên sẽ gây lỗi.

---

## Quy tắc Kiểm tra Dữ liệu (Validation Rules)

### Trường Bắt buộc
- `ten_lo` không được để trống.
- `ma_loai_nong_san` không được để trống và phải là UUID hợp lệ của loại nông sản đang hoạt động trong hệ thống.
- `san_luong_du_kien` phải lớn hơn 0.
- `ngay_gieo_trong` không được để trống.

### Định dạng Ngày
- Tất cả ngày tháng phải theo định dạng `dd/MM/yyyy` (ví dụ: `05/01/2026`).
- `ngay_thu_hoach` phải **sau** `ngay_gieo_trong`.

### Trường Số
- `san_luong_du_kien`: phải là số dương (> 0).
- `san_luong_thuc_thu`: nếu có giá trị, không được âm.
- `so_luong`: số thực, có thể để trống.

### Enum
- `hoat_dong_canh_tac` nếu có giá trị phải là một trong 7 mã hoạt động hợp lệ (PLANTING, WATERING, FERTILIZING, PESTICIDE, WEEDING, HARVESTING, OTHER).

### Tổ chức và Vùng trồng
- Người dùng chỉ có thể nhập cho tổ chức của mình (trừ VT-01 Admin).
- `ma_vung_trong` nếu có giá trị phải là UUID hợp lệ và thuộc về tổ chức đang nhập.
- `ma_loai_nong_san` phải là UUID hợp lệ và là loại nông sản đang hoạt động (`isActive = true`).

### Giới hạn Tệp
- Kích thước tệp tối đa: **10 MB**.
- Chỉ chấp nhận định dạng `.csv`.

---

## Lỗi Thường Gặp và Cách Khắc phục

| Lỗi                                           | Nguyên nhân                                           | Cách khắc phục                                                   |
|-----------------------------------------------|-------------------------------------------------------|------------------------------------------------------------------|
| `Tên lô không được để trống.`                 | Cột `ten_lo` bị bỏ trống.                             | Điền tên lô sản xuất.                                            |
| `Mã loại nông sản không được để trống.`       | Cột `ma_loai_nong_san` bị bỏ trống.                   | Nhập UUID của loại nông sản hợp lệ.                              |
| `Mã loại nông sản không hợp lệ.`              | Giá trị không phải UUID hợp lệ.                       | Kiểm tra lại UUID (định dạng 8-4-4-4-12).                        |
| `Loại nông sản không tồn tại.`                | UUID không khớp với bất kỳ loại nông sản nào.         | Kiểm tra danh sách loại nông sản trong hệ thống.                 |
| `Loại nông sản đã ngừng sử dụng.`             | Loại nông sản đã bị vô hiệu hóa.                      | Sử dụng loại nông sản khác hoặc kích hoạt lại trong hệ thống.    |
| `Sản lượng dự kiến phải lớn hơn 0.`           | `san_luong_du_kien` = 0 hoặc âm.                      | Nhập giá trị dương.                                              |
| `Sản lượng thực thu không được nhỏ hơn 0.`    | `san_luong_thuc_thu` âm.                              | Nhập giá trị ≥ 0 hoặc để trống.                                  |
| `Ngày gieo trồng không đúng định dạng dd/MM/yyyy.` | Sai định dạng ngày hoặc để trống.                | Sửa về định dạng `dd/MM/yyyy`.                                   |
| `Ngày thu hoạch phải sau ngày gieo trồng.`    | Ngày thu hoạch trước hoặc bằng ngày gieo trồng.       | Đảm bảo ngày thu hoạch sau ngày gieo trồng.                      |
| `Mã vùng trồng không hợp lệ.`                 | UUID không đúng định dạng.                             | Kiểm tra UUID vùng trồng.                                        |
| `Vùng trồng không tồn tại.`                   | UUID không khớp với vùng trồng nào trong hệ thống.    | Kiểm tra danh sách vùng trồng.                                   |
| `Vùng trồng không thuộc tổ chức.`             | Vùng trồng thuộc tổ chức khác.                        | Sử dụng vùng trồng thuộc tổ chức của bạn.                        |
| `Hoạt động canh tác không hợp lệ: ...`        | Giá trị không nằm trong danh sách enum.               | Sử dụng một trong: PLANTING, WATERING, FERTILIZING, PESTICIDE, WEEDING, HARVESTING, OTHER. |
| `Tệp không đúng mẫu: thiếu cột bắt buộc '...'.` | Thiếu cột trong dòng tiêu đề.                       | Kiểm tra tệp CSV có đủ 13 cột với tên chính xác.                 |
| `Chỉ hỗ trợ định dạng .csv.`                  | Tệp không có đuôi `.csv`.                              | Lưu tệp với định dạng CSV.                                       |
| `Không thể đọc tệp CSV.`                      | Tệp bị hỏng hoặc không đọc được.                       | Kiểm tra lại nội dung tệp.                                       |
| `Bạn không có quyền nhập dữ liệu cho tổ chức này.` | Cố gắng nhập cho tổ chức khác khi không phải Admin. | Chỉ Admin (VT-01) được phép nhập cho tổ chức khác.               |

---

## Hướng dẫn Sử dụng

### Bước 1: Tải mẫu CSV
- Trên trang **Nhập dữ liệu lô sản xuất hàng loạt**, nhấn nút **"Tải mẫu CSV"**.
- Hệ thống sẽ tải về tệp `mau_nhap_lo_san_xuat.csv` với các cột chuẩn và dữ liệu mẫu.

### Bước 2: Chuẩn bị dữ liệu
- Mở tệp CSV trong Excel, Google Sheets hoặc trình soạn thảo văn bản.
- Điền dữ liệu lô sản xuất và nhật ký canh tác.
- Mỗi lô sản xuất có thể có **nhiều dòng** (mỗi dòng là một hoạt động canh tác).
- Các dòng có cùng `ten_lo` sẽ được nhóm thành một lô sản xuất với nhiều nhật ký canh tác.

### Bước 3: Tải lên và Nhập
- Nhấn **"Chọn tệp CSV"** và chọn tệp đã chuẩn bị.
- (Chỉ VT-01) Chọn tổ chức đích nếu cần nhập hộ.
- Nhấn **"Nhập dữ liệu"**.
- Hệ thống sẽ hiển thị tiến trình tải lên và xử lý.

### Bước 4: Xem Kết quả
- Hộp thoại kết quả sẽ hiển thị:
  - **Tổng dòng**: Tổng số dòng dữ liệu.
  - **Thành công**: Số dòng được lưu thành công.
  - **Thất bại**: Số dòng bị lỗi.
  - **Trạng thái**: SUCCESS (tất cả thành công), PARTIAL_SUCCESS (một phần thành công), hoặc FAILED (tất cả thất bại).
  - **Chi tiết lỗi**: Danh sách các dòng bị lỗi kèm lý do.

### Bước 5: Xử lý Lỗi
- Nếu có dòng lỗi, sửa các lỗi được liệt kê trong tệp CSV.
- Nhập lại tệp đã sửa (các dòng thành công trước đó sẽ được tạo lại với lô mới).

---

## Dữ liệu Mẫu

Các tệp dữ liệu mẫu có sẵn trong thư mục `docs/sample-data/`:

| Tệp                                      | Mục đích                                                         |
|------------------------------------------|------------------------------------------------------------------|
| `production_lot_import_template.csv`     | Mẫu CSV với 20 dòng dữ liệu mẫu thực tế.                         |
| `production_lot_valid.csv`              | Chỉ chứa dữ liệu hợp lệ (5 dòng).                                |
| `production_lot_invalid.csv`            | Chứa các lỗi validation có chủ đích (10 dòng).                   |
| `production_lot_duplicate.csv`          | Chứa các lô trùng tên để kiểm tra xử lý trùng lặp (7 dòng).     |
| `production_lot_large_dataset.csv`      | 200 dòng dữ liệu để kiểm tra hiệu năng (stress test).            |

---

## API Tham chiếu

| Endpoint                                       | Method | Mô tả                               |
|------------------------------------------------|--------|-------------------------------------|
| `/api/v1/production-lots/import`               | POST   | Nhập dữ liệu lô sản xuất            |
| `/api/v1/production-lots/import-template`      | GET    | Tải mẫu CSV                         |
| `/api/v1/production-lots/import-history`       | GET    | Lấy lịch sử nhập dữ liệu            |

### Import Endpoint Chi tiết

**Request:**
```
POST /api/v1/production-lots/import
Content-Type: multipart/form-data

file: <tệp CSV>
organizationId: <UUID, tùy chọn, chỉ dành cho VT-01>
```

**Response (200 OK):**
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "importHistoryId": "uuid",
    "status": "SUCCESS | PARTIAL_SUCCESS | FAILED",
    "fileName": "ten_tep.csv",
    "totalRows": 20,
    "successCount": 18,
    "failedCount": 2,
    "savedLotIds": ["uuid1", "uuid2", ...],
    "errors": [
      {
        "rowNumber": 5,
        "reason": "Tên lô không được để trống."
      }
    ],
    "importedAt": "2026-01-01T00:00:00Z"
  }
}
```

---

## Hỗ trợ

Nếu gặp lỗi không có trong danh sách trên, vui lòng liên hệ quản trị viên hệ thống.