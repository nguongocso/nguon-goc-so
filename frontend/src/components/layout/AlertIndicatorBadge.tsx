import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { getUnviewedAlertCount } from '@/api/aggregateAlertApi';
import { useAuth } from '@/hooks/useAuth';
import { hasAnyRole, ROLE_ACCESS } from '@/config/roleAccess';

export const AlertIndicatorBadge: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [unviewedCount, setUnviewedCount] = useState<number>(0);
  const [hasHighSeverity, setHasHighSeverity] = useState<boolean>(false);

  const canViewAlerts = Boolean(
    user && hasAnyRole(user.roleCode, ROLE_ACCESS.aggregateAlerts)
  );

  const fetchCount = useCallback(async () => {
    if (!canViewAlerts) return;
    try {
      const res = await getUnviewedAlertCount();
      if (res) {
        setUnviewedCount(res.unviewedCount);
        setHasHighSeverity(res.hasHighSeverity);
      }
    } catch {
      // Bỏ qua lỗi ngầm để không gián đoạn trải nghiệm thanh điều hướng
    }
  }, [canViewAlerts]);

  useEffect(() => {
    fetchCount();

    // Polling định kỳ mỗi 60 giây
    const interval = setInterval(fetchCount, 60000);

    // Lắng nghe khi tab trình duyệt được active lại
    const handleFocus = () => {
      fetchCount();
    };
    window.addEventListener('focus', handleFocus);

    return () => {
      clearInterval(interval);
      window.removeEventListener('focus', handleFocus);
    };
  }, [fetchCount]);

  if (!canViewAlerts) {
    return null;
  }

  const handleClick = () => {
    navigate('/alerts');
  };

  const titleText =
    unviewedCount > 0
      ? `Có ${unviewedCount} cảnh báo đang mở cần xử lý${hasHighSeverity ? ' (có việc mức khẩn cấp cao)' : ''}`
      : 'Không có cảnh báo nào đang mở';

  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      onClick={handleClick}
      title={titleText}
      aria-label={titleText}
      className="relative rounded-lg border border-input bg-transparent text-muted-foreground hover:bg-accent hover:text-accent-foreground hover:text-emerald-700"
    >
      <ShieldAlert className={`h-5 w-5 ${unviewedCount > 0 && hasHighSeverity ? 'text-red-600' : ''}`} />

      {unviewedCount > 0 && (
        <span
          className={`absolute -top-1.5 -right-1.5 flex h-5 min-w-5 items-center justify-center rounded-full px-1 text-[11px] font-bold text-white shadow-sm ring-2 ring-white ${
            hasHighSeverity ? 'bg-red-600 animate-pulse' : 'bg-amber-600'
          }`}
        >
          {unviewedCount > 99 ? '99+' : unviewedCount}
        </span>
      )}
    </Button>
  );
};
