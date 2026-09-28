# Bộ Tiêu Chí Checklist Review Code Backend (Java 21 / Spring Boot 3)

Tài liệu này được số hóa và chuẩn hóa từ `docs/Checklist_Review_Code_be(1).xlsx`. Đây là bộ tiêu chuẩn chính thức được **AI Agent** và các **Reviewer** sử dụng để đánh giá chất lượng mã nguồn trên mỗi Pull Request Backend của dự án **Nguồn Gốc Số**.

---

## 1. Phân Loại Mức Độ Vi Phạm

* 🔴 **Blocker (Bắt buộc sửa)**: Lỗi nghiêm trọng ảnh hưởng đến an ninh, toàn vẹn dữ liệu, hiệu năng hệ thống hoặc vi phạm nguyên tắc kiến trúc cốt lõi. **AI Agent sẽ tự động từ chối (Request Changes) và KHÓA MERGE**.
* 🟡 **Major (Cần sửa)**: Vi phạm chuẩn Clean Code, chuẩn đặt tên, thiếu validation, phương thức quá dài hoặc comment rác làm bẩn mã nguồn. Bắt buộc phải giải quyết trước khi hoàn tất PR.
* 🟢 **Minor (Góp ý / Tối ưu)**: Các điểm về thẩm mỹ, ngắt dòng, khoảng trắng hoặc gợi ý tối ưu nhỏ không ảnh hưởng lớn đến vận hành.

---

## 2. Bảng Danh Sách 59 Tiêu Chí Đánh Giá Backend

### Nhóm 1: Quy Tắc Đặt Tên (Naming Convention)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-01` | Tên package viết thường toàn bộ, là danh từ ngắn gọn, không khoảng trắng/gạch ngang/ký tự đặc biệt. | 🟡 Major | Đổi tên package thành chữ thường (vd: `vn.nguongocso.alert.service`). |
| `BE-CHK-02` | Tên package phản ánh đúng chức năng kiến trúc (không đặt tên chung chung: `common`, `misc`, `helper`). | 🟡 Major | Chia tách vào đúng module nghiệp vụ (`alert`, `trace`, `auth`) hoặc tầng rõ ràng. |
| `BE-CHK-03` | Tên class là danh từ, viết theo PascalCase, thể hiện đúng chức năng và vai trò. | 🟡 Major | Đặt tên theo mẫu: `[ChứcNăng][Layer]` (vd: `CategoryThresholdOverrideService`). |
| `BE-CHK-04` | Tên interface là danh từ/tính từ, PascalCase, **không có tiền tố "I"**. | 🟡 Major | Sửa `IUserService` thành `UserService`, `IPaymentStrategy` thành `PaymentStrategy`. |
| `BE-CHK-05` | Tên method là động từ/cụm động từ, camelCase, mô tả rõ hành động. | 🟡 Major | Tránh tên mơ hồ như `check()`, `doTask()`; đổi thành `calculateTotal()`, `findUserById()`. |
| `BE-CHK-06` | Tên biến camelCase, ngắn gọn nhưng có ý nghĩa, không viết tắt khó hiểu. | 🟡 Major | Tránh `data`, `temp`, `list`, `obj`, `stName`; đổi thành `studentList`, `currentUser`. |
| `BE-CHK-07` | Tên hằng số viết hoa toàn bộ (UPPER_SNAKE_CASE), phân cách bằng dấu gạch dưới `_`. | 🟡 Major | Đổi `maxScanPerHour` thành `MAX_SCAN_PER_HOUR`. |
| `BE-CHK-08` | Tên enum dùng PascalCase; các giá trị enum viết hoa toàn bộ phân cách bằng `_`. | 🟡 Major | Đổi `enum alert_severity { Warning }` thành `enum AlertSeverity { WARNING }`. |

---

### Nhóm 2: Định Dạng Mã Nguồn & Xuống Dòng (Formatting & Line Wrapping)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-09` | Thụt lề dùng 4 khoảng trắng (Spaces), tuyệt đối không dùng Tab. | 🟢 Minor | Cấu hình IDE dùng 4 spaces, chạy reformat code. |
| `BE-CHK-10` | Mỗi dòng tối đa **120 ký tự**, vượt quá phải xuống dòng hợp lý. | 🟢 Minor | Ngắt dòng trước toán tử hoặc sau dấu phẩy để dòng không bị tràn ngang. |
| `BE-CHK-11` | Dấu mở ngoặc `{` đặt cùng dòng khai báo, dấu đóng ngoặc `}` đặt riêng dòng thẳng hàng. | 🟢 Minor | Tuân thủ chuẩn Java K&R style: `if (...) {` trên cùng một dòng. |
| `BE-CHK-12` | Có khoảng trắng sau dấu phẩy, sau từ khóa `if`/`for`/`while`/`switch`, và hai bên toán tử. | 🟢 Minor | Sửa `if(a==b)` thành `if (a == b)`. |
| `BE-CHK-13` | Mỗi dòng chỉ chứa một câu lệnh đơn lẻ. | 🟡 Major | Tách `int a = 1; int b = 2;` thành 2 dòng riêng biệt. |
| `BE-CHK-14` | Luôn sử dụng dấu ngoặc `{}` kể cả khi khối lệnh chỉ có một dòng duy nhất. | 🟡 Major | Thêm `{}` vào câu lệnh `if (condition) return true;`. |
| `BE-CHK-15` | Đã thực hiện Format Code toàn bộ file trước khi commit/tạo PR. | 🟢 Minor | Chạy auto-formatter của IDE (Ctrl+Alt+L / Cmd+Option+L). |
| `BE-CHK-16` | Xuống dòng đúng chuẩn cho Builder / Fluent API / Stream: xuống dòng TRƯỚC dấu `.`. | 🟢 Minor | Đặt mỗi method call trên 1 dòng mới bắt đầu bằng dấu chấm `.`. |
| `BE-CHK-17` | Cách dòng trống đúng chuẩn: 1 dòng trống giữa các methods; 1 dòng trống giữa các fields có annotation. Cấm 2 dòng trống liên tiếp. | 🟢 Minor | Xóa các dòng trống thừa dồn dập và thêm dòng trống giữa các field DTO có validation. |

---

### Nhóm 3: Quy Tắc Import (Import Hygiene)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-18` | Chỉ import đúng class thực sự cần sử dụng. | 🟢 Minor | Chạy Optimize Imports trong IDE để xóa các import thừa. |
| `BE-CHK-19` | Tuyệt đối **không sử dụng ký tự đại diện `*`** khi import (No wildcard imports). | 🟡 Major | Thay `import java.util.*;` thành `import java.util.List; import java.util.Map;`. |
| `BE-CHK-20` | Đã xóa bỏ toàn bộ các import không sử dụng (Unused imports). | 🟢 Minor | Dọn sạch các import bị cảnh báo xám trong IDE trước khi mở PR. |

---

### Nhóm 4: Quy Tắc Viết Class & Kiến Trúc Tầng (Class & Architecture)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-21` | Mỗi class chỉ đảm nhiệm một trách nhiệm chính duy nhất (Single Responsibility). | 🔴 Blocker | Tách các class đa nhiệm thành các service/component chuyên biệt. |
| `BE-CHK-22` | Kích thước class không quá lớn: khuyến nghị ≤ 300 dòng, tối đa không quá 500 dòng. | 🟡 Major | Tách nhỏ logic nghiệp vụ nếu class vượt quá 500 dòng. |
| `BE-CHK-23` | Thuộc tính trong class phải được khai báo `private` (Encapsulation). | 🔴 Blocker | Đổi phạm vi thuộc tính từ `public`/`protected` thành `private` và dùng getter/setter. |
| `BE-CHK-24` | Không dùng `@Data` trên JPA Entity; Entity không chứa logic tính toán nghiệp vụ. | 🔴 Blocker | Đổi `@Data` thành `@Getter @Setter @NoArgsConstructor` trên Entity. |
| `BE-CHK-25` | Tuyệt đối không trả Entity trực tiếp ra Controller/API; bắt buộc dùng DTO. | 🔴 Blocker | Sử dụng MapStruct để map Entity sang Response DTO trước khi trả về. |
| `BE-CHK-26` | DTO Request bắt buộc có Jakarta Validation (`@NotNull`, `@Min`,...) kèm `message` tiếng Việt. | 🔴 Blocker | Bổ sung annotation validation và thông điệp lỗi tiếng Việt thân thiện. |

---

### Nhóm 5: Quy Tắc Viết Phương Thức (Method Rules)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-27` | Mỗi method chỉ thực hiện một chức năng duy nhất. | 🟡 Major | Tách hàm làm nhiều việc (validate + save + send email) thành các hàm con. |
| `BE-CHK-28` | Độ dài method **không vượt quá 30 dòng**. Nếu dài hơn bắt buộc phải tách nhỏ. | 🟡 Major | Tách bớt logic ra private helper methods hoặc service phụ trợ. |
| `BE-CHK-29` | Không truyền quá 3 tham số vào một method (nếu nhiều hơn phải gom vào Parameter Object / DTO). | 🟡 Major | Tạo đối tượng Request DTO để gom các tham số lại. |

---

### Nhóm 6: Biến & Hằng Số (Variables & Constants)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-30` | Biến được khai báo gần nơi sử dụng nhất, phạm vi (scope) nhỏ nhất có thể. | 🟢 Minor | Di chuyển khai báo biến vào sát vị trí bắt đầu dùng nó. |
| `BE-CHK-31` | Biến được khởi tạo giá trị trước khi sử dụng, tránh NullPointerException. | 🟡 Major | Gán giá trị mặc định hoặc kiểm tra `Optional` an toàn. |
| `BE-CHK-32` | Không sử dụng Magic Number hoặc Magic String trong code. | 🟡 Major | Khai báo thành hằng số `public static final` có tên rõ nghĩa. |

---

### Nhóm 7: Clean Commenting & Javadoc (Súc Tích & Sạch Mã)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-33` | Class / Interface chính có Javadoc súc tích bằng tiếng Việt có dấu (**tối đa 3 - 5 dòng**). | 🟡 Major | Viết 1 đoạn tóm tắt nghiệp vụ + User Story ID; tránh viết lan man. |
| `BE-CHK-34` | Method nghiệp vụ chính có Javadoc súc tích (**tối đa 3 - 5 dòng**). Cấm Javadoc cho getter/setter. | 🟡 Major | Nêu mục đích hàm + `@param`, `@return`, `@throws` ngắn gọn. Xóa javadoc thừa của getter. |
| `BE-CHK-35` | Tuyệt đối **không có Noise Comment** (comment giải thích cú pháp hiển nhiên). | 🟡 Major | Xóa các comment thừa kiểu `// tăng i`, `// kiểm tra null`, `// gọi database`. |
| `BE-CHK-36` | Tuyệt đối **không còn Dead Code** (mã cũ bị comment lại) trong PR. | 🔴 Blocker | Xóa sạch toàn bộ các dòng code cũ bị comment; Git đã lưu lịch sử. |
| `BE-CHK-37` | Không còn `// TODO` hoặc `// FIXME` sót lại khi merge vào `develop`/`main`. | 🔴 Blocker | Giải quyết dứt điểm TODO hoặc tạo issue theo dõi riêng trên Jira/GitHub. |

---

### Nhóm 8: Xử Lý Ngoại Lệ (Exception Handling)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-38` | Bắt đúng loại Exception cụ thể cần xử lý, không lạm dụng `catch (Exception e)`. | 🔴 Blocker | Bắt ngoại lệ cụ thể (`IOException`, `EntityNotFoundException`). |
| `BE-CHK-39` | Tuyệt đối **không có khối catch rỗng (empty catch block)** nuốt lỗi im lặng. | 🔴 Blocker | Phải ghi log lỗi bằng `log.error()` hoặc throw custom `BusinessException`. |
| `BE-CHK-40` | Không trả stack trace chi tiết ra phía người dùng client. | 🔴 Blocker | Bọc lỗi qua `@RestControllerAdvice` trả về cấu trúc `ApiResponse.error()`. |

---

### Nhóm 9: Ghi Log (Logging Rules)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-41` | Tuyệt đối **không sử dụng `System.out.println()`** hoặc `printStackTrace()` trong code. | 🔴 Blocker | Thay bằng SLF4J logger: `@Slf4j` và `log.info()`, `log.error()`. |
| `BE-CHK-42` | Sử dụng đúng cấp độ log (Log Levels: `debug`, `info`, `warn`, `error`). | 🟡 Major | Dùng `error` khi có ngoại lệ, `warn` khi dữ liệu bất thường, `info` cho sự kiện nghiệp vụ. |

---

### Nhóm 10: Tổ Chức Package & Cấu Trúc Dự Án

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-43` | Cấu trúc package phản ánh đúng kiến trúc (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`). | 🟡 Major | Sắp xếp file vào đúng package chức năng. |
| `BE-CHK-44` | Không đặt tên package chung chung vô nghĩa (`common`, `misc`, `helper`). | 🟡 Major | Đặt tên gắn liền với module domain (vd: `vn.nguongocso.alert.util`). |

---

### Nhóm 11: Nguyên Tắc OOP & Thiết Kế Hướng Đối Tượng

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-45` | Tuân thủ nguyên tắc đóng gói (Encapsulation). | 🔴 Blocker | Khai báo thuộc tính `private`, truy cập qua getter/setter. |
| `BE-CHK-46` | Ưu tiên lập trình theo Interface thay vì cài đặt cụ thể (Dependency Inversion). | 🟡 Major | Tiêm dependency qua Interface (`private final UserService userService;`). |
| `BE-CHK-47` | Hạn chế sử dụng `static` tùy tiện khi không phải Utility method hoặc Constant. | 🟡 Major | Chuyển thành Spring Bean (`@Component` / `@Service`). |
| `BE-CHK-48` | Giảm sự phụ thuộc chặt chẽ giữa các lớp (Loose Coupling). | 🟡 Major | Sử dụng Constructor Injection với `@RequiredArgsConstructor`. |

---

### Nhóm 12: Bảo Mật & An Ninh Mã Nguồn (Security)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-49` | Tuyệt đối không hardcode mật khẩu, token, secret key trong mã nguồn. | 🔴 Blocker | Đưa cấu hình vào biến môi trường `.env` hoặc `application.yml`. |
| `BE-CHK-50` | Phòng chống SQL Injection: Không nối chuỗi SQL, bắt buộc dùng `@Param` hoặc JPQL. | 🔴 Blocker | Sử dụng parameterized query của Spring Data JPA. |
| `BE-CHK-51` | Phân quyền API rõ ràng bằng `@PreAuthorize("hasRole(...)")` tại Controller. | 🔴 Blocker | Thêm annotation bảo vệ API tránh truy cập trái phép. |

---

### Nhóm 13: Hiệu Năng Cơ Sở Dữ Liệu (Performance)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-52` | Các truy vấn danh sách bản ghi bắt buộc có phân trang (`Pageable`). | 🔴 Blocker | Thêm tham số `Pageable` và trả về `Page<T>`, cấm `findAll()` không giới hạn. |
| `BE-CHK-53` | Thiết lập `@Transactional(readOnly = true)` cho các thao tác chỉ đọc dữ liệu. | 🟡 Major | Gắn `@Transactional(readOnly = true)` ở mức class Service để tối ưu bộ nhớ JPA. |
| `BE-CHK-54` | Tránh lỗi N+1 Query khi truy vấn quan hệ JPA (Entity Graph / JOIN FETCH). | 🔴 Blocker | Sử dụng `@EntityGraph` hoặc query `JOIN FETCH` khi cần tải dữ liệu liên kết. |

---

### Nhóm 14: Kiểm Tra Trước Khi Merge & Deploy (Pre-Merge Gate)

| Mã ID | Tiêu chí kiểm tra | Mức độ | Hướng dẫn khắc phục / Tiêu chuẩn đạt |
|---|---|---|---|
| `BE-CHK-55` | Mã nguồn biên dịch thành công (`mvn clean compile`), không có lỗi build. | 🔴 Blocker | Chạy lệnh build kiểm tra tại local trước khi commit. |
| `BE-CHK-56` | Toàn bộ Unit Test và Integration Test liên quan đều pass 100%. | 🔴 Blocker | Chạy `mvn test` đảm bảo không có test nào bị fail. |
| `BE-CHK-57` | Mã nguồn không có cảnh báo nghiêm trọng (Linter / Checkstyle). | 🟡 Major | Khắc phục các cảnh báo vàng trước khi gửi PR. |
| `BE-CHK-58` | Đã cập nhật tài liệu API / Swagger nếu có thay đổi contract. | 🟡 Major | Bổ sung chú thích `@Operation` trên Controller. |
| `BE-CHK-59` | Đã được AI Agent review đạt 100% tiêu chí Blocker & Major trước khi merge. | 🔴 Blocker | Không còn vi phạm nào ở mức Blocker; sửa các góp ý của Agent. |
