import { CircleHelp, LoaderCircle } from "lucide-react";
import { useHelp } from "@/hooks/useHelp";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface HelpButtonProps {
  /** Mã định danh màn hình (ví dụ: "farm-log-create"). */
  screenKey: string;
  /** Nhãn nút — mặc định "Hướng dẫn". */
  label?: string;
  /** Chỉ hiển thị icon, không hiện nhãn. */
  iconOnly?: boolean;
  /** Class ngoài tuỳ chỉnh. */
  className?: string;
}

/**
 * Nút mở drawer hướng dẫn sử dụng cho một màn hình (NCL-01-CN-006).
 * Chỉ hiển thị cho người dùng đã đăng nhập (mọi trang trong PrivateRoute).
 */
export function HelpButton({
  screenKey,
  label = "Hướng dẫn",
  iconOnly = false,
  className,
}: HelpButtonProps) {
  return (
    <Sheet>
      <SheetTrigger
        render={
          <Button
            variant="outline"
            size="sm"
            className={cn("gap-1.5", className)}
          >
            <CircleHelp className="size-4" />
            {!iconOnly && label}
          </Button>
        }
      />
      <HelpDrawer screenKey={screenKey} />
    </Sheet>
  );
}

const HELP_TEXT_REPLACEMENTS: [string, string][] = [
  ['PENDING/APPROVED/REJECTED', 'Chờ duyệt / Đã duyệt / Đã từ chối'],
];

const LOCAL_HELP_CONTENT: Record<
  string,
  { title: string; steps: string[] }
> = {
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
};

function formatHelpStep(step: string): string {
  let formatted = step;
  for (const [search, replace] of HELP_TEXT_REPLACEMENTS) {
    formatted = formatted.replaceAll(search, replace);
  }
  return formatted;
}

function HelpDrawer({ screenKey }: { screenKey: string }) {
  const { data, isLoading, error } = useHelp(screenKey);
  const helpData = data || LOCAL_HELP_CONTENT[screenKey] || null;

  return (
    <SheetContent side="right">
      <SheetHeader className="pr-6">
        <SheetTitle>
          {isLoading && !helpData ? "Đang tải hướng dẫn..." : helpData?.title ?? "Hướng dẫn sử dụng"}
        </SheetTitle>
        <SheetDescription className="sr-only">
          {helpData?.title ?? "Hướng dẫn sử dụng"}
        </SheetDescription>
      </SheetHeader>

      <div className="flex-1 overflow-y-auto pr-1">
        {isLoading && !helpData ? (
          <div className="flex items-center gap-2 py-8 text-muted-foreground">
            <LoaderCircle className="size-4 animate-spin" />
            Đang tải...
          </div>
        ) : error && !helpData ? (
          <p className="py-8 text-center text-sm text-muted-foreground">{error}</p>
        ) : !helpData ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Chưa có hướng dẫn cho màn hình này.
          </p>
        ) : (
          <div className="space-y-4">
            <ol className="list-none space-y-3">
              {helpData.steps.map((step, index) => (
                <li key={index} className="flex gap-3">
                  <span
                    className={cn(
                      "flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                      "bg-primary/10 text-primary"
                    )}
                  >
                    {index + 1}
                  </span>
                  <span className="text-sm leading-relaxed text-foreground">
                    {formatHelpStep(step)}
                  </span>
                </li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </SheetContent>
  );
}