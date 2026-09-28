# Ma trận Truy vết Kiểm thử Toàn diện — User Story NCL-10-CN-015

> **Mã User Story:** NCL-10-CN-015  
> **Tên Story:** Kiểm thử hồi quy đầu cuối và cập nhật tài liệu vận hành  
> **Phiên bản:** 1.0.0 (Cập nhật sau Vòng 4 xác minh chuyên sâu)  
> **Ngày phê duyệt:** 12/09/2026  
> **Người thực hiện:** Đội ngũ Phát triển Nguồn Gốc Số  

---

## 1. Ma trận Truy vết Yêu cầu & Kết quả Thực nghiệm (Traceability Matrix)

| Mã Tiêu chí (TC) | Quy tắc / Story liên quan | Yêu cầu kiểm thử chi tiết | Endpoint / Component kiểm chứng | Tài khoản & Vai trò kiểm thử | Kết quả Vòng 1-3 | Kết quả Vòng 4 (Xác minh sâu) | Trạng thái cuối cùng |
|---|---|---|---|---|:---:|:---:|:---:|
| **NCL-10-CN-015-TC-01** | QTN-01..31, NCL-05-CN-009, NCL-08-CN-012 | Kịch bản 20 bước hồi quy đầu cuối từ khởi tạo tổ chức đến kết thúc vụ việc thu hồi và phản ánh chất lượng | 20 endpoints backend liên hoàn (`/admin/organizations`, `/farm-lots`, `/farm-logs`, `/chain-events/*`, `/labels/export`, `/handovers/*`, `/recall-cases/*`, `/public/*`) | `admin` (VT-01), `htxbaoloc015mgr` (VT-02), `recorder015baoloc` (VT-03), `procurement` (VT-04), Người tiêu dùng công khai | PASS ✅ | Không yêu cầu chạy lại | **PASS** ✅ |
| **NCL-10-CN-015-TC-02** | QTN-19 | Tính toàn vẹn chuỗi băm sự kiện (Event Hash Chaining) trên lô hàng có cả Handover và Recall | `GET /api/v1/shipments/{id}/verify-chain` | `quanly_htx` (VT-02), `admin` (VT-01) | PASS ✅ | Không yêu cầu chạy lại | **PASS** ✅ |
| **NCL-10-CN-015-TC-03a** | QTN-01 | Từ chối truy cập chéo tổ chức (Multi-tenancy Access Denial) trên 7 API nghiệp vụ | `/handovers/{id}/accept`, `/recall-cases/{id}`, `/trace/impact-scope`, `/production-lots/chain-progress` | `cm_tc` (HTX Tân Cương) vs `procurement` (DEMO_NSV) vs `orgmanager` (HTX Xanh) | PASS ✅ | Không yêu cầu chạy lại | **PASS** ✅ |
| **NCL-10-CN-015-TC-03b** | Security Audit | Ghi nhật ký truy cập trái phép (Audit Log) khi bị từ chối 403/400 nghiệp vụ chéo tổ chức | `activity_logs`, `GlobalExceptionHandler.java`, `SecurityConfig.java` | Toàn bộ các phiên vi phạm | FAIL ❌ | Rà soát `SecurityConfig`: Không có custom `AccessDeniedHandler`/`AuthenticationEntryPoint`. Ngoại lệ 403 rơi vào `GlobalExceptionHandler` và bị bỏ qua do lọc URL. | **FAIL** ❌ *(Khoảng trống kỹ thuật)* |
| **NCL-10-CN-015-TC-04** | CV-05 / Operations | Cập nhật tài liệu kỹ thuật, vận hành, dữ liệu demo và kịch bản trình diễn | `docs/handover/OPERATIONS.md`, `USER_GUIDE.md`, `DEMO_DATA.md`, `TEST_EVIDENCE.md` | Nhóm tài liệu chuyển giao | PASS ✅ | Không yêu cầu chạy lại | **PASS** ✅ |
| **MỤC A (UI Live Test)** | QTN-22, QTN-31 | Ràng buộc giao diện: Bên gửi ẩn nút nhận/từ chối; Bên nhận hiển thị đầy đủ và bấm thao tác gửi request | `http://localhost:3000/handover/{id}` | Case 1: `cm_tc` (Bên gửi)<br>Case 2: `procurement` (Bên nhận) | PASS ✅ *(Nghi vấn avatar)* | Giải mã JWT và `user_info` xác nhận danh tính `cm_tc` (`Hien La Van`), giải thích avatar `Van`, chụp ảnh DOM đồng bộ với người vận chuyển khác. | **PASS** ✅ |
| **MỤC C (QTN-06)** | QTN-06, NCL-09-CN-011 | Chặn đóng gói khi thiếu mốc canh tác bắt buộc cho loại cây "Chè" và đóng gói thành công khi đủ mốc | `POST /chain-events/packaging`, `POST /farm-logs` | `recorder015baoloc` (VT-03), `htxbaoloc015mgr` (VT-02) | PASS ✅ | Không yêu cầu chạy lại | **PASS** ✅ |

---

## 2. Chi tiết Kết quả Xác minh Vòng 4

### 2.1 Điểm 1: Xác minh Danh tính Tài khoản MỤC A (Case 1)
- **Tài khoản kiểm tra:** `cm_tc` (Mật khẩu: `admin123`).
- **Dữ liệu danh tính trích xuất từ Runtime trình duyệt Edge CDP:**
  + `localStorage.getItem('user_info')`:
    ```json
    {
      "userId": "64a82d0e-2615-4359-83c2-27460c99eba2",
      "username": "cm_tc",
      "fullName": "Hien La Van",
      "roleCode": "VT-02",
      "roleName": "ORG_MANAGER",
      "organizationId": "47bcceae-7d31-4ad4-b1da-828290041515",
      "organizationName": "Hợp tác xã chè Tân Cương"
    }
    ```
  + **Giải mã JWT Access Token:** `sub = "cm_tc"`, `roles = "VT-02"`, `organizationId = "47bcceae-7d31-4ad4-b1da-828290041515"`.
  + **Nguồn gốc chữ "Van" trên Avatar Header:** Quy định tại `Header.tsx` (dòng 122) lấy từ cuối cùng của `fullName`: `"Hien La Van".split(' ').pop()` $\rightarrow$ `"Van"`.
  + **Phiếu bàn giao đối chứng mới (`b7e95ec1-a2ef-406c-8d93-f3b60f646619`):** Ghi rõ người vận chuyển là *"Nguyen Thi Giao Hang"*, loại bỏ hoàn toàn sự trùng lặp ngẫu nhiên với tên avatar.
  + **Kết quả DOM:** `hasAcceptButtonInDOM: false`, `hasRejectButtonInDOM: false`, `hasActionCardInDOM: false`.
  + **Bằng chứng hình ảnh:** `docs/presentation/evidence_ui_sender_cm_tc_verified.png`.

### 2.2 Điểm 2: Kiểm tra `SecurityConfig` và `AccessDeniedHandler`
- **Rà soát `SecurityConfig.java`:**
  + Hoàn toàn **không có** cấu hình `.exceptionHandling(...)`.
  + **Không có** bean `AccessDeniedHandler` hoặc `AuthenticationEntryPoint` tùy chỉnh trong toàn bộ backend.
  + Spring Security sử dụng cơ chế mặc định: khi vi phạm quyền tại `@PreAuthorize`, ngoại lệ `AccessDeniedException` được chuyển thẳng tới `@ExceptionHandler(AccessDeniedException.class)` trong `GlobalExceptionHandler.java`.
  + Tại `GlobalExceptionHandler.java:226`, phương thức `publishAccessDeniedAudit` chỉ ghi log khi `uri.startsWith("/api/v1/admin/monitoring")`.
  + **Kết luận kiểm toán:** Không có cơ chế logging nào khác bị bỏ sót. Kết luận **FAIL** đối với việc ghi log truy cập trái phép chéo tổ chức là tuyệt đối chính xác và khách quan.
