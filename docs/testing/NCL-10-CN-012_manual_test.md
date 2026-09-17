# Kiểm thử thủ công — NCL-10-CN-012 Ghi nhật ký canh tác ngoại tuyến (MVP)

Ngày cập nhật: 2026-09-17. Nhánh: `feature/NCL-10-CN-012-offline-farm-log` @ `e01a76e4`.

## 0. Môi trường đang chạy (xác minh ngày 17/09)

| Thành phần | Địa chỉ | Trạng thái |
|---|---|---|
| Backend (code mới nhất) | `http://localhost:8080` (`/actuator/health` = UP) | ✅ Đã restart, API verify SUCCESS |
| Frontend (code NCL-10 mới) | **`http://localhost:3001/`** | ✅ Dùng port này |
| Frontend cũ (không rõ nguồn) | `http://localhost:3000/` | ⚠️ KHÔNG dùng để test |
| MySQL XAMPP | `127.0.0.1:3306`, DB `nguon_goc_so` | ✅ |

Tài khoản: **`ghisuA` / `admin123`** (VT-03, HTX Chè Tân Cương);
`managerA` (VT-02, dùng để hủy lô ở TC-05).

Dữ liệu lô hiện tại (8 lô):

| Lô | ID | Trạng thái | Dùng cho |
|---|---|---|---|
| Lô chè A1 | `10710001-…-000000000001` | APPROVED | TC-02/03/04 (chính) |
| Lô chè A2 | `10710002-…-000000000002` | APPROVED | Đã có 1 bản verify API |
| Lô chè A3, A0-1, A0-2 | `…000003/4/5` | APPROVED | Dự phòng |
| Lô chè thu hoạch T9 | `10710006-…-000000000007` | HARVESTED | TC-03b (trạng thái 2) |
| Lô chè sẽ hủy (TC-05) | `10710007-…-000000000008` | APPROVED | TC-05 (hủy sau khi ghi) |
| Lô rau B1 | `10720001-…-000000000006` | APPROVED (HTX Rau Sạch) | Không hiện với user Tân Cương |

Baseline đếm hiện tại: `farm_logs` = **8**, `offline_sync_logs` có **2** bản
FARM_LOG/SUCCESS. Sau mỗi TC đối chiếu số này.

## Chuẩn bị chung

- Chrome + device emulation iPhone/Android; DevTools mở sẵn tab
  Application → IndexedDB → `nong-san-offline` và tab Network (filter `sync`).

## TC-01: Offline là chế độ của màn hình ghi chung (không còn route mobile riêng)

1. Chrome thường (tắt emulation), mở `http://localhost:3001/farm-logs/create`.
2. Kỳ vọng: thấy form "Ghi nhật ký canh tác" (không còn trang "Chỉ khả dụng
   trên thiết bị di động"; route cũ `/mobile/farm-log` redirect về đây).
3. Bật emulation mobile + responsive: layout co về 1 cột, vẫn đầy đủ
   trường, nút "Lưu tạm trên thiết bị" và vùng "Nhật ký chờ đồng bộ".

## TC-02: Ghi offline (Lô chè A1)

1. Emulation mobile + Network → **Offline**.
2. Chọn **Lô chè A1**, "Bón phân", `NPK 16-16-8`, `25`/`kg`,
   ngày **16/09/2026**, ghi chú `Kiem thu 17/09 lan 1` → **Lưu tạm**.
3. Kỳ vọng: toast lưu tạm; IndexedDB `nhat-ky-cho` có 1 bản `status=pending`,
   `productionLotId=10710001-…-000000000001`; vùng "Nhật ký chờ đồng bộ"
   hiện 1 dòng "Bón phân / Lô: Lô chè A1 / Ngày: 2026-09-16". Ghi lại
   `offlineEventId`.

## TC-03: Đồng bộ (Lô chè A1)

1. Network → Online → **"Đồng bộ ngay"**.
2. Kỳ vọng: request `sync` 200, `successCount=1`/`SUCCESS`; toast thành công;
   IndexedDB trống.
3. Verify DB:
   ```sql
   SELECT activity_type, material, quantity, notes FROM farm_logs
   WHERE notes = 'Kiem thu 17/09 lan 1';
   ```
   Kỳ vọng **đúng 1 dòng** `FERTILIZING / NPK 16-16-8 / 25`;
   `SELECT COUNT(*) FROM farm_logs;` = **9**.

## TC-03b: Lô HARVESTED

Lặp TC-02/TC-03 với **Lô chè thu hoạch T9**, hoạt động "Thu hoạch",
ghi chú `Kiem thu 17/09 T9` → phải SUCCESS (`COUNT farm_logs` = **10**).

## TC-04: Chống trùng (QTN-16)

1. Offline ghi 1 bản (Lô chè A2, ghi chú `Kiem thu 17/09 trung`) → online sync.
2. Tắt mạng → "Thử lại tất cả" → bật mạng → sync lại ngay.
3. Kỳ vọng: lần 2 `DUPLICATE`; `SELECT COUNT(*) FROM farm_logs WHERE notes =
   'Kiem thu 17/09 trung';` = **1**.

## TC-05: Hủy lô sau khi ghi offline

1. Offline ghi cho **"Lô chè sẽ hủy (TC-05)"** (ghi chú `Kiem thu 17/09 huy`).
2. Online, đăng nhập `managerA`, **hủy lô này** → quay lại `ghisuA`, sync.
3. Kỳ vọng: bản ghi chuyển trạng thái "Cần xử lý" + lý do "đã bị hủy";
   `farm_logs` **không tăng**; sync lại vẫn còn (dead-letter, không tự xóa).
   Dùng "Xuất CSV" để đối soát rồi "Xóa bản ghi lỗi" để dọn.
4. Khôi phục lô để dùng lại:
   ```sql
   UPDATE production_lot SET status = 'APPROVED'
   WHERE id = '10710007-0000-0000-0000-000000000008';
   ```

## TC-06: Hàng chờ đầy + cache hết hạn

1. Ghi 100 bản offline → bản 101 bị chặn + banner đỏ "Hàng chờ đã đầy".
   Dọn bằng sync hoặc "Xóa bản ghi lỗi".
2. IndexedDB → sửa `cau-hinh/lan-dong-bo-lo` lùi 8 ngày → tải lại khi
   offline → banner đỏ "hết hạn" + form khóa; online 1 lần để gia hạn.

## Dọn dẹp sau test

```sql
DELETE FROM farm_logs WHERE notes LIKE 'Kiem thu 17/09%';
DELETE FROM farm_logs WHERE notes LIKE 'Verify%';
DELETE FROM offline_sync_logs
WHERE offline_event_id NOT IN ('4fe302b6-e5a7-449a-9b9c-bf0bc15ff996');
```

## Kết quả tự động (đã chạy)

- BE: 28/28 (offline-sync + farm-log + controller + export).
- FE: 25/25 module mới; `tsc -b`, `eslint`, `vite build` pass.
- API thật sau restart: FARM_LOG sync SUCCESS (lô A2).
- Full suite FE: 415/418 (1 fail có sẵn theo ngày hệ thống, 2 flaky đã xác
  minh không liên quan).
