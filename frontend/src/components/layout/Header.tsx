import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { NotificationBell } from '@/components/notification/NotificationBell';
import { SyncBadge } from '@/components/layout/SyncBadge';
import { ROLE_ACCESS, getRoleLabel, hasAnyRole } from '@/config/roleAccess';
import { useAuth } from '@/hooks/useAuth';
import { Check, LogOut, Menu, User } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Logo } from '@/components/common/Logo';
import { cn } from '@/lib/utils';
import {
  getMyOrganizations,
  switchOrganization,
} from '@/api/authApi';
import type { OrganizationSelection } from '@/types/organization';
import { toast } from 'sonner';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogPopup,
} from '@/components/ui/alert-dialog';

interface HeaderProps {
  onMenuClick?: () => void;
  /** Current viewport is mobile (< 768px) */
  isMobile?: boolean;
  /** Current viewport is tablet (768px - 1279px) */
  isTablet?: boolean;
}

export function Header({ onMenuClick, isMobile = false, isTablet = false }: HeaderProps) {
  const { user, logout, completeLogin } = useAuth();
  const navigate = useNavigate();
  const roleLabel = getRoleLabel(user?.roleCode);
  const canOpenOrganizationProfile = hasAnyRole(
    user?.roleCode,
    ROLE_ACCESS.organizationProfile,
  );

  const [showLogoutDialog, setShowLogoutDialog] = useState(false);
  const [organizations, setOrganizations] = useState<OrganizationSelection[]>([]);
  const [isSwitchingOrganization, setIsSwitchingOrganization] = useState(false);

  useEffect(() => {
    if (!user) return;

    const loadOrganizations = async () => {
      try {
        const response = await getMyOrganizations();
        if (response.success) {
          setOrganizations(response.data ?? []);
        }
      } catch {
        // The current session remains usable even if the switch list fails.
      }
    };

    void loadOrganizations();
  }, [user]);

  const handleLogout = () => {
    logout();
    setShowLogoutDialog(false);
  };

  const handleSwitchOrganization = async (organizationId: string) => {
    if (!user || organizationId === user.organizationId) return;

    try {
      setIsSwitchingOrganization(true);
      const response = await switchOrganization({ organizationId });

      if (!response.success || !response.data) {
        throw new Error(response.message || 'Không thể chuyển tổ chức.');
      }

      completeLogin(response.data.accessToken, response.data.user);
      toast.success(`Đã chuyển sang ${response.data.user.organizationName}.`);
      window.location.reload();
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message ||
          error?.message ||
          'Không thể chuyển tổ chức.',
      );
    } finally {
      setIsSwitchingOrganization(false);
    }
  };

  // Desktop: full name + role
  // Tablet: shortened name
  // Mobile: avatar only
  const userName = user?.fullName || user?.username || 'Người dùng';
  const shortName = userName.split(' ').pop() || userName.charAt(0);

  const accountContent = (
    <>
      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 min-w-0">
        <User className="h-4 w-4" />
      </span>
      {/* Desktop: Show name and role */}
      {!isMobile && !isTablet && (
        <span className="hidden lg:flex lg:flex-col lg:min-w-0 lg:text-left">
          <span className="block max-w-48 truncate text-sm font-medium text-foreground">
            {userName}
          </span>
          <span className="block max-w-48 truncate text-xs text-muted-foreground">
            {roleLabel}
            {user?.organizationName ? ` · ${user.organizationName}` : ''}
          </span>
        </span>
      )}
      {/* Tablet: Show shortened name */}
      {isTablet && (
        <span className="hidden sm:flex sm:flex-col sm:min-w-0 sm:text-left">
          <span className="block max-w-32 truncate text-sm font-medium text-foreground">
            {shortName}
          </span>
          <span className="block max-w-32 truncate text-xs text-muted-foreground">
            {roleLabel}
          </span>
        </span>
      )}
    </>
  );

  const accountControl = organizations.length > 1 ? (
    <DropdownMenu>
      <DropdownMenuTrigger
        disabled={isSwitchingOrganization}
        render={
          <button
            type="button"
            className="flex min-w-0 items-center gap-2 rounded-lg px-2 py-1.5 text-left transition-colors hover:bg-emerald-50 disabled:pointer-events-none disabled:opacity-60"
            aria-label="Chuyển tổ chức"
            title="Chuyển tổ chức"
          >
            {accountContent}
          </button>
        }
      />
      <DropdownMenuContent className="w-72" align="end">
        <div className="px-2 py-1.5 text-xs font-medium text-muted-foreground">
          Chuyển tổ chức
        </div>
        {organizations.map((organization) => (
          <DropdownMenuItem
            key={organization.organizationId}
            disabled={isSwitchingOrganization}
            onClick={() => void handleSwitchOrganization(organization.organizationId)}
            className="items-start py-2"
          >
            <span className="min-w-0 flex-1">
              <span className="block truncate font-medium">
                {organization.organizationName}
              </span>
              <span className="block truncate text-xs text-muted-foreground">
                {organization.roleName} · {organization.organizationCode}
              </span>
            </span>
            {organization.organizationId === user?.organizationId && (
              <Check className="mt-0.5 text-emerald-600" />
            )}
          </DropdownMenuItem>
        ))}
        {canOpenOrganizationProfile && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => navigate('/organizations/profile')}>
              Hồ sơ tổ chức hiện tại
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  ) : canOpenOrganizationProfile ? (
    <Link
      to="/organizations/profile"
      className="flex min-w-0 items-center gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-emerald-50"
    >
      {accountContent}
    </Link>
  ) : (
    <div className="flex min-w-0 items-center gap-2 px-2 py-1.5">
      {accountContent}
    </div>
  );

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-emerald-100 bg-white/80 backdrop-blur-md">
        <div className={cn(
          "flex h-16 items-center gap-2 sm:gap-3",
          isMobile ? "px-3" : "px-4 md:px-6",
        )}>
          {/* Hamburger menu button - visible on mobile only (tablet uses the sidebar expand toggle) */}
          {isMobile && onMenuClick && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="text-muted-foreground hover:text-emerald-700 flex-shrink-0"
              onClick={onMenuClick}
              aria-label="Mở menu"
            >
              <Menu className="h-5 w-5" />
            </Button>
          )}

          {/* Page title area - mobile shows compact logo */}
          <div className="min-w-0 flex-1">
            {isMobile && (
              <Link to="/dashboard" className="inline-flex">
                <Logo height={36} />
              </Link>
            )}
          </div>

          {/* Right side: account, notifications, sync, logout */}
          <div className="flex min-w-0 items-center gap-1 sm:gap-2 md:gap-3">
            {accountControl}

            <NotificationBell />
            <SyncBadge />

            {/* Logout button - opens confirmation dialog */}
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => setShowLogoutDialog(true)}
              aria-label="Đăng xuất"
              title="Đăng xuất"
              className="text-muted-foreground hover:text-red-500"
            >
              <LogOut className="h-5 w-5" />
            </Button>
          </div>
        </div>
      </header>

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
