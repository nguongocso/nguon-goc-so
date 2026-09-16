# Decision Log — NCL-10-CN-012 Ghi nhật ký canh tác khi ngoại tuyến (MVP)

Nhánh: `feature/NCL-10-CN-012-offline-farm-log` (tạo từ `develop` mới nhất).

## 1. Không tạo endpoint sync riêng — mở rộng `POST /chain-events/sync`

Spec gốc đề xuất `POST /api/v1/nhat-ky-canh-tac/sync` cùng migration `offline_id`.
Quyết định: tái dùng endpoint sync chung (đã có dedup `offline_sync_logs`,
partial commit `REQUIRES_NEW`, audit), chỉ thêm `ChainEventType.FARM_LOG` và
nhánh `processFarmLogOffline()` delegate về `FarmLogService.create()`.
Lý do: đúng quy tắc "Do Not Blindly Implement" (`docs/agent/03`), QTN-01/07/25
được giữ nguyên không duplicate logic, không migration DB ở MVP.

## 2. Không migration `farm_logs`

Idempotency đã có qua `offline_sync_logs.offline_event_id UNIQUE` + khóa bi
quan. Không thêm cột `offline_id/danh_muc_cu/thiet_bi_id` ở MVP.

## 3. IndexedDB mới thay vì localStorage (theo chốt với Product)

Hàng chờ chain-event cũ vẫn dùng localStorage; riêng nhật ký canh tác dùng
IndexedDB (`nong-san-offline` v1: `nhat-ky-cho`, `lo-cache`, `cau-hinh`).
Quy ước chống 2 nguồn sự thật: `FARM_LOG` chỉ đi IndexedDB, các loại cũ giữ
localStorage; `useOfflineSync().sync()` gọi cả hai.

## 4. MVP cắt phạm vi ảnh và danh mục vật tư

- Ảnh/đính kèm: form offline chưa nhập ảnh, `images: []` khi sync (phase 2).
- Vật tư: nhập tay text (đúng `CreateFarmLogRequest.material`), chưa cache
  `input_materials` offline.
- Cache offline chỉ gồm lô APPROVED/HARVESTED + TTL 7 ngày; hết hạn chặn ghi mới.
- Retry giữ 3 lần/backoff 5s-15s-30s như hàng chờ cũ (spec gốc đòi 10 lần —
  defer phase 2).

## 5. Mobile-only theo User-Agent thật + route guard (theo chốt với Product)

- Không dùng viewport (`useMediaQuery`): desktop thu nhỏ cửa sổ vẫn bị chặn.
- `useIsMobileDevice()`: `userAgentData.mobile` → regex UA → `false`
  (desktop cảm ứng vẫn `false`). Tablet/iPad qua cổng (chấp nhận ở MVP).
- `MobileOnlyRoute` hiển thị trang báo, không redirect (tránh vòng lặp).
- Route `mobile/farm-log` bọc `RoleRoute(VT-02, VT-03)` + `MobileOnlyRoute`.

## 6. Enum `FARM_LOG` lan sang 2 switch exhaustive

Thêm giá trị enum làm hỏng biên dịch `ExportDisplayFormatter` (BE) và
type-check `eventFormatter.ts` (FE). Đã bổ sung nhãn "Nhật ký canh tác" ở cả
hai. Các switch còn lại (`TerritoryLotAlertServiceImpl`,
`PublicTraceServiceImpl`) đã có `default` nên an toàn.

## 7. Test strategy

- BE: `OfflineSyncEventProcessorFarmLogTest` (success/duplicate/thiếu
  field/lỗi nghiệp vụ) + chạy lại toàn vùng ảnh hưởng (39 tests xanh).
- FE: unit cho `farmLogDb` (fake-indexeddb), `farmLogSync` (mock API),
  schema Zod, mobile gate. Không viết RTL test cho form (shadcn Select +
  jsdom dễ flaky, tiền lệ `RecordMobileEventForm` cũng không có test) —
  bù bằng manual test.
- Full suite FE: 415/418. 3 fail gồm 1 fail có sẵn (`OrganizationUsagePage`
  TC-06, phụ thuộc ngày hệ thống, fail cả khi stash thay đổi) và 2 flaky
  (pass khi chạy lẻ cả trước và sau thay đổi) — kết luận không regression.

## 8. Còn tồn đọng (phase 2)

- Đồng bộ ảnh/đính kèm + nén client-side.
- Cache danh mục vật tư/hoạt động, retry 10 lần theo spec gốc.
- `useOfflineSync`: tách `getEventLabel` dùng chung với `OfflineEventList`.
