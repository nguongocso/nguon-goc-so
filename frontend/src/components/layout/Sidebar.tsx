import type { ReactNode } from 'react';
import { Link, useLocation, useMatch } from 'react-router-dom';
import {
  BarChart2,
  Building2,
  Hash,
  LayoutDashboard,
  MapPinned,
  Package,
  Sprout,
  Truck,
  UserCheck,
  Users,
  X,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  ROLE_ACCESS,
  hasAnyRole,
  type AuthenticatedRoleCode,
} from '@/config/roleAccess';
import { useAuth } from '@/hooks/useAuth';
import { cn } from '@/lib/utils';

interface MenuItem {
  icon: ReactNode;
  label: string;
  href: string;
  allowedRoles: readonly AuthenticatedRoleCode[];
}

interface SidebarProps {
  onNavigate?: () => void;
  onClose?: () => void;
  showCloseButton?: boolean;
}

const MENU_ITEMS: MenuItem[] = [
  {
    icon: <LayoutDashboard className="h-5 w-5" />,
    label: 'Dashboard',
    href: '/',
    allowedRoles: ROLE_ACCESS.dashboard,
  },
  {
    icon: <Building2 className="h-5 w-5" />,
    label: 'Tổ chức',
    href: '/organizations',
    allowedRoles: ROLE_ACCESS.organizationList,
  },
  {
    icon: <UserCheck className="h-5 w-5" />,
    label: 'Hồ sơ tổ chức',
    href: '/organizations/profile',
    allowedRoles: ROLE_ACCESS.organizationProfile,
  },
  {
    icon: <MapPinned className="h-5 w-5" />,
    label: 'Tạo vùng trồng',
    href: '/farm-areas/create',
    allowedRoles: ROLE_ACCESS.farmAreaCreate,
  },
  {
    icon: <Package className="h-5 w-5" />,
    label: 'Lô sản xuất',
    href: '/production-lots',
    allowedRoles: ROLE_ACCESS.productionLotList,
  },
  {
    icon: <Truck className="h-5 w-5" />,
    label: "Ghi sự kiện vận chuyển",
    href: "/transport-events/record",
    allowedRoles: ROLE_ACCESS.transportEventRecord,
  },
  {
    icon: <Hash className='h-5 w-5' />,
    label: 'Quản lý dải mã',
    href: '/admin/code-ranges',
    allowedRoles: ROLE_ACCESS.codeRangeList,
  },
  {
    icon: <Users className="h-5 w-5" />,
    label: 'Quản lý thành viên',
    href: '/members',
    allowedRoles: ROLE_ACCESS.memberManagement,
  },
  {
  icon: <BarChart2 className="h-5 w-5" />,
  label: 'Thống kê tra cứu',
  href: '/reports/lookup-statistics',   // 👈 đổi thành đường dẫn này
  allowedRoles: ['VT-01', 'VT-02'] as const,
},
];

export function Sidebar({
  onNavigate,
  onClose,
  showCloseButton = false,
}: SidebarProps) {
  const { user } = useAuth();
  const location = useLocation();

  // 🔍 Debug: kiểm tra roleCode
  console.log('🔑 user.roleCode:', user?.roleCode);

  const visibleItems = MENU_ITEMS.filter((item) => {
    const hasAccess = hasAnyRole(user?.roleCode, item.allowedRoles);
    console.log(`📌 ${item.label} → ${hasAccess ? '✅' : '❌'}`);
    return hasAccess;
  });

  // Fallback nếu không có menu nào
  const finalItems = visibleItems.length === 0
    ? MENU_ITEMS.filter((item) => item.href === '/' || item.href === '/production-lots')
    : visibleItems;

  const isActive = (href: string) => {
  // Đối với route "/organizations" và "/organizations/profile" chúng ta muốn tách biệt
  if (href === '/organizations') {
    return location.pathname === '/organizations';
  }
  // Các route khác có thể dùng startsWith nếu cần
  return !!useMatch(href);
};

  return (
    <aside className="flex h-full min-h-0 flex-col border-r bg-background">
      <div className="flex h-16 items-center gap-2 border-b px-5">
        <Link
          to="/"
          onClick={onNavigate}
          className="flex min-w-0 flex-1 items-center gap-2 font-bold"
        >
          <Sprout className="h-6 w-6 shrink-0 text-primary" />
          <span className="truncate text-lg">Nguồn gốc số</span>
        </Link>
        {showCloseButton && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onClose}
            aria-label="Đóng menu"
          >
            <X className="h-5 w-5" />
          </Button>
        )}
      </div>
      <nav className="min-h-0 flex-1 space-y-1 overflow-y-auto px-3 py-4">
        {finalItems.map((item) => (
          <Link
            key={item.href}
            to={item.href}
            onClick={onNavigate}
            className={cn(
              'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
              isActive(item.href)
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground',
            )}
          >
            {item.icon}
            <span>{item.label}</span>
          </Link>
        ))}
        {finalItems.length === 0 && (
          <p className="px-3 py-2 text-sm text-muted-foreground">Không có menu</p>
        )}
      </nav>
    </aside>
  );
}