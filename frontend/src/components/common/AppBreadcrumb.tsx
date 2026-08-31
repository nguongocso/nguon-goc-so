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
import {
  AUTHENTICATED_ROLE_CODES,
  ROLE_ACCESS,
  hasAnyRole,
  type AuthenticatedRoleCode,
} from "@/config/roleAccess";
import { useAuth } from "@/hooks/useAuth";

// ============================================================
// Route registry: nhãn tiếng Việt cho từng route template.
// Template tĩnh đứng trước template có :param ở cùng độ dài.
// ============================================================

export const ROUTE_TEMPLATES: ReadonlyArray<readonly [string, string]> = [
  // User Profile & Dashboard
  ["/dashboard", "Dashboard"],
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
  ["/shipments/:id/cancellation-history", "Lịch sử hủy tem"],
  [
    "/production-lots/:lotId/shipments/:id/cancellation-history",
    "Lịch sử hủy tem",
  ],
  ["/shipments/:id/cancel-labels", "Hủy tem"],
  [
    "/production-lots/:lotId/shipments/:id/cancel-labels",
    "Hủy tem",
  ],
  ["/shipments/:id", "Chi tiết lô hàng"],

  // Farm logs
  ["/farm-logs/create", "Ghi nhật ký canh tác"],
  ["/farm-logs/:id/correct", "Sửa nhật ký canh tác"],
  ["/farm-logs/:id", "Chi tiết nhật ký canh tác"],

  // Preprocessing / Packaging / Transport events
  ["/preprocessing-events/create", "Ghi sơ chế"],
  ["/preprocessing-events/:id/correct", "Sửa sơ chế"],
  ["/packaging-events/create", "Ghi đóng gói"],
  ["/packaging-events/:id/correct", "Sửa đóng gói"],
  ["/transport-events/record", "Ghi vận chuyển"],
  ["/chain-events/scan", "Quét mã sự kiện"],
  ["/offline-events", "Sự kiện ngoại tuyến"],

  // Admin
  ["/admin/code-ranges/create", "Cấp dải mã"],
  ["/admin/code-ranges", "Dải mã truy xuất"],
  ["/admin/product-categories/create", "Thêm loại nông sản"],
  ["/admin/product-categories/:id/edit", "Cập nhật loại nông sản"],
  ["/admin/product-categories/:id/criteria", "Gán bộ chỉ tiêu kiểm nghiệm"],
  ["/admin/product-categories", "Danh mục sản phẩm"],
  ["/admin/input-materials/create", "Khai báo vật tư mới"],
  ["/admin/input-materials/:id/edit", "Chỉnh sửa vật tư"],
  ["/admin/input-materials/:id", "Chi tiết vật tư"],
  ["/admin/input-materials", "Danh mục vật tư"],
  ["/admin/inspection-criteria/create", "Thêm mới"],
  ["/admin/inspection-criteria", "Chỉ tiêu kiểm nghiệm"],
  ["/admin/standards/create", "Thêm tiêu chuẩn"],
  ["/admin/standards/:id/edit", "Cập nhật tiêu chuẩn"],
  ["/admin/standards", "Tiêu chuẩn"],
  ["/admin/testing-units/create", "Thêm đơn vị kiểm nghiệm"],
  ["/admin/testing-units/:id/edit", "Chỉnh sửa đơn vị kiểm nghiệm"],
  ["/admin/testing-units/:id/scopes", "Phạm vi công nhận"],
  ["/admin/testing-units", "Đơn vị kiểm nghiệm"],
  ["/admin/backup-restore", "Sao lưu & khôi phục"],
  ["/admin/system-monitoring", "Giám sát hệ thống"],
  ["/admin/suspect-trace-codes/:traceCodeId", "Chi tiết mã nghi vấn"],
  ["/admin/suspect-trace-codes", "Mã truy xuất nghi vấn"],
  ["/admin/account-areas", "Phân công địa bàn"],

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
  ["/integration/api-keys/create", "Cấp khóa API"],
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

/**
 * Cấu hình quyền truy cập cho từng route template đích.
 * Dùng để kiểm tra xem route có tồn tại và người dùng có quyền truy cập hay không.
 */
export const ROUTE_ACCESS_CONFIG: ReadonlyArray<
  readonly [string, readonly AuthenticatedRoleCode[]]
> = [
  // User Profile & Dashboard
  ["/dashboard", AUTHENTICATED_ROLE_CODES],
  ["/profile", ROLE_ACCESS.userProfile],

  // Organizations
  ["/organizations/profile", ROLE_ACCESS.organizationProfile],
  ["/organizations/create", ROLE_ACCESS.organizationCreate],
  ["/organizations/:id", ROLE_ACCESS.organizationList],
  ["/organizations", ROLE_ACCESS.organizationList],

  // Members
  ["/members/create", ROLE_ACCESS.memberManagement],
  ["/members", ROLE_ACCESS.memberManagement],

  // Farm areas
  ["/farm-areas/create", ROLE_ACCESS.farmAreaCreate],
  ["/farm-areas/:id/edit", ROLE_ACCESS.farmAreaCreate],
  ["/chinhsuavungtrong/:id", ROLE_ACCESS.farmAreaCreate],
  ["/farm-areas", ["VT-02"]],

  // Production lots
  ["/production-lots/create", ["VT-02"]],
  ["/production-lots/import", ["VT-02"]],
  [
    "/production-lots/:lotId/shipments/:shipmentId",
    ["VT-01", "VT-02", "VT-03", "VT-04"],
  ],
  ["/production-lots/:productionLotId/farm-logs", ["VT-02"]],
  ["/production-lots/:lotId/inspection-requests/create", ["VT-02"]],
  ["/production-lots/:id/inspection-requests/create", ["VT-02"]],
  [
    "/production-lots/:lotId/inspection-requests/:requestId/results",
    ["VT-02"],
  ],
  ["/inspection-requests/:requestId/results", ["VT-02"]],
  ["/production-lots/:id/edit", ROLE_ACCESS.productionLotEdit],
  ["/production-lots/:id", ["VT-01", "VT-02", "VT-03"]],
  ["/production-lots", ROLE_ACCESS.productionLotList],

  // Shipments
  [
    "/production-lots/:productionLotId/shipments/create",
    ["VT-01", "VT-02", "VT-03"],
  ],
  ["/shipments/:id/cancellation-history", ["VT-02", "VT-03", "VT-04"]],
  [
    "/production-lots/:lotId/shipments/:id/cancellation-history",
    ["VT-02", "VT-03", "VT-04"],
  ],
  ["/shipments/:id/cancel-labels", ["VT-02", "VT-03", "VT-04"]],
  [
    "/production-lots/:lotId/shipments/:id/cancel-labels",
    ["VT-02", "VT-03", "VT-04"],
  ],
  ["/shipments/:id", ["VT-02", "VT-03", "VT-04"]],

  // Farm logs
  ["/farm-logs/create", ROLE_ACCESS.farmLogCreate],
  ["/farm-logs/:id/correct", ROLE_ACCESS.farmLogCorrect],
  ["/farm-logs/:id", ROLE_ACCESS.farmLogView],

  // Preprocessing / Packaging / Transport / Chain events
  ["/preprocessing-events/create", ROLE_ACCESS.preprocessingEventCreate],
  ["/preprocessing-events/:id/correct", ROLE_ACCESS.preprocessingEventCorrect],
  ["/packaging-events/create", ROLE_ACCESS.packagingEventCreate],
  ["/packaging-events/:id/correct", ROLE_ACCESS.packagingEventCorrect],
  ["/transport-events/record", ROLE_ACCESS.transportEventRecord],
  ["/chain-events/scan", ROLE_ACCESS.scanQuickEvent],
  ["/offline-events", ["VT-02", "VT-03"]],

  // Admin
  ["/admin/code-ranges/create", ROLE_ACCESS.codeRangeList],
  ["/admin/code-ranges", ROLE_ACCESS.codeRangeList],
  ["/admin/product-categories/create", ["VT-01"]],
  ["/admin/product-categories/:id/edit", ["VT-01"]],
  ["/admin/product-categories/:id/criteria", ["VT-01"]],
  ["/admin/product-categories", ["VT-01"]],
  ["/admin/input-materials/create", ["VT-01"]],
  ["/admin/input-materials/:id/edit", ["VT-01"]],
  ["/admin/input-materials/:id", ["VT-01", "VT-02", "VT-03", "VT-04"]],
  ["/admin/input-materials", ["VT-01", "VT-02", "VT-03", "VT-04"]],
  ["/admin/inspection-criteria/create", ROLE_ACCESS.inspectionCriteriaManagement],
  ["/admin/inspection-criteria", ROLE_ACCESS.inspectionCriteriaManagement],
  ["/admin/standards/create", ROLE_ACCESS.standardManagement],
  ["/admin/standards/:id/edit", ROLE_ACCESS.standardManagement],
  ["/admin/standards", ROLE_ACCESS.standardManagement],
  ["/admin/testing-units/create", ["VT-01"]],
  ["/admin/testing-units/:id/edit", ["VT-01"]],
  ["/admin/testing-units/:id/scopes", ROLE_ACCESS.testingUnitScopeManagement],
  ["/admin/testing-units", ["VT-01"]],
  ["/admin/backup-restore", ["VT-01"]],
  ["/admin/system-monitoring", ["VT-01"]],
  ["/admin/suspect-trace-codes/:traceCodeId", ["VT-01"]],
  ["/admin/suspect-trace-codes", ["VT-01"]],
  ["/admin/account-areas", ROLE_ACCESS.areaAssignment],

  // Reports
  ["/reports/lookup-statistics", ["VT-01", "VT-02"]],
  ["/reports/crop-area-analysis", ["VT-02", "VT-03"]],
  ["/reports/season-yield-comparison", ROLE_ACCESS.seasonYieldComparison],
  ["/reports/industry", ["VT-05"]],
  ["/activity-logs", ["VT-01"]],
  ["/login-history", ["VT-01"]],
  ["/login-anomalies", ["VT-01"]],
  ["/failed-event-logs", ["VT-01"]],

  // Notifications / Alerts
  ["/notifications", ROLE_ACCESS.notificationInbox],
  ["/alerts/scan-anomaly", ROLE_ACCESS.scanAnomalyAlerts],

  // Certifications
  ["/certifications/create", ["VT-02"]],
  ["/certifications", ["VT-02"]],

  // Integration / Export / Permissions
  ["/integration/api-keys/create", ROLE_ACCESS.apiKeyManagement],
  ["/integration/api-keys", ROLE_ACCESS.apiKeyManagement],
  ["/export/open-data", ROLE_ACCESS.exportOpenData],
  ["/permissions/config", ROLE_ACCESS.rolePermissionConfig],

  // Warehouse / Storage
  ["/warehouse-receipt/:eventId", ROLE_ACCESS.warehouseReceipt],
  ["/warehouse-receipt", ROLE_ACCESS.warehouseReceipt],
  ["/storage-condition", ROLE_ACCESS.storageCondition],
  ["/event-chain-verification", ROLE_ACCESS.eventChainVerification],

  // Mobile / Invitations / Recall / Feedback
  ["/mobile/record-event", ["VT-02", "VT-03"]],
  ["/invitations/create", ["VT-02"]],
  ["/recall-requests/create", ROLE_ACCESS.recallRequestCreate],
  ["/recall-requests/:id", ROLE_ACCESS.recallRequestManage],
  ["/recall-requests", ROLE_ACCESS.recallRequestManage],
  ["/product-feedbacks", ROLE_ACCESS.productFeedbackManagement],
  ["/forgot-password", AUTHENTICATED_ROLE_CODES],
  ["/reset-password", AUTHENTICATED_ROLE_CODES],
];

/**
 * Tìm danh sách vai trò được phép truy cập theo tiền tố đường dẫn.
 */
export function matchRouteAccess(
  path: string,
  routeAccessConfig: ReadonlyArray<
    readonly [string, readonly AuthenticatedRoleCode[]]
  > = ROUTE_ACCESS_CONFIG,
): readonly AuthenticatedRoleCode[] | null {
  const cleanPath = path.split("?")[0].split("#")[0];
  const segs = cleanPath.split("/").filter(Boolean);

  let paramFallback: readonly AuthenticatedRoleCode[] | null = null;
  for (const [template, allowedRoles] of routeAccessConfig) {
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
    if (!hasParam) return allowedRoles;
    paramFallback = allowedRoles;
  }
  return paramFallback;
}

/**
 * Kiểm tra xem một đường dẫn (href) có hợp lệ (route tồn tại) và
 * vai trò người dùng hiện tại có quyền truy cập hay không.
 */
export function isRouteAccessible(
  href: string | undefined,
  userRole?: string,
): boolean {
  if (!href) return false;
  const allowedRoles = matchRouteAccess(href);
  if (!allowedRoles) {
    return false;
  }
  return hasAnyRole(userRole, allowedRoles);
}

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
  ["/admin", "Quản trị"],
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
export function buildAutoBreadcrumb(
  pathname: string,
  userRole?: string,
): BreadcrumbItem[] {
  const cleanPath = pathname.split("?")[0].split("#")[0];
  const segments = cleanPath.split("/").filter(Boolean);
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
      // Trang hiện tại: luôn hiển thị, không cần liên kết
      const label =
        matchTemplate(prefix, ROUTE_TEMPLATES) ??
        matchTemplate(prefix, GROUP_TEMPLATES) ??
        fallbackLabel;
      items.push({ label });
    } else {
      // Đoạn đường dẫn trung gian: chỉ thêm nếu là route thật và user có quyền truy cập
      const label = matchTemplate(prefix, ROUTE_TEMPLATES);
      if (label && (userRole === undefined || isRouteAccessible(prefix, userRole))) {
        items.push({
          label,
          href: prefix,
        });
      }
      // Các tiền tố nhóm (group templates) hoặc route không có quyền truy cập sẽ bị ẩn hoàn toàn
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
 * Tự động kiểm tra và lọc bỏ hoàn toàn các liên kết không tồn tại hoặc người dùng không có quyền truy cập.
 */
export function AppBreadcrumb() {
  const location = useLocation();
  const { user } = useAuth();
  const { override } = useContext(BreadcrumbOverrideContext);
  const rawItems =
    override ?? buildAutoBreadcrumb(location.pathname, user?.roleCode);

  // Lọc bỏ hoàn toàn các mục không thể truy cập (route không tồn tại hoặc user không có quyền),
  // luôn giữ lại mục cuối cùng (trang hiện tại).
  const items = useMemo(() => {
    return rawItems.filter((item, index) => {
      const isLast = index === rawItems.length - 1;
      if (isLast) return true;
      if (!item.href) return false;
      return isRouteAccessible(item.href, user?.roleCode);
    });
  }, [rawItems, user?.roleCode]);

  return <Breadcrumb items={items} />;
}
