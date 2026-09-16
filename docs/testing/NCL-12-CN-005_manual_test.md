# Kịch bản kiểm thử thủ công — NCL-12-CN-005 Cảnh báo khóa truy cập sắp hết hạn và sắp chạm hạn mức

> Nguyên tắc triển khai (đã chốt với nghiệp vụ):
> - **Không tạo trang/route mới.** Cảnh báo hiển thị trên trang **Cảnh báo tổng hợp** (`/alerts`,
>   nút khiên trên header, NCL-08-CN-016) và **thông báo trên chuông** (NCL-08-CN-005).
> - Kịch bản hết hạn: quét **theo ngày** (01:00 sáng), mỗi khóa tối đa **1 thông báo/ngày**.
> - Kịch bản hạn mức: **chạm mốc 80% là báo ngay**, mỗi khóa tối đa **1 thông báo/ngày**
>   (cờ `warning_sent_at` ở DB, chống trùng cả khi nhiều instance / restart).
> - Nội dung cảnh báo chỉ chứa **tên đối tác + số liệu thật**, không lộ dữ liệu test/thông tin thừa.

---

## 1. Môi trường & chuẩn bị

- BE: `http://localhost:8080` (đang chạy `dev` — MySQL XAMPP, db `nguon_goc_so`).
- FE: `http://localhost:3000`.
- Cài seed dữ liệu test (idempotent, chạy lại được):
  ```powershell
  cmd /c "C:\xampp\mysql\bin\mysql.exe -h 127.0.0.1 -P 3306 -u root --default-character-set=utf8mb4 nguon_goc_so < docs\sample-data\seed_ncl12cn005_apikey_warning_test.sql"
  ```
- Chạy lại có thể ghi đè trạng thái `EXPIRED` trong các lần test trước; nếu khóa TC-01 đã bị scan
  chuyển `EXPIRED`, cập nhật lại về `ACTIVE` trước khi test:
  ```sql
  UPDATE partner_api_keys SET status='ACTIVE', expires_at=DATE_ADD(NOW(), INTERVAL 5 DAY)
  WHERE id='00000000-0000-0000-0000-001200000001';
  ```

### Tài khoản

| Tài khoản | Vai trò | Tổ chức | Dùng cho |
|---|---|---|---|
| `admin` | VT-01 Quản trị viên nền tảng | SYSTEM | Xem cảnh báo toàn nền tảng (`/alerts` nguồn 8 & 9) |
| `managerA` | VT-02 Quản lý hợp tác xã | HTXA | Kiểm thử chuông + `/alerts` theo tổ chức; sở hữu 4 khóa test |

> `managerA` (org HTXA `aaa00001-0000-0000-0000-000000000001`) nhận được thông báo vì là user
> `VT-02` đầu tiên của tổ chức có permission nhận thông báo.

### Dữ liệu test (do seed tạo, org HTXA)

| Khóa | partner_name | key_prefix | Hạn mức (lượt/h) | Hết hạn | Status | is_test |
|---|---|---|---|---|---|---|
| TC-01 | Công ty TNHH Nông sản Bình Minh | `nks_live_710exp01` | 100 | +5 ngày | ACTIVE | FALSE |
| TC-02 | Hợp tác xã Cà phê Tân Cương | `nks_test_710qta02` | 10 | +25 ngày | ACTIVE | TRUE |
| TC-03 | Công ty CP Chế biến Gia vị Đại Việt | `nks_live_710rev03` | 100 | +20 ngày | **REVOKED** | FALSE |
| ĐC | Hợp tác xã Rau an toàn Sơn La | `nks_live_710ok04` | 1000 | +365 ngày | ACTIVE | FALSE |

RAW KEY (gửi header `X-API-KEY` khi gọi cổng đối tác):

- TC-01: `nks_live_710exp01aabbccddeeff00112233445566778899aabbccddeeff01`
- TC-02: `nks_test_710qta02aabbccddeeff00112233445566778899aabbccddeeff02`
- TC-03: `nks_live_710rev03aabbccddeeff00112233445566778899aabbccddeeff03`
- ĐC: `nks_live_710ok04aabbccddeeff00112233445566778899aabbccddeeff04`

---

## 2. Kịch bản kiểm thử

| ID | Kịch bản | Bước | Kết quả mong đợi |
|---|---|---|---|
| TC-01 | Cảnh báo sắp hết hạn — trang tổng hợp | Đăng nhập `managerA` → bấm **icon khiên** trên header → trang Cảnh báo tổng hợp (`/alerts`) | Có 1 mục `Khóa truy cập sắp hết hạn`, đối tượng `nks_live_710exp01`, mức Medium, message: `Khóa của đối tác "Công ty TNHH Nông sản Bình Minh" còn 4 ngày (hết hạn <dd/MM/yyyy>).` — **nội dung không chứa mã test/TC-x** |
| TC-02 | Cảnh báo hết hạn — thông báo chuông | Bấm **icon chuông** → mục "Khóa truy cập sắp hết hạn" có unread (badge đếm tăng) | Popup chi tiết mở tại chỗ: nội dung `Khóa truy cập của đối tác "..." sẽ hết hạn sau N ngày (vào <dd/MM/yyyy HH:mm>). Vui lòng gia hạn để đối tác không bị gián đoạn kết nối.`; không tự điều hướng sang trang khác |
| TC-03 | Lối tắt "Xem khóa" | Trong popup chi tiết (chuông hoặc trang Thông báo) bấm **Xem khóa** | Chuyển tới `/integration/api-keys` (trang quản trị khóa có sẵn); nếu thông báo đang chưa đọc thì được đánh dấu đã đọc |
| TC-04 | Chống trùng hết hạn — quét 2 lần/ngày | Chạy job quét lần 2 trong cùng ngày (hoặc gọi lại `scanExpiringKeys`) | Không tạo thêm thông báo trùng `entityId` + title trong ngày (câu `SELECT ... FROM notifications WHERE entity_id='...001200000001' AND title='Khóa truy cập sắp hết hạn'` đếm = 1) |
| TC-05 | Khóa đã thu hồi không cảnh báo | Xem `/alerts` + chuông của `managerA` | Không có mục/thông báo nào cho `nks_live_710rev03` (TC-03 bị bỏ qua khi quét); gọi API bằng key TC-03 trả `401 Khóa truy cập đã bị thu hồi` |
| TC-06 | Đối chứng — khóa bình thường | Kiểm tra `/alerts` + chuông | Không có cảnh báo nào cho `nks_live_710ok04` (còn 365 ngày, hạn mức cao) |
| TC-07 | Chạm hạn mức 80% — báo ngay | Gọi cổng đối tác bằng key TC-02 đúng **8 lượt thành công** rải trong ngày: `curl.exe -H "X-API-KEY: <RAW KEY TC-02>" http://localhost:8080/api/v1/partner/trace/TEST-TRACE-001` × 8 | Đủ 8/10 lượt (80%) → chuông có 1 thông báo `Khóa truy cập sắp chạm hạn mức`: `... đã dùng 8/10 lượt gọi trong ngày hôm nay (đạt 80%, ngưỡng cảnh báo 80%). Vui lòng nâng hạn mức hoặc điều tiết tần suất gọi.`; `/alerts` có mục `API_KEY_QUOTA_WARNING` |
| TC-08 | Không trùng cảnh báo hạn mức | Tiếp tục gọi key TC-02 lượt thứ 9 (vẫn 200, chưa tới ngưỡng chặn 10) và lượt 10 | Không tạo thêm thông báo hạn mức thứ 2 trong ngày (cờ `warning_sent_at` đã đặt) |
| TC-09 | Vượt hạn mức giờ → 429 | Gọi key TC-02 lượt **11 trở đi trong cùng 1 giờ** | HTTP `429` với message `Khóa truy cập đã vượt quá hạn mức 10 lượt gọi/giờ` (QTN-20 không đổi) |
| TC-10 | Badge "Tổng số khóa" đúng | Đăng nhập `managerA` → `/integration/api-keys` | Thẻ "Tổng số khóa API" = 4 (không còn là 0); bảng có 4 khóa; key TC-02 có cột **số lượt gọi hôm nay** = 10 (khi đã test TC-07→09) |
| TC-11 | VT-01 toàn nền tảng | Đăng nhập `admin` → `/alerts` | Có thể thấy các nguồn `API_KEY_EXPIRING`/`API_KEY_QUOTA_WARNING` của mọi tổ chức (orgId null = toàn nền tảng) |
| TC-12 | Nội dung không lộ dữ liệu test | Soi nội dung message trên `/alerts` + chuông + DB | Không xuất hiện chuỗi `TC-0`, `Đối tác Test` hay raw key; chỉ có tên đối tác thật |
| TC-13 | Restart + đối soát hạn mức | (Tùy chọn) reset cờ `warning_sent_at` của usage TC-02 hôm nay về NULL → khởi động lại BE → chờ job đối soát | Job gửi bù đúng **1** thông báo cho khóa đã vượt ngưỡng, không trùng (TC-05 của doc API) |

---

## 3. Kiểm tra lệnh BM&TT cho từng kịch bản (API reference)

### TC-01/TC-11 — Xem cảnh báo tổng hợp

```powershell
# Login admin -> selectionToken -> organizations -> select org -> accessToken
$login = '{"username":"admin","password":"admin123"}'
$j = (Invoke-WebRequest -Uri http://localhost:8080/api/v1/auth/login -Method POST -Body $login -ContentType application/json -UseBasicParsing).Content | ConvertFrom-Json
$sel = $j.data.selectionToken
$h0 = @{ Authorization = "Bearer $sel" }
$orgId = ((Invoke-WebRequest -Uri http://localhost:8080/api/v1/auth/organizations -Headers $h0 -UseBasicParsing).Content | ConvertFrom-Json).data[0].organizationId
$tok = ((Invoke-WebRequest -Uri http://localhost:8080/api/v1/auth/select-organization -Method POST -Body "{`"organizationId`":`"$orgId`"}" -ContentType application/json -Headers $h0 -UseBasicParsing).Content | ConvertFrom-Json).data.accessToken
$h = @{ Authorization = "Bearer $tok" }
(Invoke-WebRequest -Uri "http://localhost:8080/api/v1/alerts/aggregate?type=API_KEY_EXPIRING" -Headers $h -UseBasicParsing).Content
```

### TC-07/08/09 — Bắn lượt gọi cổng đối tác bằng key TC-02

```powershell
$key = "nks_test_710qta02aabbccddeeff00112233445566778899aabbccddeeff02"
1..11 | ForEach-Object { $c = $_; try { $r = Invoke-WebRequest -Uri http://localhost:8080/api/v1/partner/trace/TEST-TRACE-001 -Headers @{ "X-API-KEY" = $key } -UseBasicParsing; "l$c=$($r.StatusCode)" } catch { "l$c=$($_.Exception.Response.StatusCode.value__)" } }
```

### Kiểm tra số thông báo trong ngày (chống trùng)

```sql
SELECT title, COUNT(*) FROM notifications
WHERE entity_id='00000000-0000-0000-0000-001200000002'
  AND created_at >= CURDATE()
GROUP BY title;   -- Kỳ vọng: mỗi title = 1
```

### Kiểm tra cờ warning_sent_at (TC-08)

```sql
SELECT api_key_id, call_count, warning_sent_at
FROM partner_api_key_daily_usage
WHERE usage_date = CURDATE();
```

---

## 4. Kết quả chạy tự động (ghi nhận khi kết thúc)

| Kiểm tra | Kết quả |
|---|---|
| `backend: mvnw -q test -Dtest=ApiKeyWarningServiceTest,PartnerApiKeyServiceTest,PartnerApiKeyUsageServiceTest,AggregateAlertServiceTest` | 38 tests / 0 fail |
| `frontend: npm run lint` | PASS (0 lỗi) |
| `frontend: npm run build` (tsc + vite) | PASS |
| Backend runtime smoke (login → alerts → partner call → 429) | PASS |

> Ghi chú runtime đã xác nhận 2026-09-16: `/alerts?type=API_KEY_EXPIRING` trả `nks_live_710exp01`
> với message `Khóa của đối tác "Công ty TNHH Nông sản Bình Minh" còn 4 ngày (hết hạn 20/09/2026).`
> — nội dung đã sạch, không còn mã test (xem kịch bản TC-12).

---

## 5. Ghi chú khôi phục sau test

Xóa dữ liệu test khỏi DB (nếu muốn reset sạch):

```sql
DELETE FROM partner_api_key_daily_usage WHERE api_key_id LIKE '00000000-0000-0000-0000-0012%';
DELETE FROM notifications WHERE entity_id LIKE '00000000-0000-0000-0000-0012%';
DELETE FROM partner_api_keys WHERE id LIKE '00000000-0000-0000-0000-0012%';
```

> File seed + kịch bản này **không đưa vào CI** (quy ước `docs/sample-data/`, `docs/testing/`).