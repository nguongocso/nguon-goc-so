# Kế hoạch triển khai refactor giao diện Nguồn Gốc Số

## 1. Mục tiêu

Refactor giao diện theo hướng hiện đại, đáng tin cậy và nhất quán với đặc thù
agri-tech, đồng thời giữ nguyên hợp đồng API, nghiệp vụ, phân quyền, trạng thái dữ
liệu và các route đang được sử dụng.

Kế hoạch sử dụng `docs/AI_DESIGN_SYSTEM.md` làm nguồn chuẩn nội bộ và tham khảo
chọn lọc cách tổ chức nội dung công khai của Trace Việt tại
`https://traceviet.mae.gov.vn/`. Không sao chép nhận diện thương hiệu, hình ảnh,
nội dung hoặc cấu trúc độc quyền của hệ thống tham khảo.

## 2. Phạm vi

### 2.1. Trong phạm vi

- Design token, typography, spacing, radius, shadow và trạng thái màu.
- Shared UI primitives và common components.
- Application shell: `MainLayout`, Header, Sidebar, breadcrumb và vùng nội dung.
- Trang công khai, xác thực và tra cứu nguồn gốc.
- Các archetype: dashboard, list, form, detail, workflow và report.
- Responsive cho mobile, tablet và desktop.
- Accessibility, loading, empty, error và permission state.
- Visual regression và runtime validation trên các màn hình đại diện.

### 2.2. Ngoài phạm vi mặc định

- Thay đổi API, database, entity hoặc business rule.
- Đổi URL/route hoặc mã quyền hiện có.
- Thay đổi cơ chế đăng nhập, chọn tổ chức hoặc xử lý token.
- Viết lại toàn bộ frontend trong một nhánh hoặc một pull request.
- Thêm UI framework mới.
- Sao chép nguyên giao diện hoặc tài sản hình ảnh của website tham khảo.

Nếu trong quá trình refactor phát hiện lỗi nghiệp vụ, lỗi đó phải được tách thành
User Story hoặc bug fix riêng và đi đủ lifecycle của dự án.

## 3. Baseline hiện tại

Kết quả khảo sát repository tại thời điểm lập kế hoạch:

| Hạng mục | Giá trị quan sát |
|---|---:|
| Route khai báo | 148 |
| File page TSX | 182 |
| File component TSX | 269 |
| File test TypeScript/TSX | 77 |
| File dùng màu Tailwind trực tiếp | 297 |
| File TypeScript/TSX trên 200 dòng | 202 |
| Page dùng `ListPageHeader` | 36 |
| Page dùng `ListCard` | 28 |
| Page dùng `ListToolbar` | 26 |
| Page dùng `DataTableShell` | 27 |

Các vấn đề staging cần xử lý sớm:

- Trợ lý AI che CTA quét QR trên mobile.
- Tác vụ tra cứu trên trang chủ bị đẩy xuống sau các card giới thiệu.
- Header/logo trang công khai chiếm nhiều không gian trên màn hình nhỏ.
- Trang đăng nhập không có tiêu đề và label hiển thị; card nhỏ trên desktop và
  sử dụng nhiều giá trị style riêng.
- Màu raw, gradient, blur, radius và shadow được áp dụng không đồng đều.
- Một số dialog/action dùng màu không đúng ngữ nghĩa.
- Nhiều page quá lớn, khó bảo trì và khó giữ giao diện nhất quán.

## 4. Bài học chọn lọc từ Trace Việt

### 4.1. Nên áp dụng

- Hero dùng ảnh nông nghiệp thật để tạo bối cảnh và độ tin cậy.
- Tra cứu mã là tác vụ chính và xuất hiện ngay trong vùng nhìn đầu tiên.
- Tiêu đề ngắn, tương phản cao và thông điệp giá trị rõ ràng.
- Chia nội dung công khai thành các section có mục tiêu riêng.
- Tách rõ trải nghiệm người tiêu dùng và khu vực quản trị.
- Nội dung pháp lý có cấu trúc heading rõ ràng.

### 4.2. Không nên sao chép

- Trang giới thiệu/pháp lý quá dài mà thiếu mục lục sticky và điều hướng nhanh.
- Section dùng ảnh lazy-load có thể để lại vùng trống lớn trên mobile.
- Khối card quá cao và nhiều nội dung marketing trước tác vụ chính.
- Tài liệu kỹ thuật dạng bảng dài chưa tối ưu cho màn hình nhỏ.
- Dùng ảnh nền hoặc chi tiết trang trí làm giảm độ rõ của chữ.

## 5. Visual direction

Tên định hướng: **Agri-tech đáng tin cậy**.

- Xanh lá đậm là màu thương hiệu và hành động chính.
- Nền ứng dụng nội bộ dùng màu trung tính; không dùng gradient trên toàn layout.
- Trang công khai được phép giàu hình ảnh hơn khu vực nghiệp vụ.
- Màu chỉ mang ý nghĩa thương hiệu hoặc trạng thái, không dùng để trang trí ngẫu nhiên.
- Ưu tiên whitespace, typography và phân cấp nội dung thay cho shadow lớn.
- Card nội bộ dùng border nhẹ, radius và shadow chuẩn của design system.
- CTA chính có một kiểu thống nhất; destructive action sử dụng danger semantics.
- Geist và Lucide tiếp tục là font/icon chuẩn.

## 6. Chiến lược triển khai

Không triển khai theo kiểu big-bang. Mỗi wave phải có màn hình mẫu, validation và
khả năng merge độc lập. Shared component được hoàn thiện trước, sau đó migrate
theo archetype và domain.

### Wave 0 — Inventory và visual baseline

**Mục tiêu:** có dữ liệu so sánh trước/sau và giới hạn phạm vi.

1. Lập ma trận route theo public, auth, dashboard, list, form, detail, workflow,
   report và mobile/offline.
2. Gắn owner/domain và vai trò được phép cho từng route.
3. Chọn golden screens:
   - Trang chủ công khai.
   - Đăng nhập và chọn tổ chức.
   - Dashboard.
   - Danh sách tổ chức.
   - Danh sách lô sản xuất.
   - Tạo/chỉnh sửa vùng trồng.
   - Chi tiết lô sản xuất công khai.
   - Nhật ký canh tác.
   - Tạo lô hàng.
   - Kiểm nghiệm/chứng nhận.
   - Thu hồi.
   - Báo cáo.
4. Chụp baseline tại 390, 768, 1024, 1280 và 1440 px.
5. Ghi nhận overflow, lỗi console, network error, focus order, touch target và
   các trạng thái loading/empty/error.

**Đầu ra:** route inventory, screenshot baseline và danh sách regression risk.

### Wave 1 — Token và shared primitives

**Mục tiêu:** mọi wave sau dùng chung một ngôn ngữ giao diện.

File trọng tâm:

- `frontend/src/index.css`
- `frontend/src/components/ui/*`
- `frontend/src/components/common/*`

Công việc:

1. Rà soát token semantic hiện có; chỉ bổ sung token khi thật sự thiếu vai trò.
2. Chuẩn hóa Button, Input, Select, Textarea, Card, Dialog, Alert, Badge, Table,
   Tabs, Tooltip và Toast.
3. Chuẩn hóa focus ring, disabled state, icon size và touch target.
4. Hoàn thiện các composition chuẩn:
   - `ListPageHeader`
   - `ListToolbar`
   - `ListCard`
   - `SearchInput`
   - `FilterSelect`
   - `DataTableShell`
   - `StatusBadge`
   - `DetailSection` và `DetailField`
   - `FormSection` và `PageActions`
5. Tạo component gallery chỉ dành cho development nếu việc review primitive qua
   các golden screen chưa đủ.
6. Không thực hiện thay màu cơ học toàn repository trong một commit.

**Gate:** primitive đạt keyboard/focus, responsive và unit test trước khi migrate page.

### Wave 2 — Application shell

**Mục tiêu:** tạo khung nhất quán cho toàn bộ màn hình sau đăng nhập.

File trọng tâm:

- `frontend/src/components/layout/MainLayout.tsx`
- `frontend/src/components/layout/Header.tsx`
- `frontend/src/components/layout/Sidebar.tsx`
- `frontend/src/components/common/AppBreadcrumb.tsx`
- `frontend/src/components/ai/AiChatWidget.tsx`

Công việc:

1. Thay nền gradient của workspace bằng token background trung tính.
2. Giữ sidebar 272 px trên desktop; chuẩn hóa drawer tablet/mobile.
3. Rút gọn Header, cân lại account, organization switcher, alert và notification.
4. Chuẩn hóa nhóm menu theo tác vụ và vai trò; giữ nguyên route và quyền.
5. Loại bỏ dialog đăng xuất bị lặp và dùng destructive semantics đúng chuẩn.
6. Bảo đảm breadcrumb không hiển thị UUID kỹ thuật.
7. Thêm safe area cho AI widget và quy tắc tránh CTA, pagination, sheet, dialog.
8. Kiểm tra Sidebar bằng keyboard, Escape, focus trap và screen reader name.

**Gate:** shell hoạt động với mọi role đại diện ở cả năm breakpoint.

### Wave 3 — Public, auth và trace lookup

**Mục tiêu:** đưa tác vụ truy xuất lên trước, tăng độ tin cậy thương hiệu.

File trọng tâm:

- `frontend/src/pages/public/PublicHomePage.tsx`
- `frontend/src/pages/public/TraceLookupPage.tsx`
- `frontend/src/pages/public/shipment/*`
- `frontend/src/pages/auth/*`
- `frontend/src/components/auth/*`
- `frontend/src/components/layout/PublicBackground.tsx`

Công việc:

1. Thiết kế lại hero dựa trên ảnh nông nghiệp có quyền sử dụng hợp lệ.
2. Đặt nhập mã và quét QR trong first viewport trên desktop/mobile.
3. Trên mobile, đặt tra cứu trước các feature card.
4. Giảm kích thước logo/header và tránh shadow/blur quá mạnh.
5. Làm rõ trạng thái camera permission, loading, mã không hợp lệ và rate limit.
6. Trang đăng nhập có heading, mô tả và label hiển thị; không dùng placeholder
   thay label.
7. Đồng bộ Forgot Password, Reset Password và Organization Selection.
8. Thiết kế kết quả truy xuất theo thứ tự:
   - Nhận diện sản phẩm và trạng thái xác thực.
   - Tổ chức/vùng trồng chịu trách nhiệm.
   - Chứng nhận và cảnh báo.
   - Timeline từ sản xuất đến vận chuyển.
   - Tài liệu/hình ảnh liên quan.
   - Tuyên bố trách nhiệm dữ liệu.
9. Ảnh public phải có kích thước dự phòng, placeholder và fallback để tránh layout shift.

**Gate:** người dùng có thể bắt đầu tra cứu trong first viewport và không có control
bị AI widget che ở 390 px.

### Wave 4 — Dashboard và list pages

**Mục tiêu:** chuẩn hóa nhóm màn hình được sử dụng thường xuyên nhất.

Thứ tự domain:

1. Tổ chức và thành viên.
2. Vùng trồng và lô sản xuất.
3. Nhật ký canh tác và sự kiện chuỗi.
4. Lô hàng, bàn giao và mã truy xuất.
5. Chứng nhận, kiểm nghiệm và thu hồi.
6. Cảnh báo, báo cáo và quản trị hệ thống.

Mỗi list page phải có:

- Page header và description ngắn.
- Primary action duy nhất, tuân theo role access.
- Search/filter/refresh chuẩn.
- Loading, empty, error và pagination.
- Không quá ba action hiển thị trực tiếp trên một row.
- Table card hoặc horizontal scroll có chủ đích trên mobile.
- Status có text label, không truyền đạt bằng màu đơn thuần.

**Gate:** hoàn thành từng domain độc lập; không chờ migrate toàn bộ 148 route.

### Wave 5 — Form, detail và workflow

**Mục tiêu:** giảm tải nhận thức cho các luồng nghiệp vụ dài.

1. Form chia theo section nghiệp vụ, một cột trên mobile và hai cột khi phù hợp.
2. Form dài dùng sticky action bar hoặc step khi nghiệp vụ thực sự có nhiều giai đoạn.
3. Required, helper, validation và server error có cách trình bày thống nhất.
4. Detail page dùng `DetailSection`, `DetailField`, timeline và status mapping dùng chung.
5. Destructive workflow mô tả rõ hậu quả và chống double submit.
6. Tách các page lớn theo section/component/hook; không thay business logic cùng lúc
   nếu không bắt buộc.
7. Kiểm tra đặc biệt map, QR scanner, upload, offline queue và dữ liệu thời gian thực.

**Gate:** create/edit/detail của một thực thể phải dùng cùng vocabulary và hierarchy.

### Wave 6 — Report, portal và technical content

**Mục tiêu:** làm dữ liệu dày dễ đọc và dễ điều hướng.

1. Report ưu tiên summary trước chart/table chi tiết.
2. Chart phải có title, legend, đơn vị, empty state và mô tả thay thế.
3. Bảng lớn có sticky header, column priority và export action rõ ràng.
4. Portal/API docs có mục lục sticky, anchor, tìm kiếm và copy feedback.
5. Nội dung pháp lý dài có mục lục, back-to-top và width đọc phù hợp.
6. Code block và bảng kỹ thuật có horizontal scroll trên mobile.

## 7. Kế hoạch kiểm thử

### 7.1. Tự động

- Unit/component test cho shared primitives và các trạng thái page.
- Test role access và route guard hiện có phải tiếp tục pass.
- Bổ sung visual smoke test cho golden screens nếu công cụ được phê duyệt.
- Kiểm tra overflow ngang ở các breakpoint chuẩn.

Các lệnh bắt buộc sau mỗi PR frontend:

```bash
cd frontend
npm run lint
npm run test
npm run build
```

### 7.2. Runtime/UI

- Chạy frontend thật, kiểm tra terminal, console và network.
- Kiểm tra dữ liệu thật/seed cho loading, empty, success và failure.
- Kiểm tra keyboard-only, focus visible, Escape và focus return.
- Kiểm tra tên accessible cho icon-only button.
- Kiểm tra camera denied/unavailable, geolocation denied và scanner cleanup.
- Kiểm tra dark-token compatibility dù chưa cần user-facing toggle.

Áp dụng giới hạn tối đa năm lần sửa/chạy lại cho cùng một lỗi blocking theo
`AGENTS.md`.

## 8. Acceptance Criteria cấp chương trình

| ID | Tiêu chí |
|---|---|
| UI-AC-01 | Không thay đổi API contract, route, permission hoặc business rule ngoài phạm vi được duyệt. |
| UI-AC-02 | Golden screens không overflow ngang tại 390, 768, 1024, 1280 và 1440 px. |
| UI-AC-03 | Tác vụ tra cứu/quét mã xuất hiện trong first viewport của trang chủ. |
| UI-AC-04 | AI widget không che control hoặc nội dung quan trọng. |
| UI-AC-05 | Form field nghiệp vụ có label, lỗi và focus state rõ ràng. |
| UI-AC-06 | Mỗi list page có loading, empty, error và pagination phù hợp. |
| UI-AC-07 | Mọi status có nhãn chữ và mapping màu semantic thống nhất. |
| UI-AC-08 | Các thao tác destructive có mô tả hậu quả, nút cụ thể và chống submit lặp. |
| UI-AC-09 | Không thêm `any`, framework UI hoặc raw color mới khi token/component đã đáp ứng. |
| UI-AC-10 | Lint, test và production build pass; runtime không có lỗi console/network liên quan. |
| UI-AC-11 | Các role đại diện chỉ thấy và thực hiện đúng action được cấp quyền. |
| UI-AC-12 | File mới/sửa dùng UTF-8; JavaDoc/comment mới tuân thủ quy tắc tiếng Việt của dự án. |

## 9. Rủi ro và biện pháp giảm thiểu

| Rủi ro | Biện pháp |
|---|---|
| Refactor rộng làm hỏng nghiệp vụ | Migrate theo domain, giữ API và state logic, test golden flow. |
| Shared component thay đổi gây regression diện rộng | Hoàn thiện variant mới trước, migrate có kiểm soát, không thay cơ học toàn repo. |
| Sai quyền khi sắp xếp lại menu/action | Dùng `ROLE_ACCESS` hiện có và test theo role đại diện. |
| Mobile table/form khó sử dụng | Thiết kế column priority, card view hoặc scroll có chủ đích. |
| Ảnh hero làm chậm tải | Dùng WebP/AVIF, responsive source, kích thước dự phòng và preload có chọn lọc. |
| Page lớn khó refactor | Tách presentation trước, giữ service/state behavior, sau đó mới tối ưu cấu trúc. |
| PR quá lớn, khó review | Một PR cho foundation/shell hoặc một domain nhỏ; giới hạn thay đổi liên quan. |

## 10. Tổ chức User Story và pull request

Đề xuất epic: `UI Modernization 2026`.

Nhóm User Story:

1. UI-FND: token và primitives.
2. UI-SHELL: application shell.
3. UI-PUBLIC: home, auth và public trace.
4. UI-LIST: dashboard và list archetype.
5. UI-FORM: form/detail/workflow archetype.
6. UI-DOMAIN-* : migrate từng domain.
7. UI-REPORT: report, portal và technical content.
8. UI-QA: accessibility và visual regression.

Mỗi User Story phải chỉ rõ route, role, states, breakpoint và Acceptance Criteria;
đồng thời tuân thủ lifecycle trong `docs/agent/`.

## 11. Mốc nghiệm thu đề xuất

| Mốc | Nội dung | Ước lượng |
|---|---|---:|
| M1 | Baseline, visual direction, token và primitives | 1 tuần |
| M2 | Shell, home, auth và trace lookup | 1–1.5 tuần |
| M3 | Dashboard và ba domain ưu tiên | 1.5–2 tuần |
| M4 | Các domain còn lại và workflow phức tạp | 2–3 tuần |
| M5 | Report, portal, accessibility và regression | 1 tuần |

Ước lượng tổng: 6–8 tuần với một frontend developer; khoảng 4–5 tuần với hai
frontend developer nếu chia domain độc lập và vẫn giữ một người chịu trách nhiệm
duyệt design system.

## 12. Definition of Done cho từng wave

- Acceptance Criteria được đối chiếu bằng bảng PASS/FAIL.
- Không có thay đổi backend/API ngoài phạm vi được phê duyệt.
- Runtime desktop/mobile đã được kiểm tra bằng dữ liệu thực tế phù hợp.
- Không còn lỗi console/network liên quan.
- Lint, test và build pass.
- Role access và route guard pass.
- Không có overflow, control bị che hoặc touch target không đạt trên golden screens.
- `git status` và `git diff` chỉ chứa thay đổi thuộc User Story hiện tại.
- Tài liệu design system được cập nhật nếu shared pattern/token mới được phê duyệt.
