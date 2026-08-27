import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useLocation } from "react-router-dom";
import {
  Breadcrumb,
  type BreadcrumbItem,
} from "@/components/ui/Breadcrumb";

// ============================================================
// Route registry: nhãn tiếng Việt cho từng route template.
// Template tĩnh đứng trước template có :param ở cùng độ dài.
// ============================================================

const ROUTE_TEMPLATES: ReadonlyArray<readonly [string, string]> = [
  // User Profile
  ["/profile", "Hồ sơ người dùng"],

  // Organizations
  ["/organizations/profile", "Hồ sơ tổ chức"],
  ["/organizations/create", "Tạo tổ chức"],
  ["/organizations/:id", "Chi tiết tổ chức"],
  ["/organizations", "Tổ chức"],

  // Members
  ["/members/create", "Thêm thành viên"],
  ["/members", "Thành viên & quyền"],

  // Farm areas
  ["/farm-areas/create", "Tạo vùng trồng"],
  ["/farm-areas/:id/edit", "Chỉnh sửa vùng trồng"],
  ["/chinhsuavungtrong/:id", "Chỉnh sửa vùng trồng"],
  ["/farm-areas", "Vùng trồng"],

  // Production lots
  ["/production-lots/create", "Tạo lô sản xuất"],
  ["/production-lots/import", "Nhập lô sản xuất"],
  [
    "/production-lots/:lotId/shipments/:shipmentId",
    "Chi tiết lô hàng",
  ],
  [
    "/production-lots/:productionLotId/farm-logs",
    "Nhật ký canh tác",
  ],
  [
    "/production-lots/:lotId/inspection-requests/create",
    "Tạo yêu cầu kiểm nghiệm",
  ],
  [
    "/production-lots/:id/inspection-requests/create",
    "Tạo yêu cầu kiểm nghiệm",
  ],
  [
    "/production-lots/:lotId/inspection-requests/:requestId/results",
    "Kết quả kiểm nghiệm",
  ],
  [
    "/inspection-requests/:requestId/results",
    "Kết quả kiểm nghiệm",
  ],
  ["/production-lots/:id/edit", "Chỉnh sửa lô sản xuất"],
  ["/production-lots/:id", "Chi tiết lô sản xuất"],
  ["/production-lots", "Lô sản xuất"],

  // Shipments
  [
    "/production-lots/:productionLotId/shipments/create",
    "Tạo lô hàng",
  ],
  ["/shipments/:id", "Chi tiết lô hàng"],

  // Farm logs
  ["/farm-logs/create", "Ghi nhật ký canh tác"],
  ["/farm-logs", "Nhật ký canh tác"],

  // Preprocessing / Packaging / Transport events
  ["/preprocessing-events/create", "Ghi sơ chế"],
  ["/preprocessing-events/:id/correct", "Sửa sơ chế"],
  ["/preprocessing-events", "Sơ chế"],
  ["/packaging-events/create", "Ghi đóng gói"],
  ["/packaging-events/:id/correct", "Sửa đóng gói"],
  ["/packaging-events", "Đóng gói"],
  ["/transport-events/record", "Ghi vận chuyển"],
  ["/transport-events", "Vận chuyển"],
  ["/chain-events/scan", "Quét mã sự kiện"],
  ["/offline-events", "Sự kiện ngoại tuyến"],

  // Admin
  ["/admin/code-ranges/create", "Cấp dải mã"],
  ["/admin/code-ranges", "Dải mã truy xuất"],
  ["/admin/product-categories/create", "Thêm loại nông sản"],
  ["/admin/product-categories/:id/edit", "Cập nhật loại nông sản"],
  ["/admin/product-categories", "Danh mục sản phẩm"],
  ["/admin/input-materials/create", "Khai báo vật tư mới"],
  ["/admin/input-materials/:id/edit", "Chỉnh sửa vật tư"],
  ["/admin/input-materials/:id", "Chi tiết vật tư"],
  ["/admin/input-materials", "Danh mục vật tư"],
  ["/admin/standards/:standardId/criteria", "Tiêu chí đánh giá"],
  ["/admin/standards/create", "Thêm tiêu chuẩn"],
  ["/admin/standards/:id/edit", "Cập nhật tiêu chuẩn"],
  ["/admin/standards", "Tiêu chuẩn"],
  ["/admin/backup-restore", "Sao lưu & khôi phục"],
  ["/admin/system-monitoring", "Giám sát hệ thống"],
  ["/admin/suspect-trace-codes/:traceCodeId", "Chi tiết mã nghi vấn"],
  ["/admin/suspect-trace-codes", "Mã truy xuất nghi vấn"],
  ["/integration/api-keys/create", "Cấp khóa API"],
  ["/integration/api-keys", "Khóa API đối tác"],

  // Reports
  ["/reports/lookup-statistics", "Thống kê tra cứu"],
  ["/reports/crop-area-analysis", "Phân tích vùng trồng"],
  ["/reports/season-yield-comparison", "So sánh mùa vụ"],
  ["/reports/industry", "Báo cáo ngành"],
  ["/activity-logs", "Nhật ký hoạt động"],
  ["/login-history", "Lịch sử đăng nhập"],
  ["/login-anomalies", "Bất thường đăng nhập"],
  ["/failed-event-logs", "Log sự kiện lỗi"],

  // Notifications / Alerts
  ["/notifications", "Thông báo"],
  ["/alerts/scan-anomaly", "Cảnh báo quét nghi vấn"],

  // Certifications
  ["/certifications/create", "Tạo chứng nhận"],
  ["/certifications", "Kiểm nghiệm & chứng nhận"],

  // Integration / Export / Permissions
  ["/integration/api-keys", "Khóa API đối tác"],
  ["/export/open-data", "Dữ liệu mở"],
  ["/permissions/config", "Cấu hình quyền"],

  // Warehouse / Storage
  ["/warehouse-receipt/:eventId", "Chi tiết phiếu nhập kho"],
  ["/warehouse-receipt", "Phiếu nhập kho"],
  ["/storage-condition", "Điều kiện bảo quản"],
  ["/event-chain-verification", "Xác minh chuỗi sự kiện"],

  // Mobile / Invitations / Recall / Feedback
  ["/mobile/record-event", "Ghi sự kiện di động"],
  ["/invitations/create", "Tạo lời mời"],
  ["/recall-requests/create", "Tạo yêu cầu thu hồi"],
  ["/recall-requests/:id", "Chi tiết yêu cầu thu hồi"],
  ["/recall-requests", "Yêu cầu thu hồi"],
  ["/product-feedbacks", "Phản hồi người dùng"],
  ["/forgot-password", "Quên mật khẩu"],
  ["/reset-password", "Đặt lại mật khẩu"],
];

/** Tìm nhãn cho một tiền tố đường dẫn khớp với template trong danh sách cho trước. */
function matchTemplate(
  prefixPath: string,
  templates: ReadonlyArray<readonly [string, string]>,
): string | null {
  const segs = prefixPath.split("/").filter(Boolean);

  let paramFallback: string | null = null;
  for (const [template, label] of templates) {
    const tsegs = template.split("/").filter(Boolean);
    if (tsegs.length !== segs.length) continue;

    let matched = true;
    let hasParam = false;
    for (let i = 0; i < tsegs.length; i += 1) {
      if (tsegs[i].startsWith(":")) {
        hasParam = true;
        continue;
      }
      if (tsegs[i] !== segs[i]) {
        matched = false;
        break;
      }
    }
    if (!matched) continue;
    // Ưu tiên template tĩnh khớp toàn bộ; nếu chỉ khớp nhờ :param thì ghi nhớ
    // để ưu tiên các template khác, nhưng vẫn dùng làm phương án dự phòng.
    if (!hasParam) return label;
    paramFallback = label;
  }
  return paramFallback;
}

/**
 * Nhãn cho các đoạn đường dẫn trung gian KHÔNG phải là route thật
 * (ví dụ /permissions, /reports, /admin). Các mục này hiển thị dạng
 * chữ thường, không có liên kết để tránh link 404.
 */
const GROUP_TEMPLATES: ReadonlyArray<readonly [string, string]> = [
  ["/reports", "Báo cáo"],
  ["/permissions", "Phân quyền"],
  ["/alerts", "Cảnh báo"],
  ["/integration", "Tích hợp"],
  ["/export", "Xuất dữ liệu"],
  ["/preprocessing-events", "Sơ chế"],
  ["/packaging-events", "Đóng gói"],
  ["/transport-events", "Vận chuyển"],
  ["/chain-events", "Sự kiện chuỗi"],
  ["/farm-logs", "Nhật ký canh tác"],
  ["/production-lots/:id/shipments", "Lô hàng"],
  ["/production-lots/:id/inspection-requests", "Yêu cầu kiểm nghiệm"],
  [
    "/production-lots/:id/inspection-requests/:requestId",
    "Chi tiết yêu cầu",
  ],
  ["/admin/standards/:standardId", "Chi tiết tiêu chuẩn"],
];

function capitalizeFirst(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Sinh danh sách breadcrumb tự động từ pathname hiện tại. */
export function buildAutoBreadcrumb(pathname: string): BreadcrumbItem[] {
  const segments = pathname.split("?")[0].split("/").filter(Boolean);
  const items: BreadcrumbItem[] = [
    { label: "Dashboard", href: "/dashboard" },
  ];

  for (let i = 1; i <= segments.length; i += 1) {
    const prefix = `/${segments.slice(0, i).join("/")}`;
    const isLast = i === segments.length;
    const fallbackLabel = capitalizeFirst(
      decodeURIComponent(segments[i - 1]).replace(/-/g, " "),
    );

    if (isLast) {
      // Trang hiện tại: không cần liên kết
      const label =
        matchTemplate(prefix, ROUTE_TEMPLATES) ??
        matchTemplate(prefix, GROUP_TEMPLATES) ??
        fallbackLabel;
      items.push({ label });
    } else if (matchTemplate(prefix, ROUTE_TEMPLATES) !== null) {
      // Route trung gian tồn tại -> gắn liên kết
      items.push({
        label: matchTemplate(prefix, ROUTE_TEMPLATES) as string,
        href: prefix,
      });
    } else {
      // Không phải route thật -> chỉ hiện nhãn nếu có trong GROUP_TEMPLATES
      const groupLabel = matchTemplate(prefix, GROUP_TEMPLATES);
      if (groupLabel) {
        items.push({ label: groupLabel });
      }
    }
  }
  return items;
}

// ============================================================
// Context cho phép trang ghi đè breadcrumb bằng dữ liệu động
// (ví dụ tên lô sản xuất / lô hàng lấy từ API).
// ============================================================

interface BreadcrumbOverrideContextValue {
  override: BreadcrumbItem[] | null;
  setOverride: (items: BreadcrumbItem[] | null) => void;
}

const BreadcrumbOverrideContext =
  createContext<BreadcrumbOverrideContextValue>({
    override: null,
    setOverride: () => undefined,
  });

export function BreadcrumbOverrideProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [override, setOverrideState] = useState<BreadcrumbItem[] | null>(null);

  // QUAN TRỌNG: hàm setter phải ổn định (useCallback rỗng) để effect trong
  // useSetBreadcrumb không bị chạy lại vô hạn khi override thay đổi.
  const setOverride = useCallback((items: BreadcrumbItem[] | null) => {
    setOverrideState(items);
  }, []);

  const value = useMemo(
    () => ({ override, setOverride }),
    [override, setOverride],
  );

  return (
    <BreadcrumbOverrideContext.Provider value={value}>
      {children}
    </BreadcrumbOverrideContext.Provider>
  );
}

/**
 * Cho phép một trang cung cấp breadcrumb "đẹp" với dữ liệu động
 * (tên lô, tên lô hàng...). Khi trang không đặt hoặc rời khỏi trang,
 * breadcrumb tự động sinh từ pathname sẽ được dùng.
 *
 * ```tsx
 * useSetBreadcrumb(lot ? [
 *   { label: "Dashboard", href: "/dashboard" },
 *   { label: "Lô sản xuất", href: "/production-lots" },
 *   { label: lot.name }, // trang hiện tại
 * ] : null);
 * ```
 */
export function useSetBreadcrumb(items: BreadcrumbItem[] | null): void {
  const { setOverride } = useContext(BreadcrumbOverrideContext);
  const key = items ? JSON.stringify(items) : null;

  useEffect(() => {
    setOverride(key ? (JSON.parse(key) as BreadcrumbItem[]) : null);
    return () => setOverride(null);
  }, [key, setOverride]);
}

/**
 * Breadcrumb hiển thị trong layout: dùng override của trang nếu có,
 * ngược lại tự sinh từ pathname theo route registry.
 */
export function AppBreadcrumb() {
  const location = useLocation();
  const { override } = useContext(BreadcrumbOverrideContext);
  const items = override ?? buildAutoBreadcrumb(location.pathname);

  return <Breadcrumb items={items} />;
}
