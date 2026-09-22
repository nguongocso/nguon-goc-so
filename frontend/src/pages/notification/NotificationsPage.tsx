import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertTriangle, Bell, CheckCheck, CheckCircle2, ChevronLeft, ChevronRight, Info, MailWarning, MapPinOff, RefreshCw } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { useNotifications } from '@/hooks/useNotifications';
import { useUnreadCount } from '@/hooks/useUnreadCount';
import { resolveNotificationTarget } from '@/lib/notificationHelpers';
import { useAuth } from '@/hooks/useAuth';
import { hasAnyRole, ROLE_ACCESS } from '@/config/roleAccess';
import type { NotificationResponse, NotificationType } from '@/types/notification';

type ReadFilter = 'ALL' | 'UNREAD' | 'READ';

const TYPE_ICON: Record<NotificationType, typeof Bell> = {
  ALERT: AlertTriangle,
  TASK: CheckCircle2,
  INFO: Info,
  LOGIN_ANOMALY_DETECTED: AlertTriangle,
  ACCOUNT_LOCKED: Bell,
  ANOMALY_OPEN: AlertTriangle,
  ANOMALY_DISMISSED: CheckCircle2,
  ACCOUNT_UNLOCKED: Info,
  ACTIVITY_LOG_EXPORT_READY: CheckCircle2,
  FARM_LOG_SYNC_SUCCESS: CheckCircle2,
  FARM_LOG_SYNC_FAILED: RefreshCw,
};

const TYPE_STYLE: Record<NotificationType, string> = {
  ALERT: 'bg-error-bg text-destructive',
  TASK: 'bg-warning-bg text-warning',
  INFO: 'bg-info-bg text-info',
  LOGIN_ANOMALY_DETECTED: 'bg-error-bg text-destructive',
  ACCOUNT_LOCKED: 'bg-warning-bg text-warning',
  ANOMALY_OPEN: 'bg-error-bg text-destructive',
  ANOMALY_DISMISSED: 'bg-success-bg text-success',
  ACCOUNT_UNLOCKED: 'bg-info-bg text-info',
  ACTIVITY_LOG_EXPORT_READY: 'bg-success-bg text-success',
  FARM_LOG_SYNC_SUCCESS: 'bg-success-bg text-success',
  FARM_LOG_SYNC_FAILED: 'bg-warning-bg text-warning',
};

const formatNotificationReason = (content: string) => {
  return content
    .replace(/REPEATED_FAILED_LOGIN/g, 'Đăng nhập thất bại nhiều lần')
    .replace(/UNUSUAL_COUNTRY/g, 'Đăng nhập từ quốc gia bất thường')
    .replace(/OPEN/g, 'Bất thường mới phát hiện')
    .replace(/ACCOUNT_LOCKED/g, 'Tài khoản bị khóa')
    .replace(/DISMISSED/g, 'Bất thường đã được bỏ qua')
    .replace(/LOGIN_ANOMALY_DETECTED/g, 'Phát hiện đăng nhập bất thường');
};

const formatDateTime = (iso: string) => {
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
};

const NotificationsPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialFilter: ReadFilter =
    searchParams.get('filter') === 'UNREAD' ? 'UNREAD' : searchParams.get('filter') === 'READ' ? 'READ' : 'ALL';
  const [filter, setFilter] = useState<ReadFilter>(initialFilter);
  const isRead = filter === 'ALL' ? undefined : filter === 'READ';

  useEffect(() => {
    const next = searchParams.get('filter');
    if (next === 'UNREAD' || next === 'READ' || next === 'ALL') {
      setFilter(next);
    }
  }, [searchParams]);

  const { items, page, totalPages, isLoading, load, markAsRead, markAllAsRead } = useNotifications({
    size: 20,
    isRead,
  });
  const { unreadCount, refresh: refreshUnreadCount } = useUnreadCount();
  const [isMarkingAllAsRead, setIsMarkingAllAsRead] = useState(false);

  const isMissingEmail = Boolean(
    user &&
    hasAnyRole(user.roleCode, ROLE_ACCESS.userProfile) &&
    (!user.email || user.email.trim() === '')
  );

  const emailNoticeKey = user ? `session_read_email_notice_${user.userId}` : '';
  const [isEmailNoticeRead, setIsEmailNoticeRead] = useState<boolean>(() => {
    return emailNoticeKey ? sessionStorage.getItem(emailNoticeKey) === 'true' : false;
  });

  useEffect(() => {
    if (emailNoticeKey) {
      setIsEmailNoticeRead(sessionStorage.getItem(emailNoticeKey) === 'true');
    }
  }, [emailNoticeKey, user?.email]);

  const showEmailNotice =
    isMissingEmail &&
    (filter === 'ALL' ||
      (filter === 'UNREAD' && !isEmailNoticeRead) ||
      (filter === 'READ' && isEmailNoticeRead));

  // Cảnh báo thiếu địa bàn hành chính đối với vai trò Quản lý HTX (VT-02)
  const isMissingTerritory = Boolean(
    user &&
    user.roleCode === 'VT-02' &&
    (!user.organizationProvinceId || !user.organizationCommuneId)
  );

  const territoryNoticeKey = user?.organizationId
    ? `session_read_org_territory_notice_${user.organizationId}`
    : '';
  const [isTerritoryNoticeRead, setIsTerritoryNoticeRead] = useState<boolean>(() => {
    return territoryNoticeKey ? sessionStorage.getItem(territoryNoticeKey) === 'true' : false;
  });

  useEffect(() => {
    if (territoryNoticeKey) {
      setIsTerritoryNoticeRead(sessionStorage.getItem(territoryNoticeKey) === 'true');
    }
  }, [territoryNoticeKey, user?.organizationProvinceId, user?.organizationCommuneId]);

  const showTerritoryNotice =
    isMissingTerritory &&
    (filter === 'ALL' ||
      (filter === 'UNREAD' && !isTerritoryNoticeRead) ||
      (filter === 'READ' && isTerritoryNoticeRead));

  const handleItemClick = (notification: NotificationResponse) => {
    if (!notification.isRead) {
      void markAsRead(notification.id).then(() => refreshUnreadCount());
    }
    const target = resolveNotificationTarget(notification);
    if (target) {
      navigate(target);
    }
  };

  const handleMarkAllAsRead = async () => {
    setIsMarkingAllAsRead(true);
    try {
      await markAllAsRead();
      await refreshUnreadCount();
    } finally {
      setIsMarkingAllAsRead(false);
    }
  };

  const handleEmailNoticeClick = () => {
    if (emailNoticeKey) {
      sessionStorage.setItem(emailNoticeKey, 'true');
      setIsEmailNoticeRead(true);
      void refreshUnreadCount();
    }
    navigate('/profile');
  };

  const handleTerritoryNoticeClick = () => {
    if (territoryNoticeKey) {
      sessionStorage.setItem(territoryNoticeKey, 'true');
      setIsTerritoryNoticeRead(true);
      void refreshUnreadCount();
    }
    navigate('/organizations/profile');
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Thông báo</h1>
          <p className="text-sm text-muted-foreground">
            Danh sách việc cần làm và cảnh báo liên quan đến tài khoản của bạn.
          </p>
        </div>
        <HelpButton screenKey="notifications" />
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-2">
          {(
            [
              { value: 'ALL', label: 'Tất cả' },
              { value: 'UNREAD', label: unreadCount > 0 ? `Chưa đọc (${unreadCount})` : 'Chưa đọc' },
              { value: 'READ', label: 'Đã đọc' },
            ] as const
          ).map((option) => (
            <Button
              key={option.value}
              type="button"
              size="sm"
              variant={filter === option.value ? 'default' : 'outline'}
              onClick={() => {
                setFilter(option.value);
                setSearchParams(option.value === 'ALL' ? {} : { filter: option.value });
              }}
            >
              {option.label}
            </Button>
          ))}
        </div>
        {unreadCount > 0 && (
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={() => void handleMarkAllAsRead()}
            disabled={isMarkingAllAsRead}
            title="Đánh dấu tất cả thông báo là đã đọc"
          >
            <CheckCheck className="h-4 w-4" />
            {isMarkingAllAsRead ? 'Đang xử lý...' : 'Đánh dấu tất cả đã đọc'}
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Danh sách thông báo</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-primary" />
            </div>
          ) : items.length === 0 && !showEmailNotice && !showTerritoryNotice ? (
            <div className="px-4 py-16 text-center text-muted-foreground">
              <Bell className="mx-auto mb-3 h-10 w-10 text-muted-foreground/50" />
              <p className="font-medium">Chưa có thông báo nào</p>
            </div>
          ) : (
            <ul className="divide-y">
              {showEmailNotice && (
                <li>
                  <button
                    type="button"
                    onClick={handleEmailNoticeClick}
                    className="flex w-full items-start gap-3 bg-amber-50/80 px-4 py-4 text-left transition-colors hover:bg-amber-100/70"
                  >
                    <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-amber-500/20 text-amber-700">
                      <MailWarning className="h-4 w-4" />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center justify-between gap-1.5">
                        <span className="font-medium text-amber-950">
                          Cần bổ sung địa chỉ email
                        </span>
                        {!isEmailNoticeRead && (
                          <span
                            className="size-2 shrink-0 rounded-full bg-red-500 ring-2 ring-white"
                            title="Chưa đọc"
                          />
                        )}
                      </span>
                      <span className="mt-1 block text-sm text-amber-900/90 leading-relaxed">
                        Vui lòng thêm email tài khoản để có thể sử dụng tính năng lấy lại mật khẩu khi quên.
                      </span>
                      <span className="mt-2 inline-flex items-center text-xs font-semibold text-amber-700 underline">
                        Cập nhật hồ sơ người dùng ngay &rarr;
                      </span>
                    </span>
                  </button>
                </li>
              )}
              {showTerritoryNotice && (
                <li>
                  <button
                    type="button"
                    onClick={handleTerritoryNoticeClick}
                    className="flex w-full items-start gap-3 bg-amber-50/80 px-4 py-4 text-left transition-colors hover:bg-amber-100/70"
                  >
                    <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-amber-500/20 text-amber-700">
                      <MapPinOff className="h-4 w-4" />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center justify-between gap-1.5">
                        <span className="font-medium text-amber-950">
                          Cần thiết lập địa bàn hành chính
                        </span>
                        {!isTerritoryNoticeRead && (
                          <span
                            className="size-2 shrink-0 rounded-full bg-red-500 ring-2 ring-white"
                            title="Chưa đọc"
                          />
                        )}
                      </span>
                      <span className="mt-1 block text-sm text-amber-900/90 leading-relaxed">
                        Hợp tác xã chưa chọn Tỉnh/Thành phố và Xã/Phường. Vui lòng cập nhật để đồng bộ với Cán bộ ngành.
                      </span>
                      <span className="mt-2 inline-flex items-center text-xs font-semibold text-amber-700 underline">
                        Cập nhật hồ sơ tổ chức ngay &rarr;
                      </span>
                    </span>
                  </button>
                </li>
              )}
              {items.map((item) => {
                const Icon = TYPE_ICON[item.type] || Bell;
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => handleItemClick(item)}
                      className={cn(
                        'flex w-full items-start gap-3 px-4 py-4 text-left transition-colors hover:bg-muted',
                        !item.isRead && 'bg-success-bg/50',
                      )}
                    >
                      <span
                        className={cn(
                          'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full',
                          TYPE_STYLE[item.type],
                        )}
                      >
                        <Icon className="h-4 w-4" />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="flex items-center gap-1.5">
                          <span className="font-medium text-foreground">{item.title}</span>
                          {!item.isRead && (
                            <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                          )}
                        </span>
                        <span className="mt-1 block text-sm text-muted-foreground">
                          {formatNotificationReason(item.content)}
                        </span>
                        <span className="mt-1.5 block text-xs text-muted-foreground/70">
                          {formatDateTime(item.createdAt)}
                          {item.isRead && item.readAt && ' · Đã đọc'}
                        </span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {!isLoading && totalPages > 1 && (
            <div className="flex items-center justify-between border-t px-4 py-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => void load(page - 1)}
                disabled={page === 0}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">
                Trang {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => void load(page + 1)}
                disabled={page >= totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default NotificationsPage;
