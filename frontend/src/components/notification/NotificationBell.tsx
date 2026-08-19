import { useState } from 'react';
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
import type { NotificationResponse } from '@/types/notification';

export const NotificationBell = () => {
  const [open, setOpen] = useState(false);
  const { unreadCount, refresh: refreshUnreadCount } = useUnreadCount();
  const { items, isLoading, load, markAsRead } = useNotifications({
    size: 8,
    autoLoad: false,
  });

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
            {unreadCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white">
                {unreadCount > 99 ? '99+' : unreadCount}
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
        />
      </DropdownMenuContent>
    </DropdownMenu>
  );
};