# Bộ Tiêu Chí Checklist Review Code Frontend (ReactJS + TypeScript)

Tài liệu này được số hóa và chuẩn hóa từ `docs/Checklist_Review_Code_fe(1).xlsx`. Đây là bộ tiêu chuẩn chính thức được **AI Agent** và các **Reviewer** sử dụng để đánh giá chất lượng mã nguồn trên mỗi Pull Request Frontend của dự án **Nguồn Gốc Số**.

---

## 1. Phân Loại Mức Độ Vi Phạm

* 🔴 **Blocker (Bắt buộc sửa)**: Lỗi nghiêm trọng ảnh hưởng đến bảo mật, rò rỉ bộ nhớ, vỡ giao diện trên mobile, lạm dụng kiểu `any`, `console.log` sót lại, hoặc vi phạm Rules of Hooks. **AI Agent sẽ tự động từ chối (Request Changes) và KHÓA MERGE**.
* 🟡 **Major (Cần sửa)**: Vi phạm chuẩn Clean Code, kích thước component > 200 dòng, thiếu JSDoc súc tích, code cũ bị comment lại, thiếu alt hình ảnh (A11y), hoặc style inline tràn lan.
* 🟢 **Minor (Góp ý / Tối ưu)**: Các điểm về ngắt dòng props JSX, khoảng cách dòng trống, tối ưu CSS variables hoặc gợi ý cải tiến nhỏ.

---

## 2. Bảng Danh Sách 54 Tiêu Chí Đánh Giá Frontend

### Nhóm 1: Quy Tắc Đặt Tên (Naming Convention)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-01` | Tên file HTML/CSS/Asset viết thường, dùng dấu gạch ngang (kebab-case), không khoảng trắng. | 🟡 Major | Đổi `CategoryForm.css` thành `category-threshold-form.css`. |
| `FE-CHK-02` | Tên class CSS đặt theo quy ước rõ ràng (BEM hoặc kebab-case), mô tả đúng chức năng hiển thị. | 🟡 Major | Đổi `.btn1`, `.box` thành `.category-form__submit-button`. |
| `FE-CHK-03` | Tên id chỉ dùng khi thực sự cần thiết (anchor, JS hook), không lạm dụng id để style CSS. | 🟡 Major | Chuyển style dựa trên `#id` sang `.class`. |
| `FE-CHK-04` | Tên biến/hàm JavaScript viết theo camelCase, có ý nghĩa, không viết tắt khó hiểu. | 🟡 Major | Tránh `data`, `tmp`, `fn`; đổi thành `thresholdConfig`, `calculateDistance`. |
| `FE-CHK-05` | Tên hằng số viết hoa toàn bộ (UPPER_SNAKE_CASE), các từ phân cách bằng dấu `_`. | 🟡 Major | Đổi `pageSize` thành `DEFAULT_PAGE_SIZE`. |
| `FE-CHK-06` | Tên Component/Page viết theo PascalCase (VD: `CategoryThresholdForm.tsx`). | 🟡 Major | Đổi `categoryThresholdForm.tsx` thành `CategoryThresholdForm.tsx`. |

---

### Nhóm 2: Cấu Trúc HTML & Cú Pháp JSX (HTML Semantics & JSX)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-07` | Khai báo đầy đủ `<!DOCTYPE html>`, `<html lang="vi">`, `<meta charset="UTF-8">` trong template gốc. | 🔴 Blocker | Kiểm tra file `index.html` gốc của ứng dụng. |
| `FE-CHK-08` | Có thẻ `<meta name="viewport">` để hỗ trợ hiển thị responsive mượt mà trên mobile. | 🔴 Blocker | Bổ sung thẻ viewport chuẩn trong `index.html`. |
| `FE-CHK-09` | Dùng đúng thẻ ngữ nghĩa (`header`, `nav`, `main`, `section`, `footer`) thay vì lạm dụng toàn bộ thẻ `<div>`. | 🟡 Major | Thay thế các thẻ `div` bọc trang bằng thẻ ngữ nghĩa tương ứng. |
| `FE-CHK-10` | Thụt lề nhất quán 2 khoảng trắng (Spaces), không trộn lẫn Tab và Space. | 🟢 Minor | Cấu hình Prettier thụt lề 2 spaces cho `.ts`, `.tsx`, `.css`. |
| `FE-CHK-11` | Không còn thẻ HTML/JSX dư thừa, cấu trúc bọc vô nghĩa, hoặc comment JSX cũ chưa xóa. | 🟡 Major | Xóa bỏ các thẻ rỗng hoặc chuyển sang React Fragment `<> ... </>`. |
| `FE-CHK-12` | Thứ tự JSX Props: Khi component có ≥ 3 props hoặc dài > 80 ký tự, mỗi prop trên 1 dòng riêng. | 🟢 Minor | Ngắt dòng mỗi prop riêng biệt, dấu `/>` nằm ở dòng mới thẳng hàng thẻ mở. |

---

### Nhóm 3: Quy Tắc Viết CSS & Styling

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-13` | Không lạm dụng `!important`, chỉ dùng khi ghi đè CSS thư viện bên thứ 3 bất khả kháng. | 🔴 Blocker | Tăng độ ưu tiên của selector thay vì ép bằng `!important`. |
| `FE-CHK-14` | Tránh style inline (`style={{ ... }}`) trực tiếp trên thẻ JSX, trừ trường hợp giá trị động từ JS. | 🟡 Major | Chuyển style tĩnh vào file CSS chuyên biệt hoặc CSS Modules. |
| `FE-CHK-15` | CSS được tổ chức theo file/module rõ ràng, không dồn toàn bộ style vào 1 file khổng lồ duy nhất. | 🟡 Major | Tách file CSS theo từng component tương ứng. |
| `FE-CHK-16` | Không có class/selector trùng lặp hoặc định nghĩa chồng chéo mâu thuẫn. | 🟢 Minor | Tối ưu hóa file CSS, loại bỏ các rule trùng lặp. |
| `FE-CHK-17` | Sử dụng đơn vị linh hoạt (`rem`, `%`, `flex`, `grid`) thay vì fix cứng `px` cho layout. | 🟡 Major | Tránh đặt `width: 800px` cứng; đổi sang responsive style. |
| `FE-CHK-18` | Màu sắc, khoảng cách sử dụng biến CSS variables hệ thống (`var(--color-primary)`). | 🟢 Minor | Thay mã màu hex hardcode bằng biến CSS chung của design system. |

---

### Nhóm 4: Quy Tắc Viết TypeScript & JavaScript

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-19` | Không tạo biến toàn cục (global variables) trong code client. | 🔴 Blocker | Chuyển vào state React, context hoặc module scope. |
| `FE-CHK-20` | Dùng `const` hoặc `let`, tuyệt đối không sử dụng `var`. | 🔴 Blocker | Thay toàn bộ khai báo `var` bằng `const` (mặc định) hoặc `let`. |
| `FE-CHK-21` | Tuyệt đối **không còn `console.log()` hoặc `debugger`** trong code khi tạo PR. | 🔴 Blocker | Xóa bỏ toàn bộ lệnh debug; có thể dùng logging service nếu cần theo dõi. |
| `FE-CHK-22` | Tuân thủ TypeScript Strict Mode: **Cấm sử dụng kiểu `any`**. | 🔴 Blocker | Khai báo interface/type cụ thể cho mọi biến, props và dữ liệu API. |
| `FE-CHK-23` | Có xử lý lỗi (`try/catch`) khi gọi API, hiển thị thông báo lỗi thân thiện, không nuốt lỗi im lặng. | 🔴 Blocker | Hiển thị notification lỗi cho người dùng và tắt trạng thái loading. |
| `FE-CHK-24` | Event listener và timer (`setInterval`, `setTimeout`) phải được cleanup khi unmount component. | 🔴 Blocker | Thêm hàm cleanup trong `useEffect` (`return () => clearInterval(timer)`). |

---

### Nhóm 5: Responsive & Giao Diện Đa Thiết Bị

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-25` | Giao diện hiển thị đúng trên các breakpoint chính (Mobile < 768px, Tablet, Desktop). | 🔴 Blocker | Kiểm tra media query hoặc hệ thống Grid của UI Framework. |
| `FE-CHK-26` | Tuyệt đối không bị vỡ layout hoặc tràn ngang màn hình (`overflow-x`) trên mobile. | 🔴 Blocker | Rà soát các phần tử có chiều rộng cố định gây vỡ trang trên thiết bị nhỏ. |
| `FE-CHK-27` | Hình ảnh và video co giãn đúng tỷ lệ (`object-fit: cover/contain`), không bị méo hoặc tràn khung. | 🟡 Major | Thêm thuộc tính `max-width: 100%; height: auto;`. |
| `FE-CHK-28` | Kích thước font chữ và khoảng cách bấm (touch target ≥ 44px) rõ ràng, dễ thao tác trên mobile. | 🟡 Major | Đảm bảo nút bấm và chữ không bị quá nhỏ trên màn hình cảm ứng. |

---

### Nhóm 6: Khả Năng Truy Cập (Accessibility - A11y)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-29` | Mọi thẻ hình ảnh `<img>` bắt buộc phải có thuộc tính `alt` mô tả nội dung có nghĩa. | 🟡 Major | Bổ sung `alt="Mô tả hình ảnh"` hoặc `alt=""` nếu là ảnh trang trí. |
| `FE-CHK-30` | Các trường nhập liệu form đều có `<label>` tương ứng hoặc `aria-label`. | 🟡 Major | Gắn `htmlFor` kết nối label với `id` của input hoặc bọc trong Form.Item. |
| `FE-CHK-31` | Người dùng có thể điều hướng tương tác bằng bàn phím (phím Tab, Enter, Esc). | 🟡 Major | Không loại bỏ outline mặc định mà không cung cấp focus style thay thế. |
| `FE-CHK-32` | Độ tương phản màu chữ và màu nền đạt tiêu chuẩn dễ đọc (WCAG AA). | 🟡 Major | Đảm bảo chữ trên nền màu không bị chìm hoặc khó đọc. |

---

### Nhóm 7: Hiệu Năng & Tối Ưu Tài Nguyên (Performance)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-33` | Hình ảnh được nén dung lượng (WebP, SVG) trước khi đưa lên dự án. | 🟡 Major | Nén ảnh tránh commit file PNG/JPG dung lượng nhiều MB làm chậm load. |
| `FE-CHK-34` | Không load các thư viện hoặc icon không sử dụng (Tree-shaking). | 🟡 Major | Import cụ thể từng icon/component thay vì import toàn bộ package. |
| `FE-CHK-35` | Áp dụng lazy-loading (`React.lazy` & `Suspense`) cho các trang/màn hình route lớn. | 🟡 Major | Cấu hình code-splitting cho router để tải trang theo nhu cầu. |
| `FE-CHK-36` | Tránh render lại không cần thiết (re-render lặp vô hạn do thiếu dependency trong `useEffect`). | 🔴 Blocker | Kiểm tra kỹ mảng dependency của `useEffect` và `useCallback`. |

---

### Nhóm 8: SEO Cơ Bản

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-37` | Mỗi trang có tiêu đề `<title>` rõ ràng và thẻ mô tả `<meta name="description">` phù hợp. | 🟢 Minor | Cập nhật tiêu đề trang động theo từng màn hình nghiệp vụ. |
| `FE-CHK-38` | Cấu trúc tiêu đề heading (`h1` -> `h6`) đúng thứ tự logic, mỗi trang chỉ có một thẻ `h1`. | 🟢 Minor | Đảm bảo không nhảy cóc từ `h1` thẳng xuống `h4`. |

---

### Nhóm 9: Bảo Mật Phía Frontend (Frontend Security)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-39` | Tuyệt đối **không hardcode API key, secret, credentials** trong mã nguồn client. | 🔴 Blocker | Sử dụng biến môi trường `VITE_*` và chỉ chứa các key công khai. |
| `FE-CHK-40` | Phòng chống XSS: Dữ liệu người dùng nhập phải được sanitize/escape trước khi render. | 🔴 Blocker | Tránh sử dụng `dangerouslySetInnerHTML` khi chưa lọc sạch dữ liệu. |
| `FE-CHK-41` | Không lưu thông tin bảo mật nhạy cảm (mật khẩu, quyền quản trị) trong localStorage. | 🔴 Blocker | Lưu phiên đăng nhập an toàn bằng HTTP-Only Cookie hoặc access token ngắn hạn. |

---

### Nhóm 10: Khả Năng Tương Thích Trình Duyệt (Cross-Browser)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-42` | Giao diện hiển thị nhất quán trên các trình duyệt phổ biến (Chrome, Edge, Firefox, Safari). | 🟡 Major | Kiểm tra trên ít nhất 2 trình duyệt để đảm bảo không lệch giao diện. |
| `FE-CHK-43` | Không dùng thuộc tính CSS/JS quá mới mà chưa có fallback hoặc polyfill tương thích. | 🟢 Minor | Kiểm tra tính tương thích qua CanIUse trước khi áp dụng API mới. |

---

### Nhóm 11: Clean Commenting & JSDoc (Súc Tích & Sạch Mã)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-44` | Component chính và Custom Hook có JSDoc súc tích bằng tiếng Việt có dấu (**tối đa 2 - 4 dòng**). | 🟡 Major | Tóm tắt mục đích UI/Nghiệp vụ + Story ID; không viết lan man. |
| `FE-CHK-45` | Tuyệt đối **không có Noise Comment** (comment giải thích cú pháp JSX/JS hiển nhiên). | 🟡 Major | Xóa các comment thừa kiểu `// nút bấm`, `// state loading`. |
| `FE-CHK-46` | Tuyệt đối **không còn Dead Code** (JSX cũ hoặc hàm cũ bị comment lại) trong PR. | 🔴 Blocker | Xóa bỏ hoàn toàn code JSX cũ; Git đã lưu trữ toàn bộ lịch sử. |
| `FE-CHK-47` | Không còn `// TODO:` hoặc `// FIXME:` sót lại khi merge vào `develop`/`main`. | 🔴 Blocker | Giải quyết dứt điểm các ghi chú tạm bợ trước khi mở PR. |

---

### Nhóm 12: Quy Tắc Chung & Chất Lượng Mã Nguồn (Code Quality)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-48` | Kích thước component gọn gàng: **≤ 200 dòng/file**. Tách sub-components nếu vượt quá. | 🟡 Major | Tách các phần modal, form hoặc bảng phức tạp thành component con riêng. |
| `FE-CHK-49` | Không lặp lại mã nguồn (DRY) - logic gọi API và tiện ích dùng chung đã được tách vào `services/`/`utils/`. | 🟡 Major | Đưa các hàm format ngày tháng, tiền tệ vào thư viện utils dùng chung. |
| `FE-CHK-50` | Đặt tên file, thư mục nhất quán trong toàn bộ dự án (kebab-case cho style, PascalCase cho component). | 🟢 Minor | Chuẩn hóa cấu trúc thư mục module. |

---

### Nhóm 13: Kiểm Tra Trước Khi Merge & Deploy (Pre-Merge Gate)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `FE-CHK-51` | Đã kiểm tra tất cả các nút bấm, link điều hướng hoạt động trơn tru, không có link chết (404). | 🔴 Blocker | Click test toàn bộ các luồng thao tác trên màn hình vừa phát triển. |
| `FE-CHK-52` | Đã kiểm tra Console trình duyệt (F12) không còn bất kỳ lỗi đỏ (Error) hoặc warning nghiêm trọng nào. | 🔴 Blocker | Mở Developer Tools kiểm tra console log hoàn toàn sạch sẽ. |
| `FE-CHK-53` | Chạy lệnh build kiểm tra thành công (`npm run build` hoặc `tsc --noEmit`), không có lỗi type. | 🔴 Blocker | Chạy lệnh build TypeScript tại local để đảm bảo không lỗi kiểu dữ liệu. |
| `FE-CHK-54` | Đã được AI Agent review đạt 100% tiêu chí Blocker & Major trước khi merge. | 🔴 Blocker | Khắc phục triệt để các phản hồi của AI Reviewer trên PR. |
