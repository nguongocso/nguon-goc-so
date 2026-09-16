# Kế hoạch Triển khai: Đồng bộ Nhất quán NCL-08-CN-007 và NCL-08-CN-014

> **Dành cho Agentic Workers:** BẮT BUỘC: Sử dụng quy trình kiểm thử trước khi sửa đổi, tuân thủ TDD cho các hàm tính toán, thực hiện xác thực sau mỗi task. Sử dụng cú pháp checkbox (`- [ ]`) để theo dõi tiến độ.

**Mục tiêu:** Khắc phục triệt để nghịch lý "Điểm nghi vấn 95/100 nhưng bảng phân tích toàn +0", lưu snapshot bằng chứng đánh giá nghi vấn, đưa thời gian ân hạn về 0–7 ngày, chuẩn hóa thang điểm về 100, tích hợp NCL-08-CN-001 dùng chung cấu hình ngưỡng, và động hóa toàn bộ nhãn hiển thị trên Frontend.

**Kiến trúc:** 
- Tầng Database: Thêm các cột snapshot điểm chi tiết (`high_frequency_score`, `impossible_travel_score`, `multiple_locations_score`, `evaluated_at`, `violating_scan_log_ids`) vào bảng `trace_codes`. Cập nhật giá trị mặc định của `activation_age_days` về 3 ngày.
- Tầng Backend: `SuspectDetectionServiceImpl` ghi snapshot điểm ngay khi đánh giá; `getSuspectDetail()` đọc từ snapshot thay vì tính lại trên 24h trượt. Cập nhật trọng số điểm: Tần suất cao (+35), Di chuyển phi lý (+45), Nhiều địa điểm (+20). Tích hợp `ScanAnomalyDetectionServiceImpl` đọc ngưỡng động từ `AnomalyThresholdService`. Căn chỉnh `estimateImpact()` khớp công thức điểm nghi vấn.
- Tầng Frontend: Tách rõ khối "Bằng chứng tại thời điểm đánh giá" (snapshot) và "Lịch sử quét 24h gần nhất" (rolling); thay các nhãn hardcode bằng giá trị từ `effectiveThreshold`; đổi nhãn và ràng buộc input của `activationAgeDays` thành [0, 7] ngày; bổ sung badge Danh mục & Nguồn ngưỡng áp dụng.

**Công nghệ sử dụng:** Java 17, Spring Boot 3.5, Spring Data JPA, Hibernate, MySQL, Flyway, JUnit 5, Mockito, React 19, TypeScript, Tailwind CSS, Lucide React.

**Tài liệu nghiệp vụ liên quan:**
- `docs/api/trace/NCL-08-CN-007_SuspectTraceCodeLock.md`
- `docs/api/trace/NCL-08-CN-014_AnomalyThresholdConfiguration.md`
- Báo cáo phân tích tính nhất quán giữa NCL-08-CN-007 và NCL-08-CN-014

## Quy tắc ràng buộc chung (Global Constraints)
- Tất cả comment JavaDoc / inline comment phải viết bằng tiếng Việt có dấu.
- Định dạng UTF-8 không BOM.
- Không thay đổi logic hướng so sánh trong `ScanAnomalyUtils.isWithinGracePeriod()` (`daysSinceActivation < gracePeriodDays` -> bỏ qua đánh giá).
- Giữ nguyên cơ chế khóa thủ công của VT-01 (không tự động khóa tem khi đạt điểm).
- Mã tem mới hoặc sửa đổi phải pass toàn bộ unit test hiện có.

---

### Task 1: P1.1 — Sửa giá trị mặc định & ràng buộc `activationAgeDays` (Backend & Frontend)

**Mục đích:** Đổi giá trị mặc định của `activationAgeDays` từ 365 ngày thành 3 ngày (khuyến nghị 0–7 ngày), chặn nhập ngoài khoảng [0, 7], đổi nhãn hiển thị tiếng Việt chính xác là "Thời gian ân hạn miễn kiểm tra sau kích hoạt (ngày)".

**Files:**
- Create: `backend/src/main/resources/db/migration/schema/V20260916100000__update_anomaly_thresholds_default_grace_period.sql`
- Modify: `backend/src/main/java/vn/nguongocso/alert/service/impl/AnomalyThresholdServiceImpl.java`
- Modify: `backend/src/main/java/vn/nguongocso/alert/dto/request/UpdateGlobalThresholdRequest.java`
- Modify: `backend/src/main/java/vn/nguongocso/alert/dto/request/CategoryThresholdOverrideRequest.java`
- Modify: `backend/src/main/java/vn/nguongocso/alert/dto/request/ImpactEstimationRequest.java`
- Modify: `frontend/src/components/admin/anomaly-threshold/GlobalThresholdCard.tsx`
- Modify: `frontend/src/components/admin/anomaly-threshold/CategoryOverridesTable.tsx`
- Test: `backend/src/test/java/vn/nguongocso/alert/service/AnomalyThresholdServiceImplTest.java`
- Test: `backend/src/test/java/vn/nguongocso/alert/controller/AnomalyThresholdControllerTest.java`

- [ ] **Step 1: Tạo Flyway Migration cập nhật cấu hình mặc định trong DB**
  Tạo `backend/src/main/resources/db/migration/schema/V20260916100000__update_anomaly_thresholds_default_grace_period.sql`:
  ```sql
  -- Cập nhật giá trị mặc định của activation_age_days về 3 ngày nếu đang là 365 ngày mặc định
  UPDATE anomaly_thresholds
  SET activation_age_days = 3
  WHERE product_category_id IS NULL AND activation_age_days = 365;
  ```

- [ ] **Step 2: Sửa hằng số DEFAULT_ACTIVATION_AGE_DAYS trong Backend Service**
  Trong `AnomalyThresholdServiceImpl.java`:
  Đổi: `public static final int DEFAULT_ACTIVATION_AGE_DAYS = 365;`
  Thành: `public static final int DEFAULT_ACTIVATION_AGE_DAYS = 3;`

- [ ] **Step 3: Thêm Validation `@Min(0) @Max(7)` vào các DTO Request**
  Cập nhật trường `activationAgeDays` trong:
  - `UpdateGlobalThresholdRequest.java`
  - `CategoryThresholdOverrideRequest.java`
  - `ImpactEstimationRequest.java`
  Thêm annotation:
  ```java
  @NotNull(message = "Thời gian ân hạn không được để trống")
  @Min(value = 0, message = "Thời gian ân hạn phải lớn hơn hoặc bằng 0")
  @Max(value = 7, message = "Thời gian ân hạn khuyến nghị tối đa là 7 ngày")
  private Integer activationAgeDays;
  ```

- [ ] **Step 4: Cập nhật Unit Test Backend cho ngưỡng 0–7 ngày**
  Sửa các test case trong `AnomalyThresholdServiceImplTest.java` và `AnomalyThresholdControllerTest.java` đang kiểm tra giá trị 365, 180, 200 ngày thành các giá trị hợp lệ (0–7 ngày), và bổ sung test kiểm tra vi phạm `@Max(7)`.
  Chạy lệnh: `mvn test -Dtest=AnomalyThresholdControllerTest,AnomalyThresholdServiceImplTest`
  Kỳ vọng: Toàn bộ test pass.

- [ ] **Step 5: Cập nhật Frontend UI (`GlobalThresholdCard.tsx`, `CategoryOverridesTable.tsx`)**
  Trong `GlobalThresholdCard.tsx`:
  - Đổi state khởi tạo: `useState<number>(3)`
  - Đổi nhãn:
    ```tsx
    <Label htmlFor="global-act-age" className="text-sm font-medium">
      Thời gian ân hạn miễn kiểm tra sau kích hoạt (ngày) <span className="text-destructive">*</span>
    </Label>
    ```
  - Đổi thuộc tính input: `min={0} max={7}`
  - Trong `validate()`:
    ```tsx
    if (isNaN(activationAgeDays) || activationAgeDays < 0 || activationAgeDays > 7) {
      errs.activationAgeDays = 'Thời gian ân hạn phải từ 0 đến 7 ngày';
    }
    ```
  - Bổ sung helper text giải thích rõ ràng dưới input:
    "Trong khoảng thời gian này kể từ khi tem được kích hoạt, các lượt quét sẽ KHÔNG được đánh giá nghi vấn (khuyến nghị 0–7 ngày)."
  - Trong `CategoryOverridesTable.tsx`: đổi tiêu đề cột `Hạn kích hoạt` thành `Thời gian ân hạn`.

---

### Task 2: P1.2 & P1.3 — Lưu Snapshot Score Breakdown vào DB & Chuẩn hóa Thang điểm 100

**Mục đích:**
- Thêm các cột lưu snapshot điểm phân tích chi tiết vào `trace_codes`.
- Điều chỉnh trọng số thuật toán thành: Tần suất cao (+35), Di chuyển phi lý (+45), Nhiều địa điểm (+20). Tổng = 100.
- `evaluateSuspicion()` lưu toàn bộ snapshot vào DB; `getSuspectDetail()` đọc điểm từ snapshot, chấm dứt tình trạng điểm 95 nhưng breakdown = 0.
- Cập nhật migration seed dữ liệu kiểm thử.

**Files:**
- Create: `backend/src/main/resources/db/migration/schema/V20260916110000__add_suspicion_breakdown_to_trace_codes.sql`
- Modify: `backend/src/main/resources/db/migration-test/data/V20260830140000__seed_unlock_test_data.sql`
- Modify: `backend/src/main/java/vn/nguongocso/trace/entity/TraceCode.java`
- Modify: `backend/src/main/java/vn/nguongocso/trace/dto/response/SuspectTraceCodeDetailResponse.java`
- Modify: `backend/src/main/java/vn/nguongocso/trace/dto/response/AnomalyDetails.java`
- Modify: `backend/src/main/java/vn/nguongocso/trace/service/impl/SuspectDetectionServiceImpl.java`
- Test: `backend/src/test/java/vn/nguongocso/trace/service/SuspectDetectionServiceImplTest.java`

- [ ] **Step 1: Tạo Flyway Migration thêm cột snapshot vào `trace_codes`**
  Tạo `backend/src/main/resources/db/migration/schema/V20260916110000__add_suspicion_breakdown_to_trace_codes.sql`:
  ```sql
  ALTER TABLE trace_codes
      ADD COLUMN high_frequency_score INT DEFAULT 0,
      ADD COLUMN impossible_travel_score INT DEFAULT 0,
      ADD COLUMN multiple_locations_score INT DEFAULT 0,
      ADD COLUMN evaluated_at TIMESTAMP NULL,
      ADD COLUMN violating_scan_log_ids TEXT NULL;
  ```

- [ ] **Step 2: Cập nhật Entity `TraceCode.java`**
  Thêm các trường:
  ```java
  @Column(name = "high_frequency_score")
  private Integer highFrequencyScore;

  @Column(name = "impossible_travel_score")
  private Integer impossibleTravelScore;

  @Column(name = "multiple_locations_score")
  private Integer multipleLocationsScore;

  @Column(name = "evaluated_at")
  private LocalDateTime evaluatedAt;

  @Column(name = "violating_scan_log_ids", columnDefinition = "TEXT")
  private String violatingScanLogIds;
  ```

- [ ] **Step 3: Cập nhật Response DTO (`SuspectTraceCodeDetailResponse.java`, `AnomalyDetails.java`)**
  - Thêm `evaluatedAt` vào `SuspectTraceCodeDetailResponse`.
  - Đảm bảo `ScoreBreakdown` nhận các giá trị từ snapshot.

- [ ] **Step 4: Chuẩn hóa Trọng số trong `SuspectDetectionServiceImpl.java`**
  Cập nhật hằng số:
  ```java
  private static final int HIGH_FREQUENCY_SCORE = 35; // cũ: 30
  private static final int IMPOSSIBLE_TRAVEL_SCORE = 45; // cũ: 40
  private static final int MULTIPLE_LOCATIONS_SCORE = 20; // cũ: 15
  private static final int SUSPECT_THRESHOLD = 50; // giữ nguyên 50
  private static final int MAX_SCORE = 100;
  ```

- [ ] **Step 5: Cập nhật `evaluate()` để trả về Violating Scan Log IDs và Record Evaluation**
  Cập nhật `SuspicionEvaluation`:
  ```java
  private record SuspicionEvaluation(
          int highFreqScore,
          int impossibleTravelScore,
          int multipleLocationsScore,
          int impossibleTravelCount,
          int uniqueLocations,
          int totalScore,
          Double firstImpossibleDistanceKm,
          Long firstImpossibleMinutes,
          String violatingScanLogIds) {
  }
  ```
  Trong `evaluate()`: thu thập các ID của `TraceCodeScanLog` vi phạm (chuỗi UUID ngăn cách dấu phẩy).

- [ ] **Step 6: Cập nhật `evaluateSuspicion()` ghi nhận Snapshot vào `traceCode`**
  ```java
  traceCode.setSuspicionScore(evaluation.totalScore());
  traceCode.setHighFrequencyScore(evaluation.highFreqScore());
  traceCode.setImpossibleTravelScore(evaluation.impossibleTravelScore());
  traceCode.setMultipleLocationsScore(evaluation.multipleLocationsScore());
  traceCode.setEvaluatedAt(now);
  traceCode.setViolatingScanLogIds(evaluation.violatingScanLogIds());
  traceCode.setSuspicionReason(reason);
  ```

- [ ] **Step 7: Sửa `getSuspectDetail()` đọc từ Snapshot đã lưu**
  Trong `getSuspectDetail(UUID traceCodeId)`:
  - Nếu `traceCode.getEvaluatedAt() != null` (mã đã từng được đánh giá nghi vấn):
    - Đọc `highFrequency` = `traceCode.getHighFrequencyScore() ?? 0`
    - Đọc `impossibleTravel` = `traceCode.getImpossibleTravelScore() ?? 0`
    - Đọc `multipleLocations` = `traceCode.getMultipleLocationsScore() ?? 0`
    - Bảng `scoreBreakdown` lấy từ các giá trị snapshot này!
  - Vẫn truy vấn `scanLogRepository.findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(traceCodeId, twentyFourHoursAgo)` nhưng CHỈ gán vào `scanLogs` để hiển thị phần "Lịch sử quét 24 giờ gần nhất".
  - Chấm dứt hoàn toàn việc gán breakdown = +0 khi scanLogs rỗng trong khi tổng điểm > 0.

- [ ] **Step 8: Cập nhật dữ liệu Seed Test `V20260830140000__seed_unlock_test_data.sql`**
  - Sửa mã `NCL-TEST-LCK-ADM1-05`:
    Thay vì seed điểm 95 với breakdown không tồn tại, cập nhật:
    `score = 100`, `high_frequency_score = 35`, `impossible_travel_score = 45`, `multiple_locations_score = 20`, `evaluated_at = DATE_SUB(NOW(), INTERVAL 1 DAY)`.
  - Cập nhật các mã khác (`score = 80`: 35 + 45, etc.) sao cho tổng điểm luôn bằng tổng breakdown.

- [ ] **Step 9: Chạy Unit Test `SuspectDetectionServiceImplTest.java`**
  Cập nhật các assertion kiểm tra điểm 35, 45, 20 và snapshot fields.
  Chạy lệnh: `mvn test -Dtest=SuspectDetectionServiceImplTest`
  Kỳ vọng: Toàn bộ test pass.

---

### Task 3: P1.4 — Tích hợp NCL-08-CN-001 (`ScanAnomalyDetectionService`) dùng chung `AnomalyThresholdService`

**Mục đích:** Thay thế các hằng số hardcode `DETECTION_WINDOW_MINUTES = 10` và `SAME_LOCATION_THRESHOLD_KM = 5.0` trong `ScanAnomalyDetectionServiceImpl` bằng cấu hình ngưỡng hiệu lực từ `AnomalyThresholdService`, đồng thời kiểm tra thời gian ân hạn.

**Files:**
- Modify: `backend/src/main/java/vn/nguongocso/alert/service/impl/ScanAnomalyDetectionServiceImpl.java`
- Modify: `backend/src/test/java/vn/nguongocso/alert/service/ScanAnomalyDetectionServiceImplTest.java`

- [ ] **Step 1: Viết test failing kiểm tra tích hợp ngưỡng động trong `ScanAnomalyDetectionServiceImplTest.java`**
  Tạo test case `shouldUseConfiguredThresholdsFromAnomalyThresholdService()`:
  - Mock `anomalyThresholdService.getEffectiveThreshold(...)` trả về window = 20 phút, maxDistance = 15.0 km.
  - Kiểm tra lượt quét trong vòng 15 phút trước được tính vào cửa sổ đánh giá thay vì bị bỏ qua bởi 10 phút cũ.

- [ ] **Step 2: Inject `AnomalyThresholdService` vào `ScanAnomalyDetectionServiceImpl.java`**
  - Inject `AnomalyThresholdService`.
  - Triển khai helper `getEffectiveThreshold(TraceCode traceCode)`:
    ```java
    UUID categoryId = null;
    if (traceCode.getShipment() != null && traceCode.getShipment().getProductionLot() != null
            && traceCode.getShipment().getProductionLot().getProductCategory() != null) {
        categoryId = traceCode.getShipment().getProductionLot().getProductCategory().getId();
    }
    return anomalyThresholdService != null ? anomalyThresholdService.getEffectiveThreshold(categoryId) : null;
    ```

- [ ] **Step 3: Kiểm tra Grace Period và Sử dụng Ngưỡng Động trong `onScanRecorded`**
  - Gate: Nếu còn trong thời gian ân hạn (`isWithinGracePeriod`), return sớm, không tạo cảnh báo.
  - Cửa sổ thời gian: `int windowMinutes = (threshold != null && threshold.getMinTimeBetweenScansMinutes() != null) ? threshold.getMinTimeBetweenScansMinutes() : DETECTION_WINDOW_MINUTES;`
  - Khoảng cách cùng vị trí: `double sameLocDistKm = (threshold != null && threshold.getMaxDistanceKmPer30Min() != null) ? threshold.getMaxDistanceKmPer30Min().doubleValue() : SAME_LOCATION_THRESHOLD_KM;`
  - Thay thế trong `getRecentScanLogs(traceCodeId, windowMinutes)` và `isSameLocation(scan1, scan2, sameLocDistKm)`.

- [ ] **Step 4: Chạy test xác nhận pass**
  Chạy lệnh: `mvn test -Dtest=ScanAnomalyDetectionServiceImplTest`
  Kỳ vọng: Toàn bộ test pass.

---

### Task 4: P2 — Đồng bộ Thuật toán Ước lượng Tác động (Impact Estimation Dry-run)

**Mục đích:** Sửa hàm `estimateImpact()` trong `AnomalyThresholdServiceImpl.java` để mô phỏng chính xác thuật toán chấm điểm của `SuspectDetectionService` (tính điểm tổng hợp `>= 50`, bổ sung tiêu chí `multipleLocations`, dùng trọng số chuẩn 35/45/20).

**Files:**
- Modify: `backend/src/main/java/vn/nguongocso/alert/service/impl/AnomalyThresholdServiceImpl.java`
- Modify: `backend/src/main/java/vn/nguongocso/alert/dto/response/ImpactEstimationResponse.java`
- Modify: `backend/src/test/java/vn/nguongocso/alert/service/AnomalyThresholdServiceImplTest.java`

- [ ] **Step 1: Viết test failing kiểm tra logic ước lượng theo tổng điểm >= 50**
  Trong `AnomalyThresholdServiceImplTest.java`:
  - Mã tem chỉ vi phạm 1 tiêu chí tần suất cao (35 điểm < 50) -> KHÔNG bị tính vào `estimatedAnomaliesCount`.
  - Mã tem vi phạm cả tần suất cao (35) và di chuyển phi lý (45) -> tổng 80 >= 50 -> BỊ tính vào `estimatedAnomaliesCount`.

- [ ] **Step 2: Triển khai tính điểm và kiểm tra `multipleLocations` trong `estimateImpact`**
  Trong `AnomalyThresholdServiceImpl.java`:
  - Bổ sung hàm đếm vị trí duy nhất: `countUniqueLocations(scans)`
  - Tính toán:
    ```java
    boolean highFreq = checkHighFrequency(evaluatedScans, request.getMaxScansPerHour(), request.getMaxScansPerDay());
    boolean impossibleTravel = checkImpossibleTravel(evaluatedScans, request.getMaxDistanceKmPer30Min().doubleValue(), request.getMinTimeBetweenScansMinutes());
    boolean multiLocations = countUniqueLocations(evaluatedScans) >= 5;

    int score = (highFreq ? 35 : 0) + (impossibleTravel ? 45 : 0) + (multiLocations ? 20 : 0);

    if (highFreq) highFrequencyCount++;
    if (impossibleTravel) impossibleTravelCount++;
    if (multiLocations) multipleLocationsCount++;

    if (score >= 50) {
        estimatedAnomaliesCount++;
    }
    ```
  - Cập nhật thông điệp `message` phản ánh rõ ràng: "Dự kiến có X mã tem sẽ đạt ngưỡng nghi vấn (>= 50 điểm)..."

- [ ] **Step 3: Chạy test xác nhận pass**
  Chạy lệnh: `mvn test -Dtest=AnomalyThresholdServiceImplTest`
  Kỳ vọng: Toàn bộ test pass.

---

### Task 5: P1.5 & P3 — Frontend: Nhãn Ngưỡng Động, Tách Snapshot Bằng Chứng, Hiển thị Danh mục & Nguồn Ngưỡng

**Mục đích:**
- Bổ sung `effectiveThreshold` và `productCategoryName` vào API Response của Chi tiết mã tem nghi vấn.
- Trên `SuspectTraceCodeDetailPage.tsx`:
  - Thay các chuỗi hardcode bằng giá trị động từ `effectiveThreshold`.
  - Tách UI rõ ràng thành 2 card: (a) "Bằng chứng tại thời điểm đánh giá" (snapshot), (b) "Hoạt động quét trong 24 giờ gần đây" (rolling).
  - Bổ sung badge hiển thị Tên loại nông sản và Nguồn cấu hình (Mặc định toàn cục hay Ghi đè theo loại).

**Files:**
- Modify: `backend/src/main/java/vn/nguongocso/trace/dto/response/SuspectTraceCodeDetailResponse.java`
- Modify: `backend/src/main/java/vn/nguongocso/trace/service/impl/SuspectDetectionServiceImpl.java`
- Modify: `frontend/src/types/suspectTraceCode.ts`
- Modify: `frontend/src/pages/admin/SuspectTraceCodeDetailPage.tsx`

- [ ] **Step 1: Cập nhật Backend DTO và Mapper**
  Trong `SuspectTraceCodeDetailResponse.java`:
  - Thêm `private AnomalyThresholdResponse effectiveThreshold;`
  - Thêm `private String productCategoryName;`
  - Thêm `private LocalDateTime evaluatedAt;`
  - Trong `SuspectDetectionServiceImpl.getSuspectDetail()`: gán `effectiveThreshold`, `productCategoryName` (từ `productionLot.getProductCategory().getName()`), và `evaluatedAt`.

- [ ] **Step 2: Cập nhật Frontend Types (`frontend/src/types/suspectTraceCode.ts`)**
  Thêm vào `SuspectTraceCodeDetailResponse`:
  ```typescript
  effectiveThreshold?: AnomalyThresholdConfig | null;
  productCategoryName?: string | null;
  evaluatedAt?: string | null;
  ```

- [ ] **Step 3: Cập nhật `SuspectTraceCodeDetailPage.tsx`**
  - **Khối Header/Thông tin:** Thêm badge Danh mục & Nguồn cấu hình:
    ```tsx
    <div>
      <p className="text-sm text-muted-foreground">Loại nông sản</p>
      <div className="flex items-center gap-2 mt-1">
        <span className="font-medium text-slate-900">{detail.productCategoryName || 'Nông sản chung'}</span>
        <Badge variant="outline" className="text-xs">
          {detail.effectiveThreshold?.productCategoryId ? 'Ngưỡng riêng theo loại' : 'Ngưỡng mặc định toàn cục'}
        </Badge>
      </div>
    </div>
    ```
  - **Khối Bằng chứng đánh giá nghi vấn (Snapshot):**
    Tiêu đề: `Chi tiết điểm nghi vấn (Thời điểm đánh giá: {formatDateTime(detail.evaluatedAt)})`
    Hiển thị động các ngưỡng:
    ```tsx
    {/* Tần suất */}
    <span>
      Tần suất quét cao (≥ {detail.effectiveThreshold?.maxScansPerDay ?? 10} lượt/24h hoặc ≥ {detail.effectiveThreshold?.maxScansPerHour ?? 5} lượt/h)
    </span>
    {/* Khoảng cách */}
    <span>
      Khoảng cách không hợp lý (> {detail.effectiveThreshold?.maxDistanceKmPer30Min ?? 50}km trong ≤ {detail.effectiveThreshold?.minTimeBetweenScansMinutes ?? 30} phút)
    </span>
    {/* Nhiều địa điểm */}
    <span>
      Nhiều địa điểm (≥ 5 địa điểm/24h - cố định)
    </span>
    ```
  - **Khối Hoạt động quét gần đây:** Ghi rõ tiêu đề: `Lịch sử quét trong 24 giờ gần đây (hiện tại: {detail.scanLogs.length} lượt quét)`.

---

### Task 6: Kiểm thử Tích hợp Toàn diện & Nghiệm thu (Verification)

**Mục đích:** Đảm bảo toàn bộ backend test suites pass, không còn lỗi hồi quy, dữ liệu kiểm thử đồng bộ.

- [ ] **Step 1: Chạy toàn bộ Test Suite liên quan**
  Chạy lệnh: `mvn test -Dtest=*Anomaly*,*Suspect*`
  Xác nhận tất cả tests PASS 100%.

- [ ] **Step 2: Kiểm tra đối chiếu Acceptance Criteria**
  - [x] Snapshot score breakdown khớp 100% với tổng điểm nghi vấn.
  - [x] Mặc định `activationAgeDays` là 3 ngày, validation [0, 7].
  - [x] Trọng số $35 + 45 + 20 = 100$.
  - [x] `ScanAnomalyDetectionServiceImpl` đọc ngưỡng từ `AnomalyThresholdService`.
  - [x] Nhãn giao diện hiển thị động theo `effectiveThreshold`.
  - [x] `estimateImpact()` mô phỏng đúng công thức tổng điểm $\ge 50$.
