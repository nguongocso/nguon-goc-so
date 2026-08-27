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
    (!user.email || user.email.trim() === '')
  );

  const emailNoticeKey = user ? `session_read_email_notice_${user.userId}` : '';
  const [isEmailNoticeRead, setIsEmailNoticeRead] = useState<boolean>(() => {
    return emailNoticeKey ? sessionStorage.getItem(emailNoticeKey) === 'true' : false;
  });

  // Đồng bộ trạng thái đã đọc khi user thay đổi hoặc email cập nhật
  useEffect(() => {
    if (emailNoticeKey) {
      setIsEmailNoticeRead(sessionStorage.getItem(emailNoticeKey) === 'true');
    }
  }, [emailNoticeKey, user?.email]);

  // Tổng số lượng thông báo chưa đọc (bao gồm thông báo nhắc email nếu chưa đọc)
  const totalUnreadCount =
    apiUnreadCount + (isMissingEmail && !isEmailNoticeRead ? 1 : 0);

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (nextOpen) {     
      void load(0);
      void refreshUnreadCount();
    }
  };

  const handleItemClick = (notification: NotificationResponse) => {
    if (!notification.isRead) {
      void markAsRead(notification.id).then(() => refreshUnreadCount());
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
        />
      </DropdownMenuContent>
    </DropdownMenu>
  );
};