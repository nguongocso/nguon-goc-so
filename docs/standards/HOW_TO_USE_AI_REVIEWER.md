# Hướng Dẫn Vận Hành & Sử Dụng AI Code Reviewer Gatekeeper

Tài liệu này hướng dẫn cách cấu hình, kích hoạt và vận hành hệ thống **AI Code Review & Clean Code Gatekeeper** tự động trên GitHub cho toàn bộ dự án **Nguồn Gốc Số**.

---

## 1. Cơ Chế Hoạt Động

```text
Developer tạo Pull Request 
   ⬇️
GitHub Action tự động kích hoạt workflow: AI Clean Code & Checklist Gatekeeper
   ⬇️
Hệ thống phân tích Git Diff:
  - Tự động nhận diện file thuộc tầng nào (Entity, DTO, Service, Controller, Component, Hook...)
  - Quét định dạng dòng: Giới hạn 120 ký tự, ngắt dòng builder/props, cách dòng trống
  - Quét độ sạch mã nguồn: Cấm console.log, System.out.println, dead code, noise comments
  - Đối chiếu 59 tiêu chí Backend (BE-CHK) hoặc 54 tiêu chí Frontend (FE-CHK)
   ⬇️
Agent xuất kết quả trực tiếp lên Pull Request:
  - NẾU ĐẠT 100%: Phê duyệt APPROVE ✅ (cho phép merge)
  - NẾU CÓ LỖI BLOCKER/MAJOR: Gửi review REQUEST_CHANGES ❌, chỉ rõ file, số dòng, mã tiêu chí vi phạm và code mẫu gợi ý sửa (sử dụng ```suggestion```)
```

---

## 2. Các Bước Cấu Hình Trên GitHub Repository (Dành cho Quản Trị Viên / Lead)

### Bước 1: Thêm API Key vào GitHub Repository Secrets
Để AI Agent có thể gọi mô hình trí tuệ nhân tạo (Google Gemini 2.0 Flash hoặc OpenAI) để phân tích diff:
1. Truy cập vào kho mã nguồn GitHub của dự án.
2. Chọn **Settings** -> **Secrets and variables** -> **Actions**.
3. Nhấn nút **New repository secret**.
4. Tạo secret:
   * **Name**: `GEMINI_API_KEY`
   * **Secret**: Dán khóa API của bạn (Lấy miễn phí tại [Google AI Studio](https://aistudio.google.com/)).
   *(Tùy chọn: Nếu dùng OpenAI, tạo secret tên `OPENAI_API_KEY`).*

> **Ghi chú**: Nếu chưa cấu hình API Key, hệ thống vẫn tự động chạy bộ lọc tĩnh dự phòng (Static Rule Checker) để bắt các lỗi nghiêm trọng như `console.log`, `System.out.println`, `@Data` trên Entity, và dòng quá 120 ký tự.

---

### Bước 2: Bật Branch Protection Rules (Khóa Nút Merge Nếu Review Thất Bại)
Để đảm bảo mã bẩn không bao giờ lọt vào nhánh `develop` hoặc `main`:
1. Vào **Settings** -> **Branches**.
2. Tại mục **Branch protection rules**, nhấn **Add branch ruleset** hoặc **Add rule** cho nhánh `develop` và `main`.
3. Tích chọn các mục sau:
   * ✅ **Require a pull request before merging**.
   * ✅ **Require status checks to pass before merging**:
     * Tìm kiếm và chọn check: `AI Clean Code & Checklist Gatekeeper`.
   * ✅ **Require review from Pull Request**: Yêu cầu có ít nhất 1 phê duyệt (Approve).
4. Nhấn **Save changes**.

Từ nay, nếu PR có lỗi vi phạm Blocker hoặc Major, workflow sẽ trả về trạng thái thất bại (Check Failed) và GitHub sẽ **tự động vô hiệu hóa (khóa) nút Merge** cho đến khi developer sửa xong lỗi.

---

## 3. Quy Trình Làm Việc Hàng Ngày Của Lập Trình Viên (Developer Workflow)

### Bước 1: Trước Khi Tạo Pull Request
1. Mở file [docs/standards/code-convention-be.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/docs/standards/code-convention-be.md) (nếu làm backend) hoặc [docs/standards/code-convention-fe.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/docs/standards/code-convention-fe.md) (nếu làm frontend) để đối chiếu quy chuẩn theo từng tầng.
2. Kiểm tra lại:
   * Không có dòng nào vượt quá **120 ký tự**.
   * DTO có đầy đủ message tiếng Việt có dấu.
   * Xóa bỏ toàn bộ `System.out.println()` và `console.log()`.
   * Xóa các dòng code cũ bị comment lại (Dead code).
   * Javadoc ngắn gọn súc tích (≤ 3 - 5 dòng).

### Bước 2: Tạo Pull Request
* Khi tạo PR, GitHub sẽ tự động nạp mẫu [pull_request_template.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/.github/pull_request_template.md).
* Tích chọn các mục trong bảng **Self-Review Checklist**.

### Bước 3: Đọc Kết Quả Phản Hồi Từ AI Agent
* Sau khoảng 15-30 giây từ khi mở PR hoặc push commit mới, bot **AI Clean Code Gatekeeper** sẽ gửi bảng đánh giá.
* Nếu có gợi ý sửa (`suggestion`), bạn chỉ cần bấm nút **Commit suggestion** ngay trên giao diện GitHub để áp dụng nhanh code sửa.
* Sau khi push commit sửa lỗi, bot sẽ tự động chạy lại. Khi đạt 100% tiêu chí, bot sẽ chuyển sang **APPROVE ✅** và cho phép merge.

---

## 4. Danh Mục Tài Liệu Tham Chiếu

| Tài liệu | Mô tả |
|---|---|
| [code-convention-be.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/docs/standards/code-convention-be.md) | Quy ước Backend Java 21 / Spring Boot 3 chi tiết theo từng tầng (Entity, DTO, Mapper, Service, Controller). |
| [code-convention-fe.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/docs/standards/code-convention-fe.md) | Quy ước Frontend ReactJS / TypeScript chi tiết theo Component, Hook, Service, Styles. |
| [checklist-review-be.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/docs/standards/checklist-review-be.md) | Bảng 59 tiêu chí review Backend (`BE-CHK-01` đến `BE-CHK-59`). |
| [checklist-review-fe.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/docs/standards/checklist-review-fe.md) | Bảng 54 tiêu chí review Frontend (`FE-CHK-01` đến `FE-CHK-54`). |
| [.github/copilot-instructions.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/.github/copilot-instructions.md) | Chỉ thị hệ thống cho GitHub Copilot / Agent tab. |
| [.github/pull_request_template.md](file:///run/media/tran-phuong-doan/New%20Volume/file/CodeGym/Project/nguon-goc-so/.github/pull_request_template.md) | Mẫu PR tự kiểm tra chuẩn Clean Code. |
