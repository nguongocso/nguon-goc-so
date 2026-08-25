import { useState } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Pencil, Loader2, Sparkles, Clock } from 'lucide-react';
import type { BackupSchedule as BackupScheduleType } from '@/types/backup';
import { parseCronToVisualState, getCronDescriptionInVietnamese } from '@/utils/cronHelper';

interface Props {
  schedule: BackupScheduleType | null;
  onEdit: () => void;
  onToggleActive: (data: { cronExpression: string; description?: string; isActive: boolean }) => Promise<void>;
  disabled?: boolean;
}

export const BackupSchedule = ({ schedule, onEdit, onToggleActive, disabled }: Props) => {
  const [isToggling, setIsToggling] = useState(false);

  const handleToggle = async (checked: boolean) => {
    if (!schedule || isToggling) return;

    setIsToggling(true);
    try {
      await onToggleActive({
        cronExpression: schedule.cronExpression,
        description: schedule.description,
        isActive: checked,
      });
    } catch (error) {
      // Error handled in useBackup
    } finally {
      setIsToggling(false);
    }
  };

  if (!schedule) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5 text-emerald-600" />
            Lịch sao lưu tự động
          </CardTitle>
          <CardDescription>Chưa có cấu hình lịch sao lưu tự động</CardDescription>
        </CardHeader>
        <CardFooter>
          <Button onClick={onEdit} disabled={disabled} variant="create">
            <Pencil className="h-4 w-4 mr-1" /> Thiết lập ngay
          </Button>
        </CardFooter>
      </Card>
    );
  }

  const visualState = parseCronToVisualState(schedule.cronExpression);
  const friendlyVietnameseSchedule = getCronDescriptionInVietnamese(visualState);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5 text-emerald-600" />
            Lịch sao lưu tự động
          </CardTitle>
          <CardDescription className="flex items-center gap-2 mt-1">
            <span>Biểu thức Cron:</span>
            <code className="bg-muted font-mono px-2 py-0.5 rounded text-xs text-primary">
              {schedule.cronExpression}
            </code>
          </CardDescription>
        </div>
        <div className="flex items-center gap-2">
          {isToggling && <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />}
          <Switch
            checked={schedule.isActive}
            onCheckedChange={handleToggle}
            disabled={disabled || isToggling}
          />
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex items-center gap-2 text-sm text-emerald-600 bg-emerald-50 dark:bg-emerald-950/30 p-2.5 rounded-md border border-emerald-200 dark:border-emerald-800">
          <Sparkles className="h-4 w-4 shrink-0" />
          <span className="font-medium">{friendlyVietnameseSchedule}</span>
        </div>

        {schedule.description && (
          <p className="text-sm text-muted-foreground">{schedule.description}</p>
        )}

        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>Trạng thái:</span>
          <Badge variant={schedule.isActive ? 'default' : 'secondary'}>
            {schedule.isActive ? '🟢 Đang kích hoạt' : '🔴 Đã dừng'}
          </Badge>
          <span className="ml-2">Cập nhật lần cuối: {new Date(schedule.updatedAt).toLocaleString('vi-VN')}</span>
          {schedule.updatedBy && <span>• {schedule.updatedBy}</span>}
        </div>
      </CardContent>
      <CardFooter className="gap-2">
        <Button variant="outline" onClick={onEdit} disabled={disabled || isToggling}>
          <Pencil className="h-4 w-4 mr-1" /> Chỉnh sửa lịch
        </Button>
      </CardFooter>
    </Card>
  );
};