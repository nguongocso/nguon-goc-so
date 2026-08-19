import { useState } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Pencil, Loader2 } from 'lucide-react';
import type { BackupSchedule as BackupScheduleType } from '@/types/backup';

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
      // Success: state sẽ được cập nhật từ parent
    } catch (error) {
      // Error: đã được xử lý trong useBackup (toast.error)
      // Switch sẽ tự động revert vì schedule.isActive không đổi
    } finally {
      setIsToggling(false);
    }
  };

  if (!schedule) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>⚙️ Lịch sao lưu tự động</CardTitle>
          <CardDescription>Chưa có cấu hình lịch sao lưu</CardDescription>
        </CardHeader>
        <CardFooter>
          <Button onClick={onEdit} disabled={disabled}>
            <Pencil className="h-4 w-4 mr-1" /> Thiết lập
          </Button>
        </CardFooter>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle>⚙️ Lịch sao lưu tự động</CardTitle>
          <CardDescription>
            Biểu thức Cron: <code className="bg-muted px-2 py-1 rounded text-sm">{schedule.cronExpression}</code>
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
      <CardContent className="space-y-2">
        <p className="text-sm">{schedule.description || 'Không có mô tả'}</p>
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>Trạng thái:</span>
          <Badge variant={schedule.isActive ? 'default' : 'secondary'}>
            {schedule.isActive ? '🟢 Đang kích hoạt' : '🔴 Đã dừng'}
          </Badge>
          <span className="ml-2">Cập nhật lần cuối: {new Date(schedule.updatedAt).toLocaleString('vi-VN')}</span>
          <span>• {schedule.updatedBy}</span>
        </div>
      </CardContent>
      <CardFooter className="gap-2">
        <Button variant="outline" onClick={onEdit} disabled={disabled || isToggling}>
          <Pencil className="h-4 w-4 mr-1" /> Chỉnh sửa
        </Button>
      </CardFooter>
    </Card>
  );
};