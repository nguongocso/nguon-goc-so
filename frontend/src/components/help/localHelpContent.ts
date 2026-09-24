/** Nội dung hướng dẫn dự phòng khi backend chưa có dữ liệu. */
export interface LocalHelpEntry {
  title: string;
  steps: string[];
}

/** Nội dung hướng dẫn tuỳ biến cho từng màn hình. */
export interface HelpCustomContent {
  title: string;
  steps: string[];
}

/** Bảng thay thế cụm từ viết tắt thành tiếng Việt đầy đủ. */
export const HELP_TEXT_REPLACEMENTS: [string, string][] = [
  ['PENDING/APPROVED/REJECTED', 'Chờ duyệt / Đã duyệt / Đã từ chối'],
];

/** Từ điển hướng dẫn cục bộ theo mã màn hình. */
export const LOCAL_HELP_CONTENT: Record<string, LocalHelpEntry> = {
  shipments: {
    title: 'Hướng dẫn tạo lô hàng mới',
    steps: [
      'Kiểm tra số lượng mã truy xuất còn lại của tổ chức trước khi tạo lô hàng',
      'Nhập tên lô hàng phân biệt',
      'Nhập số lượng sản phẩm / đơn vị cần đóng gói xuất xưởng',
      'Điền thông tin đóng gói hoặc quy cách đóng gói',
      'Nhấn "Tạo lô hàng" để hệ thống tự động cấp phát dải mã QR truy xuất tương ứng',
    ],
  },
  'shipment-create': {
    title: 'Hướng dẫn tạo lô hàng mới',
    steps: [
      'Kiểm tra số lượng mã truy xuất còn lại của tổ chức trước khi tạo lô hàng',
      'Nhập tên lô hàng phân biệt',
      'Nhập số lượng sản phẩm / đơn vị cần đóng gói xuất xưởng',
      'Điền thông tin đóng gói hoặc quy cách đóng gói',
      'Nhấn "Tạo lô hàng" để hệ thống tự động cấp phát dải mã QR truy xuất tương ứng',
    ],
  },
  'shipment-detail': {
    title: 'Hướng dẫn chi tiết lô hàng',
    steps: [
      'Theo dõi thông tin tổng quan, số lượng, quy cách đóng gói và trạng thái của lô hàng',
      'Kích hoạt lô hàng nếu đang ở trạng thái nháp để sẵn sàng phát hành tem QR',
      'Xem danh sách mã QR truy xuất và xuất file tem in mã QR khi cần dán lên bao bì sản phẩm',
      'Xuất hồ sơ truy xuất nguồn gốc điện tử phục vụ kiểm tra và chứng nhận',
      'Thực hiện thu hồi lô hàng khi phát hiện sự cố chất lượng hoặc theo yêu cầu',
    ],
  },
  'report-login-history': {
    title: 'Hướng dẫn xem lịch sử đăng nhập',
    steps: [
      'Theo dõi danh sách các phiên đăng nhập vào hệ thống của tài khoản cá nhân',
      'Sử dụng bộ lọc theo kết quả (Thành công / Thất bại) hoặc khoảng thời gian để tra cứu phiên đăng nhập cụ thể',
      'Kiểm tra thông tin thời gian, địa chỉ IP, quốc gia và cảnh báo địa điểm mới để phát hiện đăng nhập bất thường',
      'Nhấn nút "Làm mới" để cập nhật dữ liệu đăng nhập mới nhất',
    ],
  },
  'organization-list': {
    title: 'Hướng dẫn quản lý tổ chức',
    steps: [
      'Theo dõi danh sách tất cả các hợp tác xã, doanh nghiệp và tổ chức trong hệ thống',
      'Nhấn nút "Tạo tổ chức" để thêm mới hợp tác xã hoặc doanh nghiệp',
      'Nhấn "Xem" tại từng dòng để xem chi tiết thông tin và thành viên của tổ chức',
      'Nhấn "Làm mới" để cập nhật dữ liệu danh sách tổ chức mới nhất',
    ],
  },
  'organization-detail': {
    title: 'Hướng dẫn chi tiết tổ chức',
    steps: [
      'Xem thông tin tổng quan của tổ chức gồm mã, tên, loại hình, trạng thái và địa chỉ',
      'Theo dõi danh sách thành viên thuộc tổ chức và vai trò được phân công',
      'Nhấn "Thêm thành viên" để thêm người dùng vào tổ chức',
      'Nhấn "Làm mới" để cập nhật dữ liệu chi tiết tổ chức mới nhất',
    ],
  },
  'organization-profile': {
    title: 'Hướng dẫn hồ sơ tổ chức',
    steps: [
      'Xem thông tin hồ sơ pháp lý, mã định danh và loại hình của tổ chức',
      'Kiểm tra thông tin liên hệ như số điện thoại, email và địa chỉ trụ sở',
      'Cập nhật thông tin mô tả và người đại diện khi có thay đổi',
      'Nhấn "Lưu thông tin" để cập nhật hồ sơ tổ chức',
    ],
  },
  'farm-area-edit': {
    title: 'Hướng dẫn chỉnh sửa vùng trồng',
    steps: [
      'Kiểm tra và cập nhật các thông tin cơ bản như tên, mã, diện tích và loại cây trồng',
      'Điều chỉnh vị trí địa lý, tọa độ GPS hoặc ranh giới vùng trồng trên bản đồ',
      'Cập nhật thông tin thổ nhưỡng, nguồn nước hoặc ghi chú canh tác',
      'Nhấn "Lưu thay đổi" để hoàn tất cập nhật vùng trồng',
    ],
  },
  'admin-account-areas': {
    title: 'Hướng dẫn phân công địa bàn',
    steps: [
      'Chọn cán bộ quản lý từ danh sách bên trái hoặc sử dụng ô tìm kiếm',
      'Xem danh sách các địa bàn tỉnh/thành, quận/huyện, phường/xã đã phân công',
      'Lựa chọn cấp hành chính và địa bàn mới rồi nhấn "Gán địa bàn"',
      'Nhấn "Gỡ" tại từng địa bàn để hủy phân công tương ứng',
    ],
  },
  'admin-input-materials': {
    title: 'Hướng dẫn quản lý vật tư đầu vào',
    steps: [
      'Xem danh sách các loại vật tư gồm tên, mã, loại vật tư, đơn vị tính và nhà sản xuất',
      'Sử dụng bộ lọc loại vật tư và ô tìm kiếm để tra cứu nhanh vật tư',
      'Nhấn "Thêm vật tư" để tạo mới vật tư đầu vào dùng chung trong hệ thống',
      'Nhấn "Xem" hoặc "Chỉnh sửa" tại từng dòng để cập nhật thông tin chi tiết',
    ],
  },
  'admin-standard-criteria': {
    title: 'Hướng dẫn quản lý tiêu chí kiểm nghiệm',
    steps: [
      'Xem danh sách các tiêu chí kiểm nghiệm thuộc tiêu chuẩn chất lượng đã chọn',
      'Nhấn "Thêm tiêu chí" để định nghĩa chỉ số kiểm nghiệm, phương pháp đo và ngưỡng cho phép',
      'Chỉnh sửa hoặc xóa tiêu chí khi có sự điều chỉnh theo quy chuẩn kỹ thuật',
      'Nhấn "Làm mới" để tải lại danh sách tiêu chí mới nhất',
    ],
  },
  'admin-system-monitoring': {
    title: 'Hướng dẫn giám sát hệ thống',
    steps: [
      'Theo dõi tình trạng hoạt động của các dịch vụ, database và thành phần hệ thống',
      'Giám sát các thông số tài nguyên máy chủ như CPU, bộ nhớ RAM và dung lượng đĩa',
      'Xem thống kê số lượng request, thời gian phản hồi và tỷ lệ lỗi thời gian thực',
      'Nhấn "Làm mới" để cập nhật chỉ số giám sát mới nhất',
    ],
  },
  'report-login-anomalies': {
    title: 'Hướng dẫn xử lý cảnh báo đăng nhập',
    steps: [
      'Xem danh sách các phiên đăng nhập bị cảnh báo bất thường trong hệ thống',
      'Sử dụng bộ lọc theo mức độ rủi ro, trạng thái xử lý hoặc khoảng thời gian để tra cứu',
      'Xem chi tiết địa chỉ IP, vị trí địa lý và lý do cảnh báo đăng nhập bất thường',
      'Đánh dấu đã xác nhận hoặc xử lý cảnh báo bảo mật tương ứng',
    ],
  },
  'impact-scope-trace': {
    title: 'Hướng dẫn truy vết phạm vi ảnh hưởng',
    steps: [
      'Nhập Mã lô sản xuất, Mã lô hàng hoặc Mã tem QR vào ô tìm kiếm và nhấn "Mở truy vết"',
      'Xem thông tin chiều ngược (Upstream) gồm Vùng trồng gốc và Lô sản xuất hạt nhân',
      'Theo dõi chiều xuôi (Downstream) gồm các Lô hàng sinh ra, số lượng tem kích hoạt và lượt quét công khai',
      'Kiểm tra dòng thời gian các sự kiện Vận chuyển & Thu mua và thông tin các Tổ chức nhận (Bên thứ ba)',
      'Nhấn nút "Xuất tệp" ở góc trên để tải báo cáo truy vết dạng Excel (.xlsx) hoặc PDF (.pdf)',
    ],
  },
  'bulk-recall-request-list': {
    title: 'Yêu cầu thu hồi theo phạm vi ảnh hưởng',
    steps: [
      'Xem danh sách các yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng của hợp tác xã',
      'Sử dụng thanh tìm kiếm hoặc lọc trạng thái (Chờ duyệt, Đã duyệt, Từ chối, Đã xử lý) để tra cứu yêu cầu cần xử lý',
      'Tại cột "Thao tác", với yêu cầu "Chờ duyệt": Quản lý HTX có thể nhấn icon Phê duyệt (dấu tích xanh) hoặc Từ chối (dấu X đỏ) và nhập lý do',
      'Tại cột "Thao tác", với yêu cầu "Đã duyệt": Quản lý HTX nhấn icon Kết thúc vụ việc (sổ kiểm tra) để ghi nhận kết quả xử lý các lô, biện pháp khắc phục và đính kèm tối đa 5 tệp biên bản (.pdf, .docx)',
      'Tại cột "Chi tiết", chọn biểu tượng Chi tiết (con mắt) để xem toàn bộ thông tin yêu cầu, danh sách lô hàng ảnh hưởng, tiến trình và mở xem tệp biên bản đính kèm trên tab mới',
    ],
  },
  'bulk-recall-request-create': {
    title: 'Hướng dẫn tạo yêu cầu thu hồi theo phạm vi ảnh hưởng',
    steps: [
      'Kiểm tra thông tin lô sản xuất nguồn',
      'Nhập "Lý do thu hồi" và "Bằng chứng" nếu có',
      'Chọn các lô hàng cần thu hồi trong "Phạm vi thu hồi"',
      'Chỉ các lô đủ điều kiện mới có thể được chọn',
      'Kiểm tra lại danh sách lô đã chọn và nhấn "Tạo yêu cầu thu hồi" để gửi yêu cầu phê duyệt',
    ],
  },
  'report-organization-usage': {
    title: 'Hướng dẫn bảng điều khiển mức độ sử dụng nền tảng',
    steps: [
      'Chọn khoảng "Từ ngày" và "Đến ngày", bảng tự tải lại theo kỳ mới mà không cần nhấn thêm nút nào, kỳ trước được tính tự động với độ dài tương đương',
      'Theo dõi 6 chỉ số của từng tổ chức gồm lô sản xuất, nhật ký, sự kiện chuỗi, tem kích hoạt, tra cứu công khai và người dùng hoạt động kèm phần trăm thay đổi so với kỳ trước',
      'Nhấn tiêu đề cột để sắp xếp theo từng chỉ số, dùng ô tìm kiếm và bộ lọc trạng thái để tra cứu tổ chức cụ thể',
      'Tổ chức không có hoạt động trong 30 ngày được gắn nhãn "Cần liên hệ hỗ trợ", tổ chức chưa có dữ liệu trong kỳ hiển thị "Chưa có dữ liệu"',
      'Nhấn "Xuất báo cáo" và chọn "Xuất CSV" hoặc "Xuất PDF" để tải báo cáo tổng hợp theo kỳ đang chọn',
    ],
  },
  'admin-anomaly-thresholds': {
    title: 'Hướng dẫn cấu hình ngưỡng quét bất thường',
    steps: [
      'Cấu hình Ngưỡng toàn cục: Thiết lập các thông số quét áp dụng mặc định cho toàn bộ mã tem trong hệ thống gồm: Quét / giờ, Quét / ngày (24h), Khoảng cách tối đa (km), Thời gian di chuyển (phút) và Thời gian ân hạn (ngày).',
      'Thời gian ân hạn (0 - 7 ngày, mặc định 3 ngày): Số ngày miễn đánh giá quét bất thường kể từ thời điểm kích hoạt tem. Trong thời gian này, các lượt quét thử nghiệm/nội bộ được bỏ qua để tránh báo động giả.',
      'Ước lượng tác động (30 ngày): Nhấn nút "Ước lượng tác động" trên thẻ toàn cục để chạy thử nghiệm mô phỏng (Dry-run), kiểm tra trước số lượng tem và tỷ lệ quét bị ảnh hưởng trước khi áp dụng cấu hình mới.',
      'Lưu cấu hình toàn cục: Nhấn nút "Lưu cấu hình toàn cục" để áp dụng ngay ngưỡng mới vào bộ máy phát hiện thời gian thực.',
      'Bảng Ghi đè theo danh mục nông sản: Theo dõi danh sách gồm 7 cột: Loại nông sản, Quét / giờ, Quét / ngày (24h), Khoảng cách tối đa, Thời gian di chuyển, Thời gian ân hạn và Thao tác.',
      'Thêm ghi đè danh mục: Nhấn "Thêm ghi đè danh mục" để thiết lập bộ ngưỡng riêng cho loại nông sản có đặc thù phân phối riêng biệt (ưu tiên cao hơn ngưỡng toàn cục).',
      'Chỉnh sửa hoặc Xóa ghi đè: Tại cột "Thao tác", chọn biểu tượng Sửa (cây bút) để điều chỉnh ngưỡng danh mục, hoặc chọn Xóa (thùng rác) để đưa danh mục trở lại dùng ngưỡng toàn cục mặc định.',
    ],
  },
  'admin-suspect-trace-codes': {
    title: 'Hướng dẫn xử lý mã tem nghi vấn',
    steps: [
      'Theo dõi danh sách các mã tem bị hệ thống cảnh báo nghi vấn gian lận hoặc sao chép mã (điểm nghi vấn ≥ 50/100).',
      'Xem chi tiết bằng chứng: Nhấn "Chi tiết" tại từng dòng để xem Snapshot phân tích điểm vi phạm và lịch sử quét thực tế.',
      'Khóa mã tem (SUSPECT): Nếu mã tem đang nghi vấn, nhấn "Khóa mã tem", nhập lý do vi phạm để ngừng lưu hành và cảnh báo người tiêu dùng khi quét.',
      'Mở khóa mã tem (LOCKED): Nếu tem đã khóa và có kết quả đối soát thực tế hợp lệ, nhấn "Mở khóa mã tem", nhập kết luận xác minh (≥ 10 ký tự) và bằng chứng đối chiếu để kích hoạt lại tem.',
    ],
  },
  'admin-suspect-trace-code-suspect': {
    title: 'Hướng dẫn khóa mã tem nghi vấn (Trạng thái Nghi vấn)',
    steps: [
      'Kiểm tra Thông tin nghi vấn: Đối chiếu thông tin Lô hàng, Mã tem, Loại nông sản và Nguồn cấu hình ngưỡng áp dụng.',
      'Phân tích Chi tiết điểm nghi vấn: Kiểm tra Snapshot bằng chứng vi phạm cố định (+35 Tần suất cao, +45 Di chuyển phi lý, +20 Nhiều địa điểm).',
      'Đối soát Lịch sử quét 24h: Xem lại bảng lịch sử quét gồm thời gian, vị trí, tọa độ GPS và thiết bị để xác định bất thường phân phối.',
      'Mở biểu mẫu khóa: Nhấn nút màu đỏ "Khóa mã tem" ở góc trên bên phải màn hình.',
      'Nhập lý do khóa: Điền cụ thể lý do khóa (ví dụ: phát hiện quét tem đồng thời ở 2 tỉnh cách nhau > 500km, quét lặp bất thường từ thiết bị lạ).',
      'Xác nhận khóa: Nhấn "Xác nhận khóa" để đưa tem về trạng thái ĐÃ KHÓA (LOCKED), lập tức hiển thị cảnh báo đỏ trên trang quét công khai của người tiêu dùng.',
    ],
  },
  'admin-suspect-trace-code-locked': {
    title: 'Hướng dẫn mở khóa mã tem (Trạng thái Đã khóa)',
    steps: [
      'Kiểm tra Lịch sử khóa: Xem kỹ Thời điểm khóa, Người thực hiện khóa và Lý do khóa tem ở phần Thông tin nghi vấn.',
      'Thu thập & Xác minh thực tế: Thu thập biên bản giải trình, hóa đơn chứng từ, lịch trình xe vận chuyển hoặc ảnh chụp thực tế từ chủ cơ sở/hợp tác xã.',
      'Mở biểu mẫu mở khóa: Nhấn nút màu xanh lá "Mở khóa mã tem" ở góc trên bên phải màn hình.',
      'Nhập kết luận xác minh: Điền nội dung kết luận xử lý vào ô "Kết luận xác minh" (tối thiểu 10 ký tự, nêu rõ kết quả kiểm tra thực địa).',
      'Cung cấp bằng chứng: Điền thông tin vào ô "Bằng chứng xác minh" (ví dụ: số vận đơn giao nhận, số biên bản làm việc, hình ảnh tem chính hãng).',
      'Xác nhận mở khóa: Nhấn "Xác nhận mở khóa" để đưa tem về trạng thái HOẠT ĐỘNG (ACTIVE), gỡ bỏ cảnh báo khóa trên cổng thông tin công khai.',
    ],
  },
};

/** Chuẩn hoá câu hướng dẫn trước khi hiển thị. */
export function formatHelpStep(step: string): string {
  let formatted = step;
  for (const [search, replace] of HELP_TEXT_REPLACEMENTS) {
    formatted = formatted.replaceAll(search, replace);
  }
  return formatted;
}
