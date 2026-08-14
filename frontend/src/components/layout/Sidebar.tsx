import React, { type ReactNode, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  AlertTriangle,
  Award,
  Bell,
  BookOpen,
  Building2,
  FileText,
  Hash,
  History,
  Layers,
  LayoutDashboard,
  Lock,
  LogOut,
  MapPinned,
  MessageSquare,
  Package,
  ScanLine,
  ShieldCheck,
  Truck,
  UserCheck,
  Users,
  Thermometer,
  Warehouse,
  X,
  TrendingUp,
  Activity,
  GitCompare,
  PieChart,
  Database,
  ChevronDown,
  Settings,
  ShoppingCart,
} from "lucide-react";
import { Logo } from "@/components/common/Logo";
import {
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
  href: string;
  allowedRoles: readonly AuthenticatedRoleCode[];
  activePaths?: string[];
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
        icon: <Layers className="h-5 w-5" />,
        label: "Danh mục nông sản",
        href: "/admin/product-categories",
        allowedRoles: ["VT-01"] as const,
      },
      {
        icon: <BookOpen className="h-5 w-5" />,
        label: "Tiêu chuẩn chất lượng",
        href: "/admin/standards",
        allowedRoles: ROLE_ACCESS.standardManagement,
      },
      {
        icon: <Award className="h-5 w-5" />,
        label: "Chứng nhận",
        href: "/certifications",
        allowedRoles: ["VT-02"] as const,
      },
      {
        icon: <Hash className="h-5 w-5" />,
        label: "Quản lý dải mã",
        href: "/admin/code-ranges",
        allowedRoles: ROLE_ACCESS.codeRangeList,
      },
      {
        icon: <Lock className="h-5 w-5" />,
        label: "Tem nghi vấn",
        href: "/admin/suspect-trace-codes",
        allowedRoles: ["VT-01"] as const,
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
        ],
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
        label: "Điều kiện bảo quản",
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
        icon: <AlertTriangle className="h-5 w-5" />,
        label: "Tạo yêu cầu thu hồi",
        href: "/recall-requests/create",
        allowedRoles: ROLE_ACCESS.recallRequestCreate,
        activePaths: ["/recall-requests/create"],
      },
      {
        icon: <AlertTriangle className="h-5 w-5" />,
        label: "Danh sách yêu cầu thu hồi",
        href: "/recall-requests",
        allowedRoles: ROLE_ACCESS.recallRequestManage,
        activePaths: ["/recall-requests"],
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
    ],
  },

  // ── Thu mua ──────────────────────────
  {
    id: "procurement",
    label: "Thu mua",
    icon: <ShoppingCart className="h-5 w-5" />,
    items: [
      {
        icon: <ShoppingCart className="h-5 w-5" />,
        label: "Ghi sự kiện thu mua",
        href: "/procurement-event",
        allowedRoles: ROLE_ACCESS.procurementEvent,
      },
      {
        icon: <Warehouse className="h-5 w-5" />,
        label: "Nhập kho & đối chiếu",
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
        icon: <Database className="h-5 w-5" />,
        label: "Sao lưu & Phục hồi dữ liệu",
        href: "/admin/backup-restore",
        allowedRoles: ["VT-01"] as const,
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
  return items.filter((item) => hasAnyRole(userRole, item.allowedRoles));
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
}: {
  item: MenuItem;
  collapsed: boolean;
  isActive: boolean;
  onNavigate?: () => void;
}) {
  const linkContent = (
    <Link
      to={item.href}
      onClick={onNavigate}
      className={cn(
        "flex items-center gap-3 rounded-lg text-sm font-medium transition-all duration-200",
        collapsed ? "justify-center px-0 py-3" : "px-3 py-2.5",
        isActive
          ? "bg-emerald-600 text-white shadow-sm shadow-emerald-200"
          : "text-muted-foreground hover:bg-emerald-50 hover:text-emerald-700",
      )}
      aria-label={collapsed ? item.label : undefined}
    >
      <span
        className={cn(
          "flex-shrink-0",
          isActive ? "text-white" : "text-emerald-500",
        )}
      >
        {item.icon}
      </span>
      {!collapsed && <span>{item.label}</span>}
    </Link>
  );

  if (collapsed) {
    return (
      <Tooltip key={item.href}>
        <TooltipTrigger asChild>{linkContent}</TooltipTrigger>
        <TooltipContent className="z-[60]">{item.label}</TooltipContent>
      </Tooltip>
    );
  }

  return <span key={item.href}>{linkContent}</span>;
}

// ─── Accordion Group Component ───────────────────────────

function AccordionGroup({
  group,
  isActive,
  isGroupActive,
  onNavigate,
  defaultExpanded = false,
}: {
  group: MenuGroup;
  isActive: (item: MenuItem) => boolean;
  isGroupActive: boolean;
  onNavigate?: () => void;
  defaultExpanded?: boolean;
}) {
  const [expanded, setExpanded] = React.useState(defaultExpanded);

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
            {group.items.map((item) => (
              <MenuLink
                key={item.href}
                item={item}
                collapsed={false}
                isActive={isActive(item)}
                onNavigate={onNavigate}
              />
            ))}
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

  const visibleGroups = filterVisibleGroups(MENU_GROUPS, user?.roleCode);
  const dashboardVisible = hasAnyRole(
    user?.roleCode,
    DASHBOARD_ITEM.allowedRoles,
  );

  // Flatten all visible items for active-route detection
  const allVisibleItems: MenuItem[] = [
    ...(dashboardVisible ? [DASHBOARD_ITEM] : []),
    ...visibleGroups.flatMap((g) => g.items),
  ];

  const isActive = (item: MenuItem) => {
    const matchedItems = allVisibleItems.filter((menuItem) => {
      const paths = menuItem.activePaths
        ? [menuItem.href, ...menuItem.activePaths]
        : [menuItem.href];
      return paths.some((path) => location.pathname.startsWith(path));
    });
    if (matchedItems.length === 0) return false;
    const longestMatch = matchedItems.reduce((a, b) =>
      a.href.length > b.href.length ? a : b,
    );
    return longestMatch.href === item.href;
  };

  /** Check if any item in a group is active (to auto-expand accordion). */
  const isGroupActive = (group: MenuGroup) =>
    group.items.some((item) => isActive(item));

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

      {/* ── User Info (when expanded) ─────── */}
      {!collapsed && user && (
        <div className="border-b border-emerald-50 px-4 py-3">
          <div className="flex items-center gap-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-sm font-medium text-emerald-700">
              {(user.fullName || user.username || "U").charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-foreground">
                {user.fullName || user.username || "Người dùng"}
              </p>
              <p className="truncate text-xs text-muted-foreground">
                {user.organizationName || user.roleCode || ""}
              </p>
            </div>
          </div>
        </div>
      )}

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
            />
          </div>
        )}

        {/* Collapsible menu groups */}
        {visibleGroups.map((group) => {
          const groupActive = isGroupActive(group);

          if (collapsed) {
            // Collapsed: show items individually with tooltips
            return (
              <div key={group.id} className="mb-1 space-y-1">
                {/* Small separator dot to hint at group boundaries */}
                <div className="flex justify-center py-1">
                  <div className="h-1 w-5 rounded-full bg-emerald-100" />
                </div>
                {group.items.map((item) => (
                  <MenuLink
                    key={item.href}
                    item={item}
                    collapsed={collapsed}
                    isActive={isActive(item)}
                    onNavigate={onNavigate}
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
              <Tooltip key={action.label}>
                <TooltipTrigger asChild>{btn}</TooltipTrigger>
                <TooltipContent className="z-[60]">
                  {action.label}
                </TooltipContent>
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
