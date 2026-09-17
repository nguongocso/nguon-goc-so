# Decision Log — NCL-10-CN-012 Ghi nhật ký canh tác khi ngoại tuyến (MVP)

Nhánh: `feature/NCL-10-CN-012-offline-farm-log` (tạo từ `develop` mới nhất).

## 1. Không tạo endpoint sync riêng — mở rộng `POST /chain-events/sync`

Kiểm tra lại spec gốc (`Bản sao của Nguồn Gốc Số.xlsx`, sheet "Product Backlog",
dòng `NCL-10-CN-012`): spec **không** quy định endpoint riêng hay migration `offline_id`.
Điều kiện sau hoàn thành chỉ yêu cầu "đồng bộ đúng một lần vào lô tương ứng kèm ngày
thực hiện đúng thực tế", quy tắc áp dụng là `QTN-16` + `QTN-07`.
Quyết định: tái dùng endpoint sync chung (đã có dedup `offline_sync_logs`,
partial commit `REQUIRES_NEW`, audit), chỉ thêm `ChainEventType.FARM_LOG` và
nhánh `processFarmLogOffline()` delegate về `FarmLogService.create()`.
Lý do: đúng quy tắc "Do Not Blindly Implement" (`docs/agent/03`), QTN-01/07/25
được giữ nguyên không duplicate logic, không migration DB.

## 2. Không migration `farm_logs`

Idempotency đã có qua `offline_sync_logs.offline_event_id UNIQUE` + khóa bi
quan. Không thêm cột `offline_id/danh_muc_cu/thiet_bi_id` ở MVP.

## 3. Offline là một chế độ của màn hình ghi nhật ký hiện có (chốt Q1 = a)

Bỏ màn hình song song `mobile/farm-log` + `RecordFarmLogForm` + cổng `MobileOnlyRoute`:
chế độ ngoại tuyến nằm **trong** `pages/farm-log/CreateFarmLogPage` và
`components/farm-log/CreateFarmLogForm` — đúng ý tưởng "offline chỉ là một phần nhỏ của
chức năng ghi nhật ký". Một form, một bộ luật (`farmLogOfflineSchema`), một nguồn nhãn
hoạt động (`utils/farmLogActivity.ts`).

Cổng `MobileOnlyRoute` + `useIsMobileDevice` bị xoá (chốt Q2 = cho mọi thiết bị): spec
chỉ yêu cầu "giao diện di động cho phép mở biểu mẫu nhật ký khi mất mạng", không yêu cầu
chặn desktop.

Vẫn dùng IndexedDB (`nong-san-offline`) cho nhật ký canh tác vì cần lưu ảnh dạng blob
(cửa hàng `tep-dinh-kem`); hàng chờ chain-event cũ giữ localStorage;
`useOfflineSync().sync()` gọi cả hai.

## 4. Phạm vi dữ liệu tải sẵn và ảnh ngoại tuyến (chốt Q3 = a, CV-01)

- Danh mục tải sẵn (TTL **7 ngày** cho cả ba): **danh sách lô** (APPROVED/HARVESTED) +
  **danh mục vật tư đang hoạt động** + **loại hoạt động**. Đúng Expected Result của
  `NCL-10-CN-012-CV-01` và mô tả story: "danh mục vật tư và loại hoạt động được tải về
  thiết bị khi còn mạng và có ngày hết hạn". Chưa tải/hết hạn ⇒ banner đỏ + chặn ghi
  ngoại tuyến mới (đúng Precondition "đã đồng bộ danh mục khi còn mạng").
  Chi tiết: `docs/NCL-10-CN-012/data-scope.md`.
- Ảnh: lưu blob trong cửa hàng `tep-dinh-kem`, **nén client-side** trước khi lưu và gửi
  **sau** phần dữ liệu qua `POST /api/v1/farm-logs/{logId}/attachments` (đúng
  `NCL-10-CN-012-CV-03` "nén ảnh gửi sau phần dữ liệu").
- Gửi **lần lượt từng bản ghi** (đúng mô tả story "gửi lần lượt các bản ghi chờ"), không
  gộp một batch lớn.
- Retry tự động **3 lần** (backoff 5s/15s/30s) như hàng chờ cũ. Spec gốc không quy định số
  lần thử; hết lượt **không** xoá bản ghi (xem mục 5).

## 5. Bản ghi lỗi không bị xoá — dead-letter (chốt Q4 = a, TC-04)

Spec (mô tả story, điều kiện sau hoàn thành của `NCL-10-CN-006`, QTN-16 Else) yêu cầu
"bản ghi không gửi được thì giữ trong danh sách chờ kèm lý do". Vì vậy:

- Lỗi nghiệp vụ (sai quyền, lô đã hủy/thu hồi...) ⇒ trạng thái `invalid`: giữ nguyên bản
  ghi + lý do tiếng Việt, **không** tăng lượt thử, **không** tự xoá.
- Lỗi mạng ⇒ `failed` + tăng lượt thử + backoff.
- Người dùng chủ động "Thử lại" / "Xuất CSV đối soát" / "Xoá bản ghi lỗi".
- Bản ghi kẹt `syncing` (đóng tab giữa lúc gửi) được thu hồi về `pending` sau 2 phút.
- Sau khi nội dung đã lên máy chủ nhưng ảnh chưa xong, bản ghi giữ trạng thái `da-ghi`
  kèm `farmLogId` để lần sau chỉ tải ảnh, không gửi lại nội dung (tránh mất ảnh).

## 6. Enum `FARM_LOG` lan sang 2 switch exhaustive

Thêm giá trị enum làm hỏng biên dịch `ExportDisplayFormatter` (BE) và
type-check `eventFormatter.ts` (FE). Đã bổ sung nhãn "Nhật ký canh tác" ở cả
hai. Các switch còn lại (`TerritoryLotAlertServiceImpl`,
`PublicTraceServiceImpl`) đã có `default` nên an toàn.

## 7. Test strategy

- BE: `OfflineSyncEventProcessorFarmLogTest` (success / duplicate / thiếu field / lỗi nghiệp
  vụ / **thiếu quyền `FARM_LOG/CREATE`** / **ngày thực hiện ở tương lai**) + chạy lại toàn
  vùng ảnh hưởng.
- FE: unit cho `farmLogDb` (fake-indexeddb: hàng chờ, danh mục lô/vật tư, hạn 7 ngày,
  ảnh blob), `farmLogSync` (mock API: gửi lần lượt, chống trùng, giữ bản ghi lỗi,
  thu hồi bản kẹt `syncing`, tải ảnh 2 pha), `farmLogOfflineSchema`, `anhNen` (phần
  thuần không cần canvas).
- Không viết RTL test cho form (shadcn Select + jsdom dễ flaky, tiền lệ
  `RecordMobileEventForm`) — bù bằng manual test
  (`docs/testing/NCL-10-CN-012_manual_test.md`).

## 8. Ghi chú kỹ thuật còn lại

- Không lưu giờ thiết bị (chốt Q6 = b): `farm_logs.created_at` vẫn là giờ máy chủ, không
  thêm cột/migration. Giới hạn này ghi rõ trong `docs/api/farm/OfflineFarmLogSync.md`.
- Quyền khi đồng bộ được kiểm tra như khi ghi trực tuyến (chốt Q7): nhánh `FARM_LOG` gọi
  `PermissionChecker.check("FARM_LOG", "CREATE")`.
- Không chống trùng theo nội dung: QTN-16 chỉ yêu cầu chống trùng theo **mã định danh**.
- `useOfflineSync`: `getEventLabel` vẫn cục bộ, tách dùng chung với `OfflineEventList` là
  việc dọn dẹp sau.
- Hàng chờ tối đa 100 bản ghi (chặn ghi mới khi đầy, không xoá bản ghi cũ).
