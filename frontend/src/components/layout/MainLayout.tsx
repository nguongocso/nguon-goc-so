import { useCallback, useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import {
  AppBreadcrumb,
  BreadcrumbOverrideProvider,
} from '@/components/common/AppBreadcrumb';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';

const BREAKPOINT_SIDEBAR_DESKTOP = '(min-width: 1280px)';
const BREAKPOINT_SIDEBAR_TABLET = '(min-width: 768px) and (max-width: 1279px)';
const BREAKPOINT_MOBILE = '(max-width: 767px)';

export function MainLayout() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const location = useLocation();

  const isDesktop = useMediaQuery(BREAKPOINT_SIDEBAR_DESKTOP);
  const isTablet = useMediaQuery(BREAKPOINT_SIDEBAR_TABLET);
  const isMobile = useMediaQuery(BREAKPOINT_MOBILE);

  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Close expanded sidebar on route change
  useEffect(() => {
    setSidebarExpanded(false);
  }, [location.pathname]);

  // Handle escape key for mobile menu and tablet sidebar overlay
  useEffect(() => {
    if (!mobileMenuOpen && !sidebarExpanded) return;

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMobileMenuOpen(false);
        setSidebarExpanded(false);
      }
    };

    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [mobileMenuOpen, sidebarExpanded]);

  // Toggle expanded sidebar on tablet
  const toggleSidebarExpanded = useCallback(() => {
    setSidebarExpanded((prev) => !prev);
  }, []);

  // Determine sidebar mode:
  // Desktop (>=1280px): permanent full sidebar
  // Tablet (768-1279px): hidden sidebar, opens as overlay via the toggle button
  // Mobile (<768px): drawer overlay via header hamburger
  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50/50 via-white to-green-50/30">
      {/* Permanent Sidebar (Desktop only) */}
      {isDesktop && (
        <div className="fixed inset-y-0 left-0 z-30 hidden w-[17rem] xl:block">
          <Sidebar collapsed={false} />
        </div>
      )}

      {/* Tablet Sidebar Overlay */}
      {isTablet && sidebarExpanded && (
        <div className="fixed inset-0 z-50" role="dialog" aria-modal="true">
          {/* Backdrop */}
          <button
            type="button"
            className="absolute inset-0 bg-black/45 animate-fade-in"
            onClick={() => setSidebarExpanded(false)}
            aria-label="Đóng menu"
          />
          {/* Drawer */}
          <div className="relative h-full w-[min(20rem,88vw)] shadow-xl animate-slide-in-left">
            <Sidebar
              showCloseButton
              onClose={() => setSidebarExpanded(false)}
              onNavigate={() => setSidebarExpanded(false)}
              collapsed={false}
            />
          </div>
        </div>
      )}

      {/* Mobile Sidebar Drawer */}
      {isMobile && mobileMenuOpen && (
        <div className="fixed inset-0 z-50" role="dialog" aria-modal="true">
          {/* Backdrop */}
          <button
            type="button"
            className="absolute inset-0 bg-black/45 animate-fade-in"
            onClick={() => setMobileMenuOpen(false)}
            aria-label="Đóng menu"
          />
          {/* Drawer */}
          <div className="relative h-full w-[min(20rem,88vw)] shadow-xl animate-slide-in-left">
            <Sidebar
              showCloseButton
              onClose={() => setMobileMenuOpen(false)}
              onNavigate={() => setMobileMenuOpen(false)}
              collapsed={false}
            />
          </div>
        </div>
      )}

      {/* Tablet: floating expand/collapse toggle button */}
      {isTablet && (
        <button
          type="button"
          onClick={toggleSidebarExpanded}
          className={cn(
            'fixed top-[4.5rem] z-[60] flex h-8 w-8 items-center justify-center rounded-full border bg-white text-emerald-600 shadow-lg transition-all duration-300 hover:bg-emerald-50 hover:text-emerald-800',
            sidebarExpanded
              ? 'left-[min(20rem,88vw)] -translate-x-1/2'
              : 'left-3',
          )}
          aria-label={sidebarExpanded ? 'Thu gọn menu' : 'Mở rộng menu'}
        >
          {sidebarExpanded ? (
            <ChevronLeft className="h-4 w-4" />
          ) : (
            <ChevronRight className="h-4 w-4" />
          )}
        </button>
      )}

      {/* Main Content Area */}
      <div
        className={cn(
          'flex min-h-screen min-w-0 flex-col transition-all duration-300 ease-in-out',
          isDesktop ? 'xl:pl-[17rem]' : '',
        )}
      >
        <Header
          onMenuClick={() => setMobileMenuOpen(true)}
          isMobile={isMobile}
          isTablet={isTablet}
        />
        <main className="min-w-0 flex-1 p-3 sm:p-4 md:p-5 lg:p-6 xl:p-8">
          <BreadcrumbOverrideProvider>
            <div className="mx-auto w-full max-w-7xl">
              {/* Breadcrumb điều hướng thống nhất thay cho nút "Quay lại" */}
              {location.pathname !== '/dashboard' && (
                <div className="mb-4">
                  <AppBreadcrumb />
                </div>
              )}
              <Outlet />
            </div>
          </BreadcrumbOverrideProvider>
        </main>
      </div>
    </div>
  );
}