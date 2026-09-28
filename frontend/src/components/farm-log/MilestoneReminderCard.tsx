import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  CalendarClock,
  Clock,
  ExternalLink,
  Loader2,
  RefreshCw,
  CheckCircle2,
} from 'lucide-react';
import { toast } from 'sonner';

import { useAuth } from '@/hooks/useAuth';
import { hasAnyRole, ROLE_ACCESS } from '@/config/roleAccess';
import {
  getMilestoneReminders,
  getMyActiveMilestoneReminders,
  triggerMilestoneScan,
} from '@/api/milestoneReminderApi';
import type { MilestoneReminder } from '@/types/milestoneReminder';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  PLANTING: 'Gieo trồng',
  WATERING: 'Tưới nước',
  FERTILIZING: 'Bón phân',
  PESTICIDE: 'Phun thuốc',
  WEEDING: 'Làm cỏ',
  HARVESTING: 'Thu hoạch',
  OTHER: 'Khác',
};

interface MilestoneReminderCardProps {
  /**
   * Nếu true, chỉ hiển thị danh sách của chính user đăng nhập (dành cho VT-03).
   * Nếu false hoặc undefined, hiển thị theo vai trò (VT-01, VT-02 thấy của cả tổ chức).
   */
  userOnly?: boolean;
  /**
   * Tùy chọn ẩn thẻ khi không có nhắc việc quá hạn nào.
   * Mặc định là false.
   */
  hideWhenEmpty?: boolean;
  className?: string;
}

export const MilestoneReminderCard: React.FC<MilestoneReminderCardProps> = ({
  userOnly = false,
  hideWhenEmpty = false,
  className = '',
}) => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [reminders, setReminders] = useState<MilestoneReminder[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isScanning, setIsScanning] = useState(false);

  const canScan = hasAnyRole(user?.roleCode, ROLE_ACCESS.milestoneReminderScan);

  const loadReminders = useCallback(async () => {
    try {
      setIsLoading(true);
      if (userOnly || user?.roleCode === 'VT-03') {
        const data = await getMyActiveMilestoneReminders();
        setReminders(data);
      } else {
        const pageResponse = await getMilestoneReminders({
          status: 'OPEN',
          size: 10,
        });
        setReminders(pageResponse.items || []);
      }
    } catch {
      // Khi không có quyền hoặc lỗi kết nối, để danh sách rỗng
      setReminders([]);
    } finally {
      setIsLoading(false);
    }
  }, [userOnly, user?.roleCode]);

  useEffect(() => {
    void loadReminders();
  }, [loadReminders]);

  const handleTriggerScan = async () => {
    try {
      setIsScanning(true);
      const result = await triggerMilestoneScan();
      toast.success(result.message || 'Quét mốc canh tác hoàn tất');
      await loadReminders();
    } catch {
      toast.error('Không thể thực hiện quét mốc canh tác quá hạn');
    } finally {
      setIsScanning(false);
    }
  };

  const handleRecordNow = (reminder: MilestoneReminder) => {
    const milestoneParam = reminder.milestoneId ? `&milestoneId=${reminder.milestoneId}` : '';
    navigate(
      `/farm-logs/create?productionLotId=${reminder.lotId}&activityType=${reminder.activityType}${milestoneParam}`
    );
  };

  if (hideWhenEmpty && !isLoading && reminders.length === 0) {
    return null;
  }

  return (
    <Card
      className={`border-amber-200 bg-gradient-to-br from-amber-50/70 via-white to-amber-50/30 shadow-sm ${className}`}
    >
      <CardHeader className="pb-3">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-amber-100 text-amber-700">
              <CalendarClock className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <CardTitle className="text-base font-bold text-amber-950">
                  Nhắc lịch ghi nhật ký mốc canh tác
                </CardTitle>
                {reminders.length > 0 && (
                  <Badge variant="destructive" className="px-2 py-0.5 text-xs font-semibold">
                    {reminders.length} quá hạn
                  </Badge>
                )}
              </div>
              <CardDescription className="text-xs text-amber-800/80">
                Các mốc canh tác bắt buộc chưa được ghi nhật ký và đã quá ngày dự kiến
              </CardDescription>
            </div>
          </div>

          <div className="flex items-center gap-2 self-end sm:self-auto">
            {canScan && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleTriggerScan}
                disabled={isScanning}
                className="h-8 border-amber-300 bg-white/80 text-amber-900 hover:bg-amber-100/60 hover:text-amber-950 text-xs"
              >
                {isScanning ? (
                  <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                ) : (
                  <RefreshCw className="mr-1.5 h-3.5 w-3.5" />
                )}
                Quét quá hạn ngay
              </Button>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {isLoading ? (
          <div className="flex items-center justify-center py-6 text-sm text-amber-800">
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Đang kiểm tra lịch nhắc việc...
          </div>
        ) : reminders.length === 0 ? (
          <div className="flex items-center gap-2 rounded-lg bg-emerald-50/80 p-3 text-sm text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
            <span>Tất cả mốc canh tác bắt buộc hiện tại đều đã được ghi nhật ký đầy đủ.</span>
          </div>
        ) : (
          <div className="space-y-2.5">
            {reminders.map((reminder) => {
              const activityLabel =
                ACTIVITY_TYPE_LABELS[reminder.activityType] || reminder.activityType;

              return (
                <div
                  key={reminder.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-lg border border-amber-200/80 bg-white p-3 shadow-2xs transition hover:border-amber-300"
                >
                  <div className="flex items-start gap-3">
                    <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold text-sm text-slate-900">
                          {reminder.milestoneName}
                        </span>
                        <Badge
                          variant="secondary"
                          className="bg-emerald-50 text-emerald-700 hover:bg-emerald-100 text-xs font-medium"
                        >
                          {activityLabel}
                        </Badge>
                        <Badge
                          variant="destructive"
                          className="bg-rose-50 text-rose-700 hover:bg-rose-100 text-xs font-medium border border-rose-200"
                        >
                          <Clock className="mr-1 h-3 w-3" />
                          Quá hạn {reminder.overdueDays} ngày
                        </Badge>
                      </div>

                      <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                        <span>
                          Lô:{' '}
                          <strong className="font-medium text-slate-700">
                            {reminder.lotName}
                          </strong>
                        </span>
                        {reminder.expectedDate && (
                          <span>
                            Ngày dự kiến:{' '}
                            <span className="text-slate-600">
                              {reminder.expectedDate}
                            </span>
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <Button
                    size="sm"
                    onClick={() => handleRecordNow(reminder)}
                    className="shrink-0 bg-emerald-600 text-white hover:bg-emerald-700 text-xs h-8 self-end sm:self-center"
                  >
                    Ghi nhật ký ngay
                    <ExternalLink className="ml-1.5 h-3.5 w-3.5" />
                  </Button>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
