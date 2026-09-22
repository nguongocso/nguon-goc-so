import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { NotificationPanel } from '@/components/notification/NotificationPanel';
import { isApiKeyWarningNotification } from '@/lib/notificationHelpers';
import { useNotifications } from '@/hooks/useNotifications';
import { useUnreadCount } from '@/hooks/useUnreadCount';
import { useAuth } from '@/hooks/useAuth';
import { hasAnyRole, ROLE_ACCESS } from '@/config/roleAccess';
import type { NotificationResponse } from '@/types/notification';

export const NotificationBell = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const { unreadCount: apiUnreadCount, refresh: refreshUnreadCount } = useUnreadCount();
  const { items, isLoading, load, markAsRead } = useNotifications({
    size: 8,
    autoLoad: false,
  });

  const isMissingEmail = Boolean(
    user &&
    hasAnyRole(user.roleCode, ROLE_ACCESS.userProfile) &&
    (!user.email || user.email.trim() === ''),
  );

  const isMissingTerritory = Boolean(
    user &&
    hasAnyRole(user.roleCode, ROLE_ACCESS.organizationProfile) &&
    user.roleCode === 'VT-02' &&
    (!user.organizationProvinceId || !user.organizationCommuneId),
  );

  const emailNoticeKey = user ? `session_read_email_notice_${user.userId}` : '';
  const [isEmailNoticeRead, setIsEmailNoticeRead] = useState<boolean>(() => {
    return emailNoticeKey ? sessionStorage.getItem(emailNoticeKey) === 'true' : false;
  });

  const territoryNoticeKey = user?.organizationId
    ? `session_read_org_territory_notice_${user.organizationId}`
    : '';
  const [isTerritoryNoticeRead, setIsTerritoryNoticeRead] = useState<boolean>(() => {
    return territoryNoticeKey ? sessionStorage.getItem(territoryNoticeKey) === 'true' : false;
  });

  // Đồng bộ trạng thái đã đọc khi user hoặc thông tin tài khoản thay đổi
  useEffect(() => {
    if (emailNoticeKey) {
      setIsEmailNoticeRead(sessionStorage.getItem(emailNoticeKey) === 'true');
    }
  }, [emailNoticeKey, user?.email]);

  useEffect(() => {
    if (territoryNoticeKey) {
      setIsTerritoryNoticeRead(sessionStorage.getItem(territoryNoticeKey) === 'true');
    }
  }, [territoryNoticeKey, user?.organizationProvinceId, user?.organizationCommuneId]);

  const totalUnreadCount =
    apiUnreadCount +
    (isMissingEmail && !isEmailNoticeRead ? 1 : 0) +
    (isMissingTerritory && !isTerritoryNoticeRead ? 1 : 0);

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (nextOpen) {
      void load(0);
      void refreshUnreadCount();
    }
  };

  const handleItemClick = (notification: NotificationResponse) => {
    if (isApiKeyWarningNotification(notification)) {
      if (!notification.isRead) {
        void markAsRead(notification.id).then(() => refreshUnreadCount());
      }
      setOpen(false);
      const action = notification.title.toLowerCase().includes('hạn mức') ? 'quota' : 'renew';
      if (notification.entityId) {
        navigate(`/integration/api-keys?keyId=${notification.entityId}&action=${action}`);
      } else {
        navigate('/integration/api-keys');
      }
      return;
    }
    if (!notification.isRead) {
      void markAsRead(notification.id).then(() => refreshUnreadCount());
    }
    setOpen(false);
    if (notification.type === 'ACTIVITY_LOG_EXPORT_READY' && notification.entityId) {
      navigate(`/activity-logs?exportJobId=${notification.entityId}`);
      return;
    }
    if (notification.entityId) {
      navigate(`/shipment-handovers/${notification.entityId}`);
      return;
    }
    const text = `${notification.title} ${notification.content}`.toLowerCase();
    if (text.includes('kiểm nghiệm') || text.includes('lô sản xuất')) {
      navigate('/production-lots');
    }
  };

  const handleEmailNoticeClick = () => {
    if (emailNoticeKey) {
      sessionStorage.setItem(emailNoticeKey, 'true');
      setIsEmailNoticeRead(true);
    }
    setOpen(false);
    navigate('/profile');
  };

  const handleTerritoryNoticeClick = () => {
    if (territoryNoticeKey) {
      sessionStorage.setItem(territoryNoticeKey, 'true');
      setIsTerritoryNoticeRead(true);
    }
    setOpen(false);
    navigate('/organizations/profile');
  };

  return (
    <DropdownMenu open={open} onOpenChange={handleOpenChange}>
      <DropdownMenuTrigger
        render={
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="relative border-0"
            aria-label="Thông báo"
            title="Thông báo"
          >
            <Bell className="h-5 w-5" />
            {totalUnreadCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white">
                {totalUnreadCount > 99 ? '99+' : totalUnreadCount}
              </span>
            )}
          </Button>
        }
      />
      <DropdownMenuContent className="p-0" align="end">
        <NotificationPanel
          items={items}
          isLoading={isLoading}
          onItemClick={handleItemClick}
          isMissingEmail={isMissingEmail}
          isEmailNoticeRead={isEmailNoticeRead}
          onEmailNoticeClick={handleEmailNoticeClick}
          isMissingTerritory={isMissingTerritory}
          isTerritoryNoticeRead={isTerritoryNoticeRead}
          onTerritoryNoticeClick={handleTerritoryNoticeClick}
        />
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
