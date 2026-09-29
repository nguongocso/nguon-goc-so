# Kịch bản Demo & Thuyết trình — Nguồn Gốc Số (Thời lượng: ~7 phút)

> **Mục tiêu:** Kịch bản demo **deterministic** và trực quan hóa toàn bộ chuỗi giá trị nông nghiệp số: từ trải nghiệm minh bạch của người tiêu dùng, trợ lý giọng nói AI ngoài cánh đồng, bảo mật chuỗi băm mật mã học SHA-256, đến quy trình bàn giao số và thu hồi nguyên tắc 4 mắt.

---

## 📋 CHUẨN BỊ TRƯỚC KHI BẤM NÚT QUAY (Mở sẵn 5 Tab)

* **Tab 1:** Trang chủ tra cứu công khai: `http://localhost:3000/` (chuẩn bị sẵn mã tem đã kích hoạt: `DEMO-NHO-01`).
* **Tab 2:** Đăng nhập sẵn tài khoản Quản lý HTX: `orgmanager` / `admin123` (chọn tổ chức `HTX Nông Sản Demo`).
* **Tab 3:** Đăng nhập sẵn tài khoản Doanh nghiệp thu mua: `procurement` / `admin123` (chọn `Công ty Nông Sản Việt Demo`).
* **Tab 4:** Mở sẵn trang Kiểm chứng dòng sự kiện: `http://localhost:3000/chain-events/verification` (hoặc `/event-chain-verification`).
* **Tab 5:** Đăng nhập sẵn tài khoản Cán bộ Sở Nông nghiệp: `regulator` / `admin123` (chọn `Chi cục Quản lý Chất lượng Nông Sản`).

---

## 🎬 KỊCH BẢN CHI TIẾT: THAO TÁC MÀN HÌNH & LỜI THOẠI THUYẾT TRÌNH

---

### ⏱️ PHÂN ĐOẠN 1: CÚ HOOK TỪ NGƯỜI TIÊU DÙNG (00:00 – 00:45)
* **Màn hình:** Tab 1 — Trang chủ Tra cứu công khai (`http://localhost:3000/`).

* **Thao tác chuột:**
  * **(00:00 – 00:10):** Để chuột ở giữa màn hình trang chủ, paste mã tra cứu `DEMO-NHO-01` vào ô tìm kiếm (hoặc bấm quét camera giả lập), bấm nút **Tra cứu**.
  * **(00:10 – 00:25):** Cuộn chuột xuống mượt mà: di chuột qua vị trí thửa ruộng trên **Bản đồ vệ tinh GIS** (có khoanh ranh giới Polygon xanh lá), rồi lướt qua **Dòng sự kiện (Timeline)** từ ngày xuống giống, chăm sóc đến ngày đóng gói.
  * **(00:25 – 00:45):** Cuộn xuống phần **Chứng nhận & Kết quả kiểm nghiệm**: Di chuột làm nổi bật 3 chỉ tiêu: **Chì (Pb)**, **Cadmi (Cd)**, **E. coli** đều có badge xanh **ĐẠT CHUẨN** do Trung tâm Kiểm nghiệm Quốc gia cấp.

* **🎙️ Lời thuyết minh (Voice-over):**
  > "Xin kính chào quý vị và các bạn! Hãy tưởng tượng quý vị đang cầm trên tay một hộp Nho Ninh Thuận trong siêu thị. Làm thế nào để biết đây là nông sản sạch thật sự hay chỉ là hàng trôi nổi dán mác lừa đảo?
  >
  > Với nền tảng Nguồn Gốc Số, người tiêu dùng không cần cài đặt bất kỳ ứng dụng nào. Chỉ với một cú quét mã QR, toàn bộ 'hồ sơ cuộc đời' của sản phẩm hiện ra: từ thửa ruộng nơi gieo hạt, lịch trình chăm sóc, cho đến phiếu xét nghiệm an toàn thực phẩm VietGAP với đầy đủ chỉ tiêu dư lượng hóa chất đều đạt chuẩn.
  >
  > Vậy điều gì đảm bảo những dữ liệu này là trung thực và không bị làm giả? Chúng ta hãy cùng quay ngược thời gian về cánh đồng của bà con nông dân."

---

### ⏱️ PHÂN ĐOẠN 2: QUẢN LÝ SẢN XUẤT HTX, TRỢ LÝ GIỌNG NÓI AI & TÍNH NĂNG OFFLINE (00:45 – 02:00)
* **Màn hình:** Chuyển sang Tab 2 (Giao diện Quản lý HTX — tài khoản `orgmanager`).

* **Thao tác chuột:**
  * **(00:45 – 01:05):** Bấm menu **Vận hành sản xuất → Vùng trồng**. Click chọn **Vùng trồng Nho Ninh Thuận 01**, màn hình mở bản đồ vệ tinh có khoanh ranh giới tọa độ GPS chính xác từng mét vuông.
  * **(01:05 – 01:25):** Bấm menu **Tiến độ chuỗi (Kanban)**. Lướt chuột qua các cột *Chuẩn bị → Canh tác → Thu hoạch → Đóng gói*. Dừng chuột và zoom nhẹ vào thẻ **Lô Nho 02** có nhãn màu cam **"Lô tồn đọng"** (>10 ngày chưa phát sinh sự kiện mới).
  * **(01:25 – 01:45) — [ĐIỂM NHẤN AI MỚI]:** Bấm vào Lô sản xuất → chọn **Lô Nho 01** → mở tab **Nhật ký canh tác** → bấm **Thêm nhật ký canh tác** (route `/farm-logs/create`).
    * Click vào biểu tượng **Micro AI** ở đầu form.
    * Đọc giọng nói (hoặc chọn câu lệnh mẫu): *"Hôm nay bón phân hữu cơ vi sinh 20kg và tưới nước cho luống nho phía đông"*.
    * Sóng âm chuyển động sinh động, AI lập tức bóc tách tự động: *Loại hoạt động: Bón phân*, *Vật tư: Phân hữu cơ vi sinh*, *Số lượng: 20*, *Đơn vị: kg*, *Ghi chú: Luống nho phía đông* và tự động điền form trong 1 giây.
    * Trợ lý AI cất giọng đọc phản hồi xác nhận tiếng Việt (TTS).
  * **(01:45 – 02:00):** Mở tiếp menu **Sự kiện chờ đồng bộ** (`/offline-events`) để minh họa cơ chế Offline-first.

* **🎙️ Lời thuyết minh (Voice-over):**
  > "Tại Hợp tác xã, ban quản lý số hóa toàn bộ vùng canh tác bằng bản đồ GIS với ranh giới GPS chuẩn xác đến từng mét vuông. Mỗi mùa vụ sẽ được quản lý bằng một Lô sản xuất tương ứng.
  >
  > Bảng tiến độ chuỗi thông minh tự động phát hiện nút thắt cổ chai: bất kỳ lô hàng nào bị ứ đọng quá 10 ngày không có sự kiện mới sẽ bị gắn cờ cảnh báo màu cam, giúp ngăn chặn nguy cơ nông sản bị lãng quên dẫn đến hư hỏng.
  >
  > Đặc biệt, để thấu hiểu nỗi vất vả của bà con ngoài đồng ruộng 'tay lấm chân bùn', hệ thống tiên phong tích hợp **Trợ lý nhập liệu bằng Giọng nói AI**. Nông dân không cần gõ bàn phím điện thoại dưới trời nắng gắt: chỉ cần bấm Micro và nói một câu tự nhiên, AI sẽ tự động phân tích ngữ nghĩa, bóc tách công việc, tên vật tư, liều lượng và tự động điền form chuẩn VietGAP, đồng thời cất giọng đọc phản hồi xác nhận.
  >
  > Cùng với cơ chế **Offline-first**, bà con thoải mái ghi chép ngay cả khi mất sóng 4G ngoài đồng. Ngay khi có kết nối Internet, dữ liệu sẽ tự động đồng bộ tức thì lên máy chủ trung tâm."

---

### ⏱️ PHÂN ĐOẠN 3: CỔNG KIỂM NGHIỆM ĐỘC LẬP & XUẤT TEM QR (02:00 – 03:00)
* **Màn hình:** Vẫn ở Tab 2 (HTX).

* **Thao tác chuột:**
  * **(02:00 – 02:25):** Bấm menu **Quản lý → Chứng nhận** và mở chi tiết một **Yêu cầu kiểm nghiệm** của Lô Nho 01 đã đạt kết quả **PASSED** (hiển thị 3 chỉ tiêu Chì, Cadmi, E. coli).
  * **(02:25 – 02:40):** Chuyển sang chi tiết **Lô hàng (Shipment)**. Trình chiếu nút **Kích hoạt tem** và danh sách mã tem GS1 sinh tự động.
  * **(02:40 – 03:00):** Bấm nút **Xuất tem QR**. Màn hình tải/mở ra file PDF hiển thị các tem mã QR chuẩn dải mã GS1 được căn chỉnh sẵn sàng để in dán lên bao bì.

* **🎙️ Lời thuyết minh (Voice-over):**
  > "Khác với các hệ thống thông thường cho phép tự ý in tem vô tội vạ, Nguồn Gốc Số thiết lập một 'cổng kiểm soát chất lượng' độc lập.
  >
  > Một lô nông sản chỉ được phép cấp tem khi và chỉ khi đã có kết quả kiểm nghiệm đạt tiêu chuẩn an toàn từ phòng thí nghiệm được chứng nhận. Khi kiểm nghiệm đạt, Quản lý HTX mới có thể bấm kích hoạt dải mã tem truy xuất.
  >
  > Hệ thống tự động sinh mã theo chuẩn GS1 và hỗ trợ xuất file in tem hàng loạt chuẩn quy cách bao bì, sẵn sàng dán lên từng hộp nông sản trước khi xuất kho."

---

### ⏱️ PHÂN ĐOẠN 4: BÀN GIAO SỐ CHO DOANH NGHIỆP THU MUA (03:00 – 04:00)
* **Màn hình:** Chuyển sang Tab 3 (Tài khoản Doanh nghiệp thu mua — `procurement`).

* **Thao tác chuột:**
  * **(03:00 – 03:20):** Bấm menu **Thu mua → Phiếu bàn giao nhận** (`/handover`). Di chuột qua danh sách phiếu bàn giao đang ở trạng thái **Chờ xác nhận (PENDING_CONFIRMATION)**.
  * **(03:20 – 03:40):** Mở chi tiết phiếu bàn giao: Lướt chuột qua thông tin đơn vị gửi (`HTX Nông Sản Demo`), số lượng 50 thùng, tài xế và biển số xe tải chuyên dụng (`29H-123.45`).
  * **(03:40 – 04:00):** Bấm nút **Xác nhận bàn giao**. Trạng thái lập tức chuyển sang huy hiệu xanh **ĐÃ TIẾP NHẬN (ACCEPTED)**.

* **🎙️ Lời thuyết minh (Voice-over):**
  > "Khi nông sản rời cánh đồng, bài toán nhức nhối nhất là nạn 'đánh tráo hàng hóa dọc đường'. Nguồn Gốc Số giải quyết bằng quy trình Bàn giao số khép kín.
  >
  > HTX gửi lệnh bàn giao số kèm thông tin tài xế và phương tiện chuyên chở. Đơn vị thu mua khi nhận hàng tại tổng kho sẽ kiểm tra đối chiếu và bấm nút Xác nhận trên hệ thống.
  >
  > Ngay lập tức, quyền sở hữu số của lô hàng được chuyển giao minh bạch, đồng thời tự động ghi nhận mắt xích vận chuyển vào lịch sử chuỗi cung ứng. Bất kỳ hao hụt hay tráo đổi ở chặng nào đều được quy trách nhiệm pháp lý rõ ràng."

---

### ⏱️ PHÂN ĐOẠN 5: CAO TRÀO 1 — CHUỖI BĂM SHA-256 & BẮT GIAN LẬN TEM (04:00 – 05:15)
* **Màn hình:** Chuyển sang Tab 4 — Trang **Kiểm chứng dòng sự kiện** (`/chain-events/verification` hoặc `/event-chain-verification`).

* **Thao tác chuột:**
  * **(04:00 – 04:30):** Bấm nút **Kiểm chứng** (đã điền sẵn mã `DEMO-NHO-01`): Di chuột dọc theo cột Previous Hash và Hash của 4 sự kiện liên tiếp. Chỉ vào huy hiệu màu xanh lá cây **INTACT (Toàn vẹn)** và các dấu tích xanh hợp lệ.
  * **(04:30 – 04:55):** Giải thích cơ chế bảo mật: Chỉ cho người xem thấy nếu có ai đó xâm nhập cơ sở dữ liệu để sửa lén ngày thu hoạch hay sự kiện, hàm băm sẽ không khớp, hệ thống lập tức báo động đỏ **CORRUPTED**, khoanh vùng chính xác bản ghi bị can thiệp.
  * **(04:55 – 05:15):** Bấm menu **Tem nghi vấn** (`/admin/suspect-trace-codes` hoặc `/trace/suspicious-cases`). Zoom vào bản ghi tem `ANOMALY-DEMO-01`: Quét tại **Cần Thơ lúc 10h00** và quét tại **Hà Nội lúc 10h15** (vận tốc ~6000 km/h — vi phạm định luật vật lý).

* **🎙️ Lời thuyết minh (Voice-over):**
  > "Bây giờ, chúng ta đến với phần cốt lõi của công nghệ bảo mật: Điều gì ngăn chặn quản trị viên tự vào cơ sở dữ liệu để sửa lén ngày xịt thuốc bảo vệ thực vật từ hôm qua thành cách đây hai tuần?
  >
  > Hệ thống áp dụng cơ chế **Chuỗi băm mật mã học SHA-256** tương tự cấu trúc Blockchain. Mỗi sự kiện phát sinh được tính toán kèm mã băm của sự kiện liền trước. Khi hệ thống chạy kiểm chứng, nếu toàn bộ chuỗi khớp nhau, trạng thái sẽ là **INTACT — Toàn vẹn**.
  >
  > Nhưng nếu bất kỳ ai can thiệp trực tiếp vào database để chỉnh sửa dù chỉ một dấu phẩy, chuỗi băm sẽ bị gãy vụn ngay lập tức. Hệ thống sẽ bật báo động đỏ **CORRUPTED** và chỉ đích danh chính xác bản ghi nào đã bị can thiệp trái phép.
  >
  > Chưa dừng lại ở đó, thuật toán **Phát hiện quét mã bất thường** dựa trên định vị địa lý IP giúp ngăn chặn nạn photocopy tem lậu. Nếu cùng một mã tem được quét đồng thời ở hai thành phố cách nhau hàng ngàn cây số trong 15 phút, hệ thống sẽ tự động gắn cờ nghi vấn và gửi cảnh báo đỏ đến quản trị viên."

---

### ⏱️ PHÂN ĐOẠN 6: CAO TRÀO 2 — THU HỒI NGUYÊN TẮC 4 MẮT & CẢNH BÁO TỨC THỜI (05:15 – 06:15)
* **Màn hình:** Về lại Tab 2 (HTX) và chuẩn bị sẵn Tab 1 (Người tiêu dùng).

* **Thao tác chuột:**
  * **(05:15 – 05:35):** Bấm menu **Thu hồi & Cảnh báo → Truy vết phạm vi ảnh hưởng** (`/trace/impact-scope`). Nhập mã lô nguồn, bấm quét: màn hình vẽ ra toàn bộ danh sách các lô hàng, đại lý và mã tem liên đới.
  * **(05:35 – 05:55):** Mở chi tiết **Yêu cầu thu hồi** (`/recall-requests`). Nhấn mạnh quy trình phê duyệt độc lập của người quản lý thứ 2 (**Nguyên tắc 4 mắt — Four-eyes principle**). Bấm **Phê duyệt thu hồi**!
  * **(05:55 – 06:15):** Lập tức click chuột chuyển ngay về Tab 1 (Trang tra cứu của Người tiêu dùng ở Phút 0), bấm phím **F5** tải lại trang.
    * 👉 **Zoom cận cảnh màn hình:** Giao diện xanh mướt lúc đầu lập tức bị thay thế bằng Khung cảnh báo đỏ rực: **"CẢNH BÁO: LÔ HÀNG ĐANG BỊ THU HỒI, KHÔNG SỬ DỤNG!"**, kèm khuyến cáo y tế và hotline liên hệ.

* **🎙️ Lời thuyết minh (Voice-over):**
  > "Kịch bản xấu nhất: Giả sử cơ quan y tế phát hiện một mẫu nho bị nhiễm khuẩn ngoài thị trường, quy trình xử lý khủng hoảng sẽ diễn ra như thế nào?
  >
  > Hệ thống cung cấp công cụ **Truy vết phạm vi ảnh hưởng**. Chỉ cần nhập mã lô, hệ thống quét thần tốc toàn bộ mạng lưới phân phối để xác định chính xác những lô hàng nào cần thu hồi.
  >
  > Để tránh việc lạm quyền hoặc phá hoại, quy trình thu hồi bắt buộc tuân thủ **Nguyên tắc 4 mắt**: Người thứ nhất tạo đề xuất, người quản lý thứ hai có thẩm quyền độc lập mới được phê duyệt.
  >
  > Và điều kỳ diệu xảy ra ngay sau đây: Khi lệnh thu hồi vừa được duyệt, hãy nhìn vào màn hình điện thoại của người tiêu dùng tại siêu thị... [Bấm F5]. Ngay lập tức, màn hình chuyển sang màu đỏ cảnh báo khẩn cấp: Lô hàng đang bị thu hồi, kèm khuyến cáo không sử dụng và hotline liên hệ. Mối nguy hại cho sức khỏe người tiêu dùng đã được ngăn chặn kịp thời chỉ trong tích tắc!"

---

### ⏱️ PHÂN ĐOẠN 7: TRỢ LÝ AI ĐIỀU HÀNH & BỨC TRANH VĨ MÔ (06:15 – 07:00)
* **Màn hình:** Mở widget Trợ lý AI ở góc phải màn hình → Chuyển sang Tab 5 (Sở Nông nghiệp — `regulator`).

* **Thao tác chuột:**
  * **(06:15 – 06:35):** Bấm mở cửa sổ **Trợ lý AI** (AiChatWidget): Click chọn câu hỏi gợi ý nhanh: *"Tóm tắt tình hình các lô sản xuất đang canh tác và cảnh báo rủi ro"*. AI hiển thị câu trả lời phân tích số liệu gọn gàng: thống kê số lô đang canh tác, phát hiện lô tồn đọng và nhắc lịch cách ly thuốc bảo vệ thực vật.
  * **(06:35 – 06:45):** Chuyển sang Tab 5: Lướt nhanh qua **Dashboard quản lý địa bàn của Sở Nông nghiệp** (Biểu đồ phân tích vùng trồng, nút **Xuất dữ liệu mở Open Data**).
  * **(06:45 – 07:00):** Trở về trang chủ hoặc màn hình tổng quan, giữ chuột yên và kết thúc video.

* **🎙️ Lời thuyết minh (Voice-over):**
  > "Bên cạnh việc giải phóng sức lao động ngoài đồng ruộng bằng giọng nói, Nguồn Gốc Số còn đóng vai trò như một Giám đốc Kỹ thuật số 24/7. Trợ lý AI thông minh trên Dashboard có khả năng trả lời theo ngữ cảnh từng vai trò: giúp ban quản trị HTX tóm tắt tình hình mùa vụ, cảnh báo lô hàng tồn đọng nguy cơ hư hỏng, nhắc lịch cách ly thuốc bảo vệ thực vật, và giải đáp các quy chuẩn kỹ thuật xuất khẩu khắt khe.
  >
  > Ở tầm vĩ mô, các cơ quan quản lý nhà nước như Sở Nông nghiệp có thể giám sát bức tranh tổng thể theo địa bàn, phân tích năng suất mùa vụ và xuất dữ liệu mở (Open Data) để liên thông với cổng thông tin quốc gia.
  >
  > Tóm lại, Nguồn Gốc Số không chỉ là một chiếc tem QR dán lên sản phẩm. Đó là một hạ tầng số toàn diện: bảo vệ người tiêu dùng bằng sự minh bạch, bảo vệ nông dân chân chính bằng chuỗi bảo mật, và đồng hành cùng chuyển đổi số nông nghiệp Việt Nam vươn tầm thế giới. Xin chân thành cảm ơn quý vị đã lắng nghe!"

---

## 🛠️ HƯỚNG DẪN TÁI TẠO DỮ LIỆU SEED CHO BUỔI BẢO VỆ

Nếu khởi động lại database hoặc muốn làm mới dữ liệu trước buổi quay:

```bash
# 1. Copy file seed SQL vào container MySQL
docker cp docs/sample-data/seed_demo_full.sql nguon-goc-so-mysql-1:/tmp/seed_demo_full.sql

# 2. Thực thi nạp dữ liệu bằng charset UTF-8
docker compose exec mysql mysql -u root -proot --default-character-set=utf8mb4 nguon_goc_so -e "SOURCE /tmp/seed_demo_full.sql;"
```

Sau khi chạy lệnh trên, toàn bộ dữ liệu 5 Tab (`DEMO-NHO-01`, `orgmanager`, `procurement`, `regulator`, `ANOMALY-DEMO-01`, chuỗi SHA-256 INTACT, phiếu bàn giao, lệnh thu hồi) đều sẵn sàng 100%.