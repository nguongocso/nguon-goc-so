# NHẬT KÝ TRIỂN KHAI TỐI ƯU HÓA THỐNG KÊ & XUẤT DỮ LIỆU LỚN (NGUONGOCSO)

## 1. THÔNG TIN KHẢO SÁT BAN ĐẦU
- **Thời điểm bắt đầu:** 2026-09-24
- **Branch làm việc:** `feature/perf-export-statistics-optimization` (nhánh rẽ từ `develop`)
- **Backend root:** `backend/` (Spring Boot 3.5.16, Java 21, JPA/Hibernate, Flyway, Maven)
- **Frontend root:** `frontend/` (React, Vite, TypeScript, TailwindCSS)
- **Baseline Build & Test:**
  - Backend `test-compile`: SUCCESS
  - Frontend `npm run build`: SUCCESS

---

## 2. KẾT QUẢ ĐỐI SOÁT CHI TIẾT TỪNG MỤC KẾ HOẠCH VỚI CODEBASE THỰC TẾ

### Mục 1.1: Vá Bug O(N²) trong Export Open Data CSV
- **Tài liệu kế hoạch:** Chỉ định `ExportServiceImpl.convertToCsv`.
- **Thực tế codebase:** `ExportServiceImpl` đã được tái cấu trúc theo Single Responsibility Principle thành các lớp con:
  - `vn.nguongocso.export.service.processor.OpenDataExportProcessor` phụ trách toàn bộ snapshot và chuyển đổi OpenData sang JSON/CSV.
  - `vn.nguongocso.export.service.renderer.ExportCsvRenderer` phụ trách kết xuất hồ sơ theo mẫu đối tác (`exportWithTemplate`).
- **Phát hiện:** Phương thức `convertToCsv` nằm tại `OpenDataExportProcessor.java` (dòng 312–336). Cần đảm bảo logic escape chuẩn CSV, tránh nhân bản bộ nhớ Heap và viết Unit Test với tối thiểu 3 shipments có timeline riêng biệt để chứng minh không bị rò rỉ timeline chéo.

### Mục 1.2: Vá Bug quét toàn bộ CSDL trong getEventTypesByShipment
- **Vị trí thực tế:** `OpenDataExportProcessor.java` dòng 171–173.
- **Phát hiện:** `chainEventRepository.findByShipmentIsNullAndEventTypeIn(unassignedTypes)` không có tham số phạm vi. Cần bổ sung lọc theo danh sách `productionLotIds` của các shipment cần xuất (hoặc theo tổ chức và khoảng thời gian xuất), loại bỏ việc quét toàn bộ bảng `chain_events`.
- **Bổ sung:** Tại dòng 202–204, `getDocumentationExistence` gọi `filter(farmLogRepository::existsByProductionLotId)` gây ra N+1 queries. Cần thay bằng batch query `findProductionLotIdsWithFarmLogsIn(lotIds)`.

### Mục 1.3: Bổ sung `@Max` và `@Size(max)` cho các DTO Export
- **Vị trí thực tế:**
  - `ExportLabelsRequest.java`: Cột `count` có `@Min(1)`, thiếu `@Max(500)`.
  - `BatchDossierExportRequest.java`: Cột `shipmentIds` có `@NotEmpty`, thiếu `@Size(min = 1, max = 20)`.
  - `ExportOpenDataRequest.java`: Cột `shipmentIds` thiếu `@Size(max = 500)`.
- **Kiểm tra Controller:**
  - `LabelExportController`: Đã có `@Valid @RequestBody ExportLabelsRequest request`.
  - `DossierController`: `exportBatchDossier` đã có `@Valid @RequestBody BatchDossierExportRequest request`.
  - `ExportController`: `exportOpenData` đã có `@Valid @RequestBody ExportOpenDataRequest request`.

### Mục 2.1: Composite Indexes (Flyway Migration)
- Thư mục migration: `backend/src/main/resources/db/migration/schema/`
- Phiên bản timestamp tiếp theo: `V20260924000000__add_composite_indexes_for_stats.sql`.
- Lưu ý MySQL: MySQL không hỗ trợ partial index `WHERE shipment_id IS NULL`, do đó sẽ dùng composite index `(event_type, recorded_at, shipment_id)` hoặc `(event_type, recorded_at)`.
- Độ dài tiền tố cho cột text/varchar dài: `location(100)` cho bảng `trace_code_scan_logs`.

### Mục 2.2: Cấu hình Connection Pool HikariCP
- `backend/src/main/resources/application.properties` hiện chưa có cấu hình HikariCP.
- Sẽ bổ sung cấu hình chuẩn 30 connections tối đa, 10 idle, connection timeout 20s.

### Mục 2.3: Chuyển dịch gom nhóm TimeSeries xuống SQL
- `LookupStatisticsServiceImpl.java` dòng 99–105 gọi `traceCodeScanLogRepository.getScannedAtList` rồi tự gom nhóm bằng Java loop `groupScannedAt`.
- Sẽ bổ sung truy vấn tổng hợp SQL `GROUP BY` trực tiếp trong repository.

### Mục 2.4: Phân lô tham số mệnh đề IN (Query Chunking)
- Tạo `vn.nguongocso.common.util.QueryChunkUtils` với `chunkList(List<T>, int chunkSize)`.
- Áp dụng cho các repository query nhận mảng ID lớn (`countScansByTraceCodeIds`, `findByProductionLotIdIn`, v.v.).

### Mục 3.1: POI SXSSFWorkbook
- `IndustryReportExcelGeneratorImpl.java` và `TerritoryAlertLotExcelGeneratorImpl.java` đang dùng `XSSFWorkbook`.
- Chuyển sang `SXSSFWorkbook(100)` và gọi `dispose()` giải phóng tệp tạm.

### Mục 3.2: Async Job Pattern cho Export Open Data
- Tái cấu trúc cơ chế kiểm tra ngưỡng bản ghi và xuất nền khi số lượng lô hàng lớn.

### Mục 4.1 – 4.3: Frontend Optimization
- `ManagementDashboard.tsx` và `CooperativeDashboard.tsx` gọi thừa `getProductionLots()`.
- `LookupStatisticsContent.tsx` thiếu debounce cho bộ lọc ngày.

---
*Ghi nhận nhật ký hoàn tất Bước 0.*
