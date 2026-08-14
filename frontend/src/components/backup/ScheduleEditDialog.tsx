import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import type { BackupSchedule, BackupScheduleRequest } from '@/types/backup';

interface Props {
  open: boolean;
  onClose: () => void;
  schedule: BackupSchedule | null;
  onSave: (data: BackupScheduleRequest) => Promise<void>;
}

export const ScheduleEditDialog = ({ open, onClose, schedule, onSave }: Props) => {
  const [cronExpression, setCronExpression] = useState(schedule?.cronExpression || '0 0 2 * * ?');
  const [description, setDescription] = useState(schedule?.description || '');
  const [isActive, setIsActive] = useState(schedule?.isActive ?? true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    setError(null);
    try {
      setLoading(true);
      await onSave({ cronExpression, description, isActive });
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lưu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{schedule ? 'Chỉnh sửa lịch sao lưu' : 'Thiết lập lịch sao lưu'}</DialogTitle>
          <DialogDescription>
            Cấu hình biểu thức cron và trạng thái kích hoạt cho lịch sao lưu tự động.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="cron">Biểu thức Cron *</Label>
            <Input
              id="cron"
              value={cronExpression}
              onChange={(e) => setCronExpression(e.target.value)}
              placeholder="VD: 0 0 2 * * ?"
            />
            <p className="text-xs text-muted-foreground">Ví dụ: 0 0 2 * * ? (2 giờ sáng hàng ngày)</p>
          </div>
          <div className="space-y-2">
            <Label htmlFor="desc">Mô tả</Label>
            <Textarea
              id="desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Mô tả ngắn về lịch sao lưu"
            />
          </div>
          <div className="flex items-center justify-between">
            <Label htmlFor="active">Kích hoạt</Label>
            <Switch id="active" checked={isActive} onCheckedChange={setIsActive} />
          </div>
          {error && <p className="text-sm text-red-500">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Hủy
          </Button>
          <Button onClick={handleSubmit} disabled={loading}>
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {schedule ? 'Cập nhật' : 'Tạo mới'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};