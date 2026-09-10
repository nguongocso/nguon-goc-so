import React, { type ReactNode, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  Activity,
  AlertTriangle,
  Award,
  Bell,
  BookOpen,
  Building2,
  CalendarCheck,
  FileSignature,
  FileText,
  Hash,
  History,
  FlaskConical,
  Layers,
  LayoutDashboard,
  Lock,
  LogOut,
  MapPinned,
  MessageSquare,
  Package,
  PackageCheck,
  PackageX,
  ScanLine,
  ShieldCheck,
  Truck,
  User,
  UserCheck,
  Users,
  Thermometer,
  Warehouse,
  X,
  TrendingUp,
  GitCompare,
  PieChart,
  Database,
  ChevronDown,
  Settings,
  ShoppingCart,
  Key,
  WifiOff,
  MapPin,
  SlidersHorizontal,
} from "lucide-react";
import { Logo } from "@/components/common/Logo";
import {
  AUTHENTICATED_ROLE_CODES,
  ROLE_ACCESS,
  hasAnyRole,
  type AuthenticatedRoleCode,
} from "@/config/roleAccess";
import { useAuth } from "@/hooks/useAuth";
import { cn } from "@/lib/utils";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

// ─── Types ───────────────────────────────────────────────

interface MenuItem {
  icon: ReactNode;
  label: string;
  /**
   * Đường dẫn điều hướng của mục lá. Mục cha dạng submenu (có `children`)
   * chỉ đóng/mở submenu, không điều hướng nên không cần `href`.
   */
  href?: string;
  allowedRoles: readonly AuthenticatedRoleCode[];
  activePaths?: string[];
  /**
   * Danh sách mục con của submenu cấp 2 (ví dụ "Yêu cầu thu hồi" nằm
   * trong "Vận hành sản xuất"). Chỉ hỗ trợ một cấp lồng nhau.
   */
  children?: MenuItem[];
}

interface MenuGroup {
  id: string;
  label: string;
  icon: ReactNode;
  items: MenuItem[];
}

interface BottomAction {
  icon: ReactNode;
  label: string;
  href?: string;
  onClick?: () => void;
  variant?: "default" | "danger";
  allowedRoles?: readonly AuthenticatedRoleCode[];
}

interface SidebarProps {
  onNavigate?: () => void;
  onClose?: () => void;
  showCloseButton?: boolean;
  collapsed?: boolean;
}

// ─── Menu Items Definition ───────────────────────────────

const DASHBOARD_ITEM: MenuItem = {
  icon: <LayoutDashboard className="h-5 w-5" />,
  label: "Dashboard",
  href: "/dashboard",
  allowedRoles: ROLE_ACCESS.dashboard,
};

const MENU_GROUPS: MenuGroup[] = [
  // ── Quản lý ──────────────────────────
  {
    id: "management",
    label: "Quản lý",
    icon: <Building2 className="h-5 w-5" />,
    items: [
      {
        icon: <Building2 className="h-5 w-5" />,
        label: "Tổ chức",
        href: "/organizations",
        allowedRoles: ROLE_ACCESS.organizationList,
      },
      {
        icon: <Users className="h-5 w-5" />,
        label: "Quản lý thành viên",
        href: "/members",
        allowedRoles: ROLE_ACCESS.memberManagement,
        activePaths: ["/members", "/invitations/create"],
      },
      {
        icon: <ShieldCheck className="h-5 w-5" />,
        label: "Cấu hình phân quyền",
        href: "/permissions/config",
        allowedRoles: ROLE_ACCESS.rolePermissionConfig,
        activePaths: ["/permissions/config"],
      },
      {
        icon: <Layers className="h-5 w-5" />,
        label: "Danh mục nông sản",
        href: "/admin/product-categories",
        allowedRoles: ["VT-01"] as const,
      },
      {
        icon: <FlaskConical className="h-5 w-5" />,
        label: "Chỉ tiêu kiểm nghiệm",
        href: "/admin/inspection-criteria",
        allowedRoles: ROLE_ACCESS.inspectionCriteriaManagement,
      },
      {
        icon: <CalendarCheck className="h-5 w-5" />,
        label: "Mốc canh tác",
        href: "/admin/cultivation-milestones",
        allowedRoles: ROLE_ACCESS.cultivationMilestoneManagement,
      },
      {
        icon: <ShieldCheck className="h-5 w-5" />,
        label: "Đơn vị kiểm nghiệm",
        href: "/admin/testing-units",
        allowedRoles: ROLE_ACCESS.testingUnitScopeManagement,
      },
      {
        icon: <PackageCheck className="h-5 w-5" />,
        label: "Danh mục vật tư",
        href: "/admin/input-materials",
        allowedRoles: ["VT-01"] as const,
      },
      {
        icon: <BookOpen className="h-5 w-5" />,
        label: "Tiêu chuẩn chất lượng",
        href: "/admin/standards",
        allowedRoles: ROLE_ACCESS.standardManagement,
      },
      {
        icon: <ShieldCheck className="h-5 w-5" />,
        label: "Xác thực chứng nhận",
        href: "/admin/certifications",
        allowedRoles: ROLE_ACCESS.certificateVerification,
      },
      {
        icon: <Award className="h-5 w-5" />,
        label: "Chứng nhận",
        href: "/certifications",
        allowedRoles: ["VT-02"] as const,
      },
      {
        icon: <Key className="h-5 w-5" />,
        label: "Khóa API bên thứ ba",
        href: "/integration/api-keys",
        allowedRoles: ROLE_ACCESS.apiKeyManagement,
      },
      {
        icon: <Hash className="h-5 w-5" />,
        label: "Quản lý dải mã truy xuất",
        href: "/admin/code-ranges",
        allowedRoles: ROLE_ACCESS.codeRangeList,
        activePaths: [
          "/admin/code-ranges",
          "/admin/code-range-supplements",
        ],
      },
      {
        icon: <Lock className="h-5 w-5" />,
        label: "Tem nghi vấn",
        href: "/admin/suspect-trace-codes",
        allowedRoles: ["VT-01"] as const,
      },
      {
        icon: <SlidersHorizontal className="h-5 w-5" />,
        label: "Ngưỡng quét bất thường",
        href: "/admin/anomaly-thresholds",
        allowedRoles: ROLE_ACCESS.anomalyThresholdConfig,
      },
      {
        icon: <MapPin className="h-5 w-5" />,
        label: "Phân công địa bàn",
        href: "/admin/account-areas",
        allowedRoles: ROLE_ACCESS.areaAssignment,
      },
      {
        icon: <MessageSquare className="h-5 w-5" />,
        label: "Nhận phản ánh",
        href: "/product-feedbacks",
        allowedRoles: ROLE_ACCESS.productFeedbackManagement,
      },
    ],
  },

  // ── Vận hành sản xuất ─────────────────
  {
    id: "operations",
    label: "Vận hành sản xuất",
    icon: <Package className="h-5 w-5" />,
    items: [
      {
        icon: <MapPinned className="h-5 w-5" />,
        label: "Vùng trồng",
        href: "/farm-areas",
        allowedRoles: ["VT-02"] as const,
      },
      {
        icon: <Package className="h-5 w-5" />,
        label: "Lô sản xuất",
        href: "/production-lots",
        allowedRoles: ROLE_ACCESS.productionLotList,
        activePaths: [
          "/production-lots",
          "/packaging-events/create",
          "/production-lots/import",
          "/shipments/",
          "/farm-logs/", 
        ],
      },
      {
        icon: <Activity className="h-5 w-5" />,
        label: "Bảng tiến độ chuỗi",
        href: "/chain-progress",
        allowedRoles: ["VT-01", "VT-02", "VT-03"] as const,
      },
      {
        icon: <Truck className="h-5 w-5" />,
        label: "Ghi sự kiện vận chuyển",
        href: "/transport-events/record",
        allowedRoles: ROLE_ACCESS.transportEventRecord,
      },
      {
        icon: <ScanLine className="h-5 w-5" />,
        label: "Quét mã ghi sự kiện nhanh",
        href: "/chain-events/scan",
        allowedRoles: ROLE_ACCESS.scanQuickEvent,
      },
      {
        icon: <Thermometer className="h-5 w-5" />,
        label: "Bảo quản",
        href: "/storage-condition",
        allowedRoles: ROLE_ACCESS.storageCondition,
      },
      {
        icon: <AlertTriangle className="h-5 w-5" />,
        label: "Cảnh báo tem bất thường",
        href: "/alerts/scan-anomaly",
        allowedRoles: ROLE_ACCESS.scanAnomalyAlerts,
      },
      {
        icon: <AlertTriangle className="h-5 w-5" />,
        label: "Nhật ký lỗi sự kiện",
        href: "/failed-event-logs",
        allowedRoles: ["VT-02", "VT-03"] as const,
      },
      {
        icon: <WifiOff className="h-5 w-5" />,
        label: "Sự kiện chờ đồng bộ",
        href: "/offline-events",
        allowedRoles: AUTHENTICATED_ROLE_CODES,
      },
      {
        icon: <AlertTriangle className="h-5 w-5" />,
        label: "Tạo yêu cầu thu hồi",
        href: "/recall-requests/create",
        allowedRoles: ROLE_ACCESS.recallRequestCreate,
        activePaths: ["/recall-requests/create"],
      },
      // NCL-04-CN-007: mục "Yêu cầu bổ sung mã" đã bỏ — chức năng chuyển thành
      // tùy chọn trong tab "Lô hàng & Mã QR" của chi tiết lô sản xuất.
      {
        icon: <GitCompare className="h-5 w-5" />,
        label: "Truy vết phạm vi ảnh hưởng",
        href: "/trace/impact-scope",
        allowedRoles: ROLE_ACCESS.impactScopeTrace,
        activePaths: ["/trace/impact-scope"],
      },
      {
        icon: <PackageX className="h-5 w-5" />,
        label: "Yêu cầu thu hồi",
        allowedRoles: ROLE_ACCESS.recallRequestManage,
        children: [
          {
            icon: <PackageX className="h-5 w-5" />,
            label: "Yêu cầu thu hồi từng lô hàng",
            href: "/recall-requests",
            allowedRoles: ROLE_ACCESS.recallRequestManage,
            activePaths: ["/recall-requests"],
          },
          {
            icon: <PackageX className="h-5 w-5" />,
            label: "Yêu cầu thu hồi lô hàng theo phạm vi",
            href: "/recall-requests/bulk",
            allowedRoles: ROLE_ACCESS.recallRequestManage,
            activePaths: ["/recall-requests/bulk"],
          },
        ],
      },
    ],
  },

  // ── Thống kê & Báo cáo ────────────────
  {
    id: "reports",
    label: "Thống kê & Báo cáo",
    icon: <PieChart className="h-5 w-5" />,
    items: [
      {
        icon: <PieChart className="h-5 w-5" />,
        label: "Thống kê tra cứu",
        href: "/reports/lookup-statistics",
        allowedRoles: ["VT-01", "VT-02"] as const,
      },
      {
        icon: <Activity className="h-5 w-5" />,
        label: "Phân tích vùng trồng",
        href: "/reports/crop-area-analysis",
        allowedRoles: ["VT-01", "VT-05"] as const,
      },
      {
        icon: <GitCompare className="h-5 w-5" />,
        label: "So sánh mùa vụ",
        href: "/reports/season-yield-comparison",
        allowedRoles: ROLE_ACCESS.seasonYieldComparison,
      },
      {
        icon: <TrendingUp className="h-5 w-5" />,
        label: "Báo cáo ngành",
        href: "/reports/industry",
        allowedRoles: ["VT-05"] as const,
      },
      {
        icon: <FileText className="h-5 w-5" />,
        label: "Xuất dữ liệu mở",
        href: "/export/open-data",
        allowedRoles: ["VT-05"] as const,
      },
      {
        icon: <FileSignature className="h-5 w-5" />,
        label: "Phiếu bàn giao nhận",
        href: "/shipment-handovers/received",
        allowedRoles: ROLE_ACCESS.handoverReceivedView,
      },
      {
        icon: <Truck className="h-5 w-5" />,
        label: "Phiếu bàn giao đã gửi",
        href: "/shipment-handovers/sent",
        allowedRoles: ROLE_ACCESS.handoverSentView,
      },
    ],
  },

  // ── Thu mua ──────────────────────────
  {
    id: "procurement",
    label: "Thu mua",
    icon: <ShoppingCart className="h-5 w-5" />,
    items: [

{
          icon: <Warehouse className="h-5 w-5" />,
          label: "Nhập kho",
          href: "/warehouse-receipt",
          allowedRoles: ROLE_ACCESS.warehouseReceipt,
        },
    ],
  },

  // ── Hệ thống ──────────────────────────
  {
    id: "system",
    label: "Hệ thống",
    icon: <Settings className="h-5 w-5" />,
    items: [
      {
        icon: <ShieldCheck className="h-5 w-5" />,
        label: "Kiểm chứng dòng sự kiện",
        href: "/event-chain-verification",
        allowedRoles: ROLE_ACCESS.eventChainVerification,
      },
      {
        icon: <History className="h-5 w-5" />,
        label: "Lịch sử hoạt động",
        href: "/activity-logs",
        allowedRoles: ["VT-02"] as const,
      },
      {
        icon: <ShieldCheck className="h-5 w-5" />,
        label: "Lịch sử đăng nhập",
        href: "/login-history",
        allowedRoles: AUTHENTICATED_ROLE_CODES,
      },
      {
        icon: <AlertTriangle className="h-5 w-5" />,
        label: "Theo dõi đăng nhập bất thường",
        href: "/login-anomalies",
        allowedRoles: ["VT-01"] as const,
      },
      {
        icon: <Database className="h-5 w-5" />,
        label: "Sao lưu & Phục hồi dữ liệu",
        href: "/admin/backup-restore",
        allowedRoles: ["VT-01"] as const,
      },
      {
        icon: <Activity className="h-5 w-5" />,
        label: "Giám sát hệ thống",
        href: "/admin/system-monitoring",
        allowedRoles: ["VT-01"] as const,
      },
      {
        icon: <User className="h-5 w-5" />,
        label: "Hồ sơ người dùng",
        href: "/profile",
        allowedRoles: ROLE_ACCESS.userProfile,
      },
      {
        icon: <UserCheck className="h-5 w-5" />,
        label: "Hồ sơ tổ chức",
        href: "/organizations/profile",
        allowedRoles: ROLE_ACCESS.organizationProfile,
      },
    ],
  },
];

// ─── Helpers ─────────────────────────────────────────────

function filterVisibleItems(items: MenuItem[], userRole?: string): MenuItem[] {
  const result: MenuItem[] = [];
  for (const item of items) {
    if (!hasAnyRole(userRole, item.allowedRoles)) {
      continue;
    }
    if (item.children && item.children.length > 0) {
      const visibleChildren = item.children.filter((child) =>
        hasAnyRole(userRole, child.allowedRoles),
      );
      if (visibleChildren.length === 0) {
        continue;
      }
      result.push({ ...item, children: visibleChildren });
    } else {
      result.push(item);
    }
  }
  return result;
}

/**
 * Thu thập toàn bộ mục lá (có `href`) kể cả mục nằm trong submenu cấp 2,
 * dùng cho việc xác định route active và render khi sidebar thu gọn.
 */
function collectLeafItems(items: MenuItem[]): MenuItem[] {
  const result: MenuItem[] = [];
  for (const item of items) {
    if (item.children && item.children.length > 0) {
      result.push(...collectLeafItems(item.children));
    } else if (item.href) {
      result.push(item);
    }
  }
  return result;
}

/** Danh sách path dùng để xác định active của một mục lá. */
function getItemPaths(item: MenuItem): string[] {
  if (!item.href) {
    return item.activePaths ?? [];
  }
  return item.activePaths ? [item.href, ...item.activePaths] : [item.href];
}

function filterVisibleGroups(
  groups: MenuGroup[],
  userRole?: string,
): MenuGroup[] {
  return groups
    .map((group) => ({
      ...group,
      items: filterVisibleItems(group.items, userRole),
    }))
    .filter((group) => group.items.length > 0);
}

// ─── Sub-components ──────────────────────────────────────

/** Single flat menu link (used for Dashboard and items inside accordion groups). */
function MenuLink({
  item,
  collapsed,
  isActive,
  onNavigate,
  showWarningDot = false,
}: {
  item: MenuItem;
  collapsed: boolean;
  isActive: boolean;
  onNavigate?: () => void;
  showWarningDot?: boolean;
}) {
  const linkContent = item.href ? (
    <Link
      to={item.href}
      onClick={onNavigate}
      className={cn(
        "flex items-center gap-3 rounded-lg text-sm font-medium transition-all duration-200",
        collapsed ? "justify-center px-0 py-3" : "px-3 py-2.5",
        isActive
          ? "bg-emerald-700 text-white shadow-sm shadow-emerald-200"
          : "text-muted-foreground hover:bg-emerald-50 hover:text-emerald-700",
      )}
      aria-label={collapsed ? item.label : undefined}
    >
      <span
        className={cn(
          "relative flex-shrink-0",
          isActive ? "text-white" : "text-emerald-500",
        )}
      >
        {item.icon}
        {collapsed && showWarningDot && (
          <span
            className="absolute -top-1 -right-1 size-2 rounded-full bg-red-500 ring-2 ring-white"
            title="Chưa cập nhật email"
          />
        )}
      </span>
      {!collapsed && (
        <span className="flex flex-1 items-center justify-between gap-2">
          <span>{item.label}</span>
          {showWarningDot && (
            <span
              className="size-2 rounded-full bg-red-500 ring-2 ring-white"
              title="Chưa cập nhật email"
            />
          )}
        </span>
      )}
    </Link>
  ) : null;

  if (!linkContent) {
    return null;
  }

  const itemKey = item.href ?? item.label;

  if (collapsed) {
    return (
      <Tooltip key={itemKey} side="right">
        <TooltipTrigger asChild>{linkContent}</TooltipTrigger>
        <TooltipContent>{item.label}</TooltipContent>
      </Tooltip>
    );
  }

  return <span key={itemKey}>{linkContent}</span>;
}

/**
 * Submenu cấp 2 nằm bên trong một nhóm accordion (ví dụ "Yêu cầu thu hồi"
 * nằm trong "Vận hành sản xuất"). Menu cha chỉ đóng/mở submenu, mục con
 * điều hướng tới route của từng chức năng.
 */
function SubMenu({
  item,
  isActive,
  onNavigate,
  defaultExpanded = false,
}: {
  item: MenuItem;
  isActive: (child: MenuItem) => boolean;
  onNavigate?: () => void;
  defaultExpanded?: boolean;
}) {
  const [expanded, setExpanded] = React.useState(defaultExpanded);

  const subActive = (item.children ?? []).some((child) => isActive(child));

  // Tự động mở submenu khi một mục con trở thành active (kể cả F5 trực tiếp URL).
  React.useEffect(() => {
    if (subActive) {
      setExpanded(true);
    }
  }, [subActive]);

  if (!item.children || item.children.length === 0) {
    return null;
  }

  return (
    <div className="rounded-lg overflow-hidden">
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        className={cn(
          "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200",
          subActive
            ? "bg-emerald-50 text-emerald-700"
            : "text-muted-foreground hover:bg-emerald-50 hover:text-emerald-700",
        )}
        aria-expanded={expanded}
        aria-label={item.label}
        title={item.label}
      >
        <span className="flex-shrink-0 text-emerald-500">{item.icon}</span>
        <span className="flex-1 text-left">{item.label}</span>
        <span
          className="flex-shrink-0 text-emerald-400 transition-transform duration-200"
          style={{ transform: expanded ? "rotate(180deg)" : "rotate(0deg)" }}
        >
          <ChevronDown className="h-4 w-4" />
        </span>
      </button>
      <div
        className={cn(
          "grid transition-all duration-300 ease-in-out",
          expanded
            ? "grid-rows-[1fr] opacity-100"
            : "grid-rows-[0fr] opacity-0",
        )}
      >
        <div className="overflow-hidden">
          <div className="space-y-0.5 py-1 pl-4 pr-1">
            {item.children.map((child) => (
              <MenuLink
                key={child.href ?? child.label}
                item={child}
                collapsed={false}
                isActive={isActive(child)}
                onNavigate={onNavigate}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Accordion Group Component ───────────────────────────

function AccordionGroup({
  group,
  isActive,
  isGroupActive,
  onNavigate,
  defaultExpanded = false,
  isMissingEmail = false,
}: {
  group: MenuGroup;
  isActive: (item: MenuItem) => boolean;
  isGroupActive: boolean;
  onNavigate?: () => void;
  defaultExpanded?: boolean;
  isMissingEmail?: boolean;
}) {
  const [expanded, setExpanded] = React.useState(defaultExpanded);
  const hasMissingEmailChild =
    isMissingEmail && group.items.some((it) => it.href === "/profile");

  // Auto-expand when a child becomes active
  React.useEffect(() => {
    if (isGroupActive) {
      setExpanded(true);
    }
  }, [isGroupActive]);

  return (
    <div className="rounded-lg overflow-hidden">
      {/* Group header – clickable to expand/collapse */}
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        className={cn(
          "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200",
          isGroupActive
            ? "bg-emerald-50 text-emerald-700"
            : "text-muted-foreground hover:bg-emerald-50 hover:text-emerald-700",
        )}
        aria-expanded={expanded}
      >
        <span className="flex-shrink-0 text-emerald-500">{group.icon}</span>
        <span className="flex-1 text-left">{group.label}</span>
        {!expanded && hasMissingEmailChild && (
          <span
            className="size-2 rounded-full bg-red-500 ring-2 ring-white"
            title="Chưa cập nhật email"
          />
        )}
        <span
          className="flex-shrink-0 text-emerald-400 transition-transform duration-200"
          style={{ transform: expanded ? "rotate(180deg)" : "rotate(0deg)" }}
        >
          <ChevronDown className="h-4 w-4" />
        </span>
      </button>

      {/* Collapsible child items */}
      <div
        className={cn(
          "grid transition-all duration-300 ease-in-out",
          expanded
            ? "grid-rows-[1fr] opacity-100"
            : "grid-rows-[0fr] opacity-0",
        )}
      >
        <div className="overflow-hidden">
          <div className="space-y-0.5 py-1 pl-4 pr-1">
            {group.items.map((item) =>
              item.children && item.children.length > 0 ? (
                <SubMenu
                  key={item.label}
                  item={item}
                  isActive={isActive}
                  onNavigate={onNavigate}
                  defaultExpanded={item.children.some((child) =>
                    isActive(child),
                  )}
                />
              ) : (
                <MenuLink
                  key={item.href ?? item.label}
                  item={item}
                  collapsed={false}
                  isActive={isActive(item)}
                  onNavigate={onNavigate}
                  showWarningDot={item.href === "/profile" && isMissingEmail}
                />
              ),
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Main Sidebar Component ──────────────────────────────

export function Sidebar({
  onNavigate,
  onClose,
  showCloseButton = false,
  collapsed = false,
}: SidebarProps) {
  const { user, logout } = useAuth();
  const location = useLocation();

  const [showLogoutDialog, setShowLogoutDialog] = useState(false);

  const handleLogout = () => {
    if (onNavigate) onNavigate();
    logout();
    setShowLogoutDialog(false);
  };

  const isMissingEmail = Boolean(
    user &&
    hasAnyRole(user.roleCode, ROLE_ACCESS.userProfile) &&
    (!user.email || user.email.trim() === "")
  );

  const visibleGroups = filterVisibleGroups(MENU_GROUPS, user?.roleCode);
  const dashboardVisible = hasAnyRole(
    user?.roleCode,
    DASHBOARD_ITEM.allowedRoles,
  );

  // Flatten all visible leaf items (kể cả mục trong submenu cấp 2) for active-route detection
  const allVisibleItems: MenuItem[] = [
    ...(dashboardVisible ? [DASHBOARD_ITEM] : []),
    ...visibleGroups.flatMap((g) => collectLeafItems(g.items)),
  ];

  const isActiveLeaf = (item: MenuItem) => {
    const matchedItems = allVisibleItems.filter((menuItem) => {
      const paths = getItemPaths(menuItem);
      return paths.some(
        (path) => path && location.pathname.startsWith(path),
      );
    });
    if (matchedItems.length === 0) return false;
    const longestMatch = matchedItems.reduce((a, b) =>
      (a.href?.length ?? 0) > (b.href?.length ?? 0) ? a : b,
    );
    return longestMatch.href === item.href;
  };

  const isActive = (item: MenuItem) => {
    if (item.children && item.children.length > 0) {
      return item.children.some((child) => isActiveLeaf(child));
    }
    if (!item.href) {
      return false;
    }
    return isActiveLeaf(item);
  };

  /** Check if any item in a group is active (to auto-expand accordion). */
  const isGroupActive = (group: MenuGroup) =>
    collectLeafItems(group.items).some((item) => isActiveLeaf(item));

  const sidebarWidth = collapsed ? "w-[4.5rem]" : "w-[17rem]";

  // ── Bottom actions ──────────────────────
  const bottomActions: BottomAction[] = [
    {
      icon: <Bell className="h-5 w-5" />,
      label: "Thông báo",
      href: "/notifications",
      variant: "default",
      allowedRoles: ["VT-01", "VT-02", "VT-03", "VT-04", "VT-05"] as const,
    },
    {
      icon: <LogOut className="h-5 w-5" />,
      label: "Đăng xuất",
      onClick: () => setShowLogoutDialog(true),
      variant: "danger",
    },
  ];

  const visibleBottomActions = bottomActions.filter((action) => {
    if (!action.allowedRoles) return true;
    return hasAnyRole(user?.roleCode, action.allowedRoles);
  });

  const hasAnyVisibleItem = dashboardVisible || visibleGroups.length > 0;

  return (
    <>
      <aside
        className={cn(
          "flex h-full min-h-0 flex-col border-r border-emerald-100 bg-white/90 backdrop-blur-sm transition-all duration-300 ease-in-out",
          sidebarWidth,
        )}
      >
      {/* ── Header / Logo ─────────────────── */}
      <div
        className={cn(
          "flex h-16 items-center border-b border-emerald-100 transition-all duration-300",
          collapsed ? "justify-center px-2" : "px-5",
        )}
      >
        <Link
          to="/dashboard"
          onClick={onNavigate}
          className="flex min-w-0 flex-1 items-center overflow-hidden"
        >
          <Logo height={40} />
        </Link>
        {showCloseButton && (
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng menu"
            className="text-muted-foreground hover:text-emerald-700 shrink-0"
          >
            <X className="h-5 w-5" />
          </button>
        )}
      </div>

      {/* ── Navigation ────────────────────── */}
      <nav className="min-h-0 flex-1 overflow-y-auto px-3 py-4">
        {!hasAnyVisibleItem && !collapsed && (
          <p className="px-3 py-2 text-sm text-muted-foreground">
            Không có menu
          </p>
        )}

        {/* Dashboard – always first, standalone */}
        {dashboardVisible && (
          <div className="mb-1">
            <MenuLink
              item={DASHBOARD_ITEM}
              collapsed={collapsed}
              isActive={isActive(DASHBOARD_ITEM)}
              onNavigate={onNavigate}
              showWarningDot={DASHBOARD_ITEM.href === "/profile" && isMissingEmail}
            />
          </div>
        )}

        {/* Collapsible menu groups */}
        {visibleGroups.map((group) => {
          const groupActive = isGroupActive(group);

          if (collapsed) {
            // Collapsed: show leaf items individually with tooltips.
            // Mục cha dạng submenu (có `children`) được trải phẳng thành
            // các mục lá để không mất icon/active/tooltip khi thu gọn.
            const collapsedItems = group.items.flatMap((item) =>
              item.children && item.children.length > 0
                ? item.children
                : [item],
            );
            return (
              <div key={group.id} className="mb-1 space-y-1">
                {/* Small separator dot to hint at group boundaries */}
                <div className="flex justify-center py-1">
                  <div className="h-1 w-5 rounded-full bg-emerald-100" />
                </div>
                {collapsedItems.map((item) => (
                  <MenuLink
                    key={item.href ?? item.label}
                    item={item}
                    collapsed={collapsed}
                    isActive={isActive(item)}
                    onNavigate={onNavigate}
                    showWarningDot={item.href === "/profile" && isMissingEmail}
                  />
                ))}
              </div>
            );
          }

          // Expanded: accordion group
          return (
            <div key={group.id} className="mb-1">
              <AccordionGroup
                group={group}
                isActive={isActive}
                isGroupActive={groupActive}
                onNavigate={onNavigate}
                defaultExpanded={groupActive}
                isMissingEmail={isMissingEmail}
              />
            </div>
          );
        })}
      </nav>

      {/* ── Bottom Actions ────────────────── */}
      <div className="border-t border-emerald-50 px-3 py-3 space-y-1">
        {visibleBottomActions.map((action) => {
          const btnContent = (
            <>
              <span
                className={cn(
                  "flex-shrink-0",
                  action.variant === "danger"
                    ? "text-red-400"
                    : "text-emerald-500",
                )}
              >
                {action.icon}
              </span>
              {!collapsed && <span>{action.label}</span>}
            </>
          );

          const btnClasses = cn(
            "flex w-full items-center gap-3 rounded-lg text-sm font-medium transition-colors",
            collapsed ? "justify-center px-0 py-3" : "px-3 py-2.5",
            action.variant === "danger"
              ? "text-muted-foreground hover:bg-red-50 hover:text-red-600"
              : "text-muted-foreground hover:bg-emerald-50 hover:text-emerald-700",
          );

          // Render as Link or button
          const btn =
            action.href && !action.onClick ? (
              <Link
                key={action.label}
                to={action.href}
                onClick={onNavigate}
                className={btnClasses}
                aria-label={collapsed ? action.label : undefined}
              >
                {btnContent}
              </Link>
            ) : (
              <button
                key={action.label}
                type="button"
                onClick={action.onClick}
                className={btnClasses}
                aria-label={collapsed ? action.label : undefined}
              >
                {btnContent}
              </button>
            );

          if (collapsed) {
            return (
              <Tooltip key={action.label} side="right">
                <TooltipTrigger asChild>{btn}</TooltipTrigger>
                <TooltipContent>{action.label}</TooltipContent>
              </Tooltip>
            );
          }

          return btn;
        })}
      </div>
    </aside>

    {/* Logout confirmation dialog */}
    <AlertDialog open={showLogoutDialog} onOpenChange={setShowLogoutDialog}>
      <AlertDialogPopup>
        <AlertDialogHeader>
          <AlertDialogTitle>Xác nhận đăng xuất</AlertDialogTitle>
          <AlertDialogDescription>
            Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel
            onClick={() => setShowLogoutDialog(false)}
            className="border border-gray-300 bg-white text-gray-700 hover:bg-gray-50"
          >
            Hủy
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleLogout}
            className="bg-blue-600 hover:bg-blue-700 text-white"
          >
            Đăng xuất
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
    </>
  );
}
