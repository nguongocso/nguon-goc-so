# Kiểm thử thủ công — NCL-10-CN-012 Ghi nhật ký canh tác ngoại tuyến (MVP)

Ngày: 2026-09-16. Nhánh: `feature/NCL-10-CN-012-offline-farm-log`.
Môi trường: FE `npm run dev` (port 3000) + BE local (port 8080).
Tài khoản: VT-03 (Người ghi sự kiện) hoặc VT-02.

## Chuẩn bị

- Mở Chrome DevTools → Application → IndexedDB → `nong-san-offline` để quan sát.
- Dùng device emulation (iPhone/Android) cho kịch bản mobile-only.

## TC-01: Desktop bị chặn ở route mobile

1. Trên Chrome desktop, mở `/mobile/farm-log`.
2. Kỳ vọng: thấy trang "Chỉ khả dụng trên thiết bị di động" + nút "Quay lại",
   không thấy form.
3. Bật device emulation mobile, tải lại → thấy form ghi nhật ký.

## TC-02: Ghi offline cơ bản

1. Giả lập mobile + DevTools Network → Offline.
2. Mở `/mobile/farm-log`, chọn lô, hoạt động "Bón phân", vật tư, số lượng,
   ngày hôm qua, ghi chú → "Lưu tạm".
3. Kỳ vọng: toast "đã được lưu tạm"; IndexedDB `nhat-ky-cho` có 1 bản ghi
   `status=pending`; danh sách "Nhật ký chờ đồng bộ" hiện 1 dòng.

## TC-03: Đồng bộ khi có mạng

1. Network → Online (hoặc bấm "Đồng bộ ngay").
2. Kỳ vọng: toast "Đã đồng bộ 1 nhật ký canh tác."; IndexedDB trống;
   `GET /farm-logs?productionLotId=` có bản ghi mới.

## TC-04: Chống trùng (QTN-16)

1. Lặp: ghi offline 1 bản → online sync → tắt mạng ngay → sync lại.
2. Kỳ vọng: server chỉ có 1 `farm_logs`; lần 2 trả `DUPLICATE`, client xóa chờ.

## TC-05: Lô bị hủy / mất quyền khi sync

1. Ghi offline cho lô A → trên desktop hủy lô A (VT-02) → sync.
2. Kỳ vọng: bản ghi chuyển `failed`, hiển thị lý do "đã bị hủy"; bấm "Xóa bản
   ghi lỗi" để dọn.

## TC-06: Hàng chờ đầy + cache hết hạn

1. Ghi 100 bản offline → bản 101 kỳ vọng bị chặn + banner đỏ.
2. DevTools → IndexedDB → sửa `cau-hinh/lan-dong-bo-lo` lùi 8 ngày → tải lại
   khi offline → kỳ vọng banner đỏ "hết hạn" và form bị khóa.

## Kết quả tự động (đã chạy)

- BE: 9/9 offline-sync + 30/30 vùng ảnh hưởng (controller, export, farm-log).
- FE: 25/25 module mới; `tsc -b`, `eslint`, `vite build` pass.
- Full suite: 415/418 (1 fail có sẵn theo ngày hệ thống, 2 flaky đã xác minh).
