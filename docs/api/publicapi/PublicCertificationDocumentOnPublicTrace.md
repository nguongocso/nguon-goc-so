📄 API Docs – Xem tệp chứng nhận trên trang tra cứu công khai

Epic: NCL-09 – Quản trị danh mục, chứng nhận và thành viên nâng cao

User Story: NCL-09-CN-006 (mở rộng)

Quy tắc: QTN-13 – Chỉ gắn và hiển thị chứng nhận còn hiệu lực

Phụ thuộc: NCL-06-CN-001 (Trang tra cứu công khai), NCL-09-CN-004 (Gắn tiêu chuẩn và chứng nhận cho lô sản xuất), NCL-09-CN-006 (Hiển thị chứng nhận trên trang tra cứu công khai)

## 1. Thông tin chung

### Mục tiêu

Cho phép Người tiêu dùng tra cứu (VT-06) xem **hình ảnh chứng nhận đã được gán** cho lô sản xuất, thay vì chỉ đọc tên, mã số và ngày hạn. Đây là bước mở rộng của NCL-09-CN-006: trước đó trang công khai không hiển thị tệp chứng nhận.

### Yêu cầu nghiệp vụ

- Tệp chứng nhận do VT-02 gắn khi tạo chứng nhận (bắt buộc nhận `application/pdf`, `image/jpeg`, `image/png`).
- Trang tra cứu công khai hiển thị ảnh `image/jpeg` / `image/png` ngay trong thẻ chứng nhận; bấm vào ảnh để phóng to.
- Với tệp `application/pdf`, hiển thị liên kết mở tệp ở tab mới.
- Chứng nhận chưa có tệp đính kèm: không hiển thị vùng tệp (không báo lỗi).
- API là public API: không yêu cầu đăng nhập, không phân quyền theo VT/tổ chức (QTN-01 không áp dụng).

### Ràng buộc bảo mật

- **Không trả `document_storage_path`** trong response danh sách. Chỉ trả metadata tệp và URL tương đối tới endpoint công khai.
- Chỉ cấp tệp khi chứng nhận **thực sự được gắn cho lô** của mã tem đang tra cứu **và** chưa ở trạng thái `REJECTED`. Nhờ vậy không thể dò/xem tệp chứng nhận thuộc lô khác bằng cách thay ID trên URL.
- Áp dụng lại kiểm tra an toàn đường dẫn lưu trữ của `CertificationServiceImpl` (chống path traversal, chỉ nhận 3 MIME ở trên).
- Trả header `X-Content-Type-Options: nosniff` và `Content-Disposition: inline`.

## 2. Vị trí làm việc tại cây thư mục Backend

| Thay đổi | File |
|---|---|
| Thêm trường tài liệu vào DTO | `publicapi/dto/response/PublicCertificationResponse.java` |
| Mở rộng service tra cứu công khai | `publicapi/service/PublicTraceService.java`, `publicapi/service/impl/PublicTraceServiceImpl.java` |
| Thêm endpoint tải tệp | `publicapi/controller/PublicTraceController.java` |
| Tái sử dụng logic đọc tệp | `certification/service/CertificationService.java`, `certification/service/impl/CertificationServiceImpl.java` |

Không tạo package mới.

## 3. Cơ sở dữ liệu

**Không cần migration.** Đọc sẵn các cột `certifications.document_file_name`, `document_content_type`, `document_file_size`, `document_storage_path` (đã có từ `V20260909111500__add_verification_fields_to_certifications.sql`).

## 4. API Endpoint

### 4.1. Danh sách chứng nhận (mở rộng response)

Method: GET
Endpoint: `/api/v1/public/trace/{codeValue}/certifications`
Quyền: Public

Response 200 OK – `certifications[]` bổ sung 5 trường:

| Trường | Kiểu | Mô tả |
|---|---|---|
| `hasDocument` | boolean | Chứng nhận có tệp đính kèm hay không |
| `documentFileName` | string \| null | Tên tệp gốc, chỉ dùng hiển thị |
| `documentContentType` | string \| null | `image/jpeg` \| `image/png` \| `application/pdf` |
| `documentFileSize` | long \| null | Dung lượng (byte) |
| `documentUrl` | string \| null | Đường dẫn tương đối tới endpoint 4.2; `null` khi không có tệp |

Các trường tài liệu có giá trị `null` khi `hasDocument = false` (do `@JsonInclude(NON_NULL)` nên các trường này bị lược bỏ khỏi JSON).

Ví dụ (rút gọn):

```json
{
  "data": {
    "productionLotId": "…",
    "lotName": "Lô xoài xuất khẩu Cát Chu",
    "hasCertification": true,
    "certifications": [
      {
        "certificationId": "0b8f…",
        "certificationName": "GlobalGAP",
        "certificationCode": "GLOBALGAP-001",
        "issuedBy": "SGS Vietnam",
        "issueDate": "2026-01-01",
        "expiryDate": "2027-01-01",
        "status": "VALID",
        "statusLabel": "Đã đạt chuẩn",
        "hasDocument": true,
        "documentFileName": "globalgap.png",
        "documentContentType": "image/png",
        "documentFileSize": 204800,
        "documentUrl": "/api/v1/public/trace/L9/certifications/0b8f…/document"
      }
    ]
  }
}
```

### 4.2. Tải tệp tài liệu chứng nhận

Method: GET
Endpoint: `/api/v1/public/trace/{codeValue}/certifications/{certificationId}/document`
Quyền: Public – không yêu cầu đăng nhận

Response 200 OK – nội dung tệp, `Content-Type` theo `document_content_type`, header `Content-Disposition: inline; filename*=UTF-8''<tên tệp đã mã hoá>`.

Mã lỗi:

| Mã | Trường hợp |
|---|---|
| 404 | Mã tem không tồn tại, hoặc chứng nhận không thuộc lô của mã tem, hoặc chứng nhận chưa có tệp đính kèm, hoặc chứng nhận đã bị từ chối |
| 410 | Tệp vật lý không còn trên máy chủ hoặc không đọc được |
| 409 | Metadata tệp không hợp lệ (MIME ngoài danh sách cho phép, hoặc đường dẫn lưu trữ nằm ngoài vùng cho phép) |

## 5. Business Rules

- Không thay đổi quy tắc QTN-13 về hiển thị chứng nhận: vẫn trả về đầy đủ chứng nhận còn hiệu lực lẫn đã hết hạn, kèm `statusLabel` tương ứng. Tệp tài liệu đi kèm theo từng chứng nhận, bao gồm chứng nhận đã hết hạn.
- Tệp được cấp bất kể `verificationStatus` là `PENDING` hay `VERIFIED`, miễn không phải `REJECTED` — khớp với bộ lọc của endpoint danh sách.
- Không ghi `activity_logs` cho thao tác xem tệp công khai (đọc, ẩn danh, khối lượng lớn).
- Không thêm thao tác gắn/gỡ/sửa chứng nhận trên trang công khai.

## 6. Ghi chú Frontend

- Component `frontend/src/components/public/PublicCertificationsSection.tsx` render thêm vùng tệp chứng nhận.
- URL tệp đi qua `getAssetUrl()` của `frontend/src/config/runtimeConfig.ts` để hợp lệ ở cả dev (Vite proxy) và production (nginx).
- Ảnh dùng `loading="lazy"` và `onError` để hiện thông báo "Không tải được ảnh chứng nhận" thay vì biểu tượng ảnh vỡ.
- Ảnh lỗi không chặn hiển thị phần thông tin chữ của chứng nhận.
- Chuỗi hiển thị đa ngôn ngữ nằm trong `frontend/src/i18n/translations.ts` (`cert_document_view`, `cert_document_open`, `cert_document_preview`, `cert_document_load_error`, `cert_document_absent`).

## 7. Test Case

| TC | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-01 | Chứng nhận có tệp PNG | `hasDocument=true`, `documentUrl` trỏ đúng endpoint 4.2, FE hiện `<img>` |
| TC-02 | Chứng nhận có tệp PDF | FE hiện liên kết mở tab mới, không render `<img>` |
| TC-03 | Chứng nhận không có tệp | `hasDocument=false`, `documentUrl=null`, FE không hiện vùng tệp |
| TC-04 | Chứng nhận thuộc lô khác, hỏi URL với ID khác | 404, không trả tệp |
| TC-05 | Chứng nhận bị từ chối (`REJECTED`) | 404 |
| TC-06 | Ảnh trả về lỗi HTTP | FE hiện thông báo không tải được ảnh, vẫn hiện thông tin chữ |

## 8. Mapping Test ↔ Xử lý

| TC | Lớp xử lý | Test |
|---|---|---|
| TC-01, TC-02, TC-03 | `PublicTraceServiceImpl.getPublicCertifications` | `PublicTraceServiceImplTest#publicCertificationsShouldExposeDocumentMetadataAndPublicUrl` |
| TC-04, TC-05 | `PublicTraceServiceImpl.getPublicCertificationDocument` | `PublicTraceServiceImplTest#getPublicCertificationDocument_WhenCertNotAttachedToLot_ShouldThrow404`, `#getPublicCertificationDocument_WhenCertRejected_ShouldThrow404` |
| TC-04 (đường hợp hợp lệ) | `PublicTraceServiceImpl` + `CertificationServiceImpl` | `PublicTraceServiceImplTest#getPublicCertificationDocument_WhenCertAttachedToLot_ShouldDelegateToCertificationService` |
| TC-01, TC-02, TC-06 | `PublicCertificationsSection` | `TraceLookupPage.test.tsx` – nhóm "TraceLookupPage hiển thị tệp chứng nhận" |
