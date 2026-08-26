import { createContext, useContext, useEffect, useMemo, useState } from "react";
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
  ["/admin/product-categories", "Danh mục sản phẩm"],
  ["/admin/standards/:standardId/criteria", "Tiêu chí đánh giá"],
  ["/admin/standards", "Tiêu chuẩn"],
  ["/admin/backup-restore", "Sao lưu & khôi phục"],
  ["/admin/system-monitoring", "Giám sát hệ thống"],
  ["/admin/suspect-trace-codes/:traceCodeId", "Chi tiết mã nghi vấn"],
  ["/admin/suspect-trace-codes", "Mã truy xuất nghi vấn"],

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
];

/** Tìm nhãn cho một tiền tố đường dẫn khớp với template trong registry. */
function matchTemplateLabel(prefixPath: string): string | null {
  const segs = prefixPath.split("/").filter(Boolean);

  let paramFallback: string | null = null;
  for (const [template, label] of ROUTE_TEMPLATES) {
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

/** Sinh danh sách breadcrumb tự động từ pathname hiện tại. */
export function buildAutoBreadcrumb(pathname: string): BreadcrumbItem[] {
  const segments = pathname.split("?")[0].split("/").filter(Boolean);
  const items: BreadcrumbItem[] = [];

  for (let i = 1; i <= segments.length; i += 1) {
    const prefix = `/${segments.slice(0, i).join("/")}`;
    const isLast = i === segments.length;
    const label =
      matchTemplateLabel(prefix) ??
      decodeURIComponent(segments[i - 1]).replace(/-/g, " ");

    items.push(isLast ? { label } : { label, href: prefix });
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

  const value = useMemo(
    () => ({
      override,
      setOverride: (items: BreadcrumbItem[] | null) => setOverrideState(items),
    }),
    [override],
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
