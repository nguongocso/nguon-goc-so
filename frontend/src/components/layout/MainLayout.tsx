import { useCallback, useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
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

  // Handle escape key for mobile menu
  useEffect(() => {
    if (!mobileMenuOpen) return;

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMobileMenuOpen(false);
    };

    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [mobileMenuOpen]);

  // Toggle expanded sidebar on tablet
  const toggleSidebarExpanded = useCallback(() => {
    setSidebarExpanded((prev) => !prev);
  }, []);

  // Determine sidebar mode:
  // Desktop (>=1280px): permanent full sidebar
  // Tablet (768-1279px): permanent collapsed sidebar (icons only), expandable
  // Mobile (<768px): no permanent sidebar, drawer overlay instead
  const showPermanentSidebar = isDesktop || isTablet;
  const sidebarCollapsed = isTablet && !sidebarExpanded;

  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50/50 via-white to-green-50/30">
      {/* Permanent Sidebar (Desktop + Tablet) */}
      {showPermanentSidebar && (
        <div
          className={cn(
            'fixed inset-y-0 left-0 z-30 hidden md:block transition-all duration-300 ease-in-out',
            sidebarCollapsed ? 'w-[4.5rem]' : 'w-[17rem]',
          )}
        >
          <Sidebar collapsed={sidebarCollapsed} />

          {/* Tablet: Expand/collapse toggle button */}
          {isTablet && (
            <button
              type="button"
              onClick={toggleSidebarExpanded}
              className="absolute -right-3 top-[4.5rem] z-40 flex h-6 w-6 items-center justify-center rounded-full bg-white border border-emerald-100 shadow-sm text-emerald-600 hover:text-emerald-800 transition-all duration-300"
              aria-label={sidebarExpanded ? 'Thu gọn menu' : 'Mở rộng menu'}
            >
              {sidebarExpanded ? (
                <ChevronLeft className="h-3.5 w-3.5" />
              ) : (
                <ChevronRight className="h-3.5 w-3.5" />
              )}
            </button>
          )}
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

      {/* Main Content Area */}
      <div
        className={cn(
          'flex min-h-screen min-w-0 flex-col transition-all duration-300 ease-in-out',
          showPermanentSidebar && sidebarCollapsed
            ? 'md:pl-[4.5rem]'
            : showPermanentSidebar
              ? 'md:pl-[17rem]'
              : '',
        )}
      >
        <Header
          onMenuClick={() => setMobileMenuOpen(true)}
          isMobile={isMobile}
          isTablet={isTablet}
        />
        <main className="min-w-0 flex-1 p-3 sm:p-4 md:p-5 lg:p-6 xl:p-8">
          <div className="mx-auto w-full max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}