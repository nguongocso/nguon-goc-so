import { useEffect, useState } from 'react';
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Calendar, Loader2, Sparkles, Code2 } from 'lucide-react';
import type { BackupSchedule, BackupScheduleRequest } from '@/types/backup';
import {
  type ScheduleType,
  type VisualCronState,
  DAY_OF_WEEK_OPTIONS,
  INTERVAL_HOUR_OPTIONS,
  buildCronFromVisualState,
  parseCronToVisualState,
  getCronDescriptionInVietnamese,
} from '@/utils/cronHelper';

interface Props {
  open: boolean;
  onClose: () => void;
  schedule: BackupSchedule | null;
  onSave: (data: BackupScheduleRequest) => Promise<void>;
}

// Generate options 0-23
const HOURS = Array.from({ length: 24 }, (_, i) => ({
  value: i,
  label: `${i < 10 ? '0' : ''}${i}:00`,
}));

// Generate options 0-59 (step 5 or all)
const MINUTES = Array.from({ length: 60 }, (_, i) => ({
  value: i,
  label: `${i < 10 ? '0' : ''}${i} phút`,
}));

// Generate options 1-28 + 31 (Last day)
const DAYS_OF_MONTH = [
  ...Array.from({ length: 28 }, (_, i) => ({
    value: i + 1,
    label: `Ngày ${i + 1}`,
  })),
  { value: 31, label: 'Ngày cuối cùng trong tháng' },
];

export const ScheduleEditDialog = ({ open, onClose, schedule, onSave }: Props) => {
  const [visualState, setVisualState] = useState<VisualCronState>(() =>
    parseCronToVisualState(schedule?.cronExpression || '0 0 2 * * ?')
  );
  const [description, setDescription] = useState(schedule?.description || '');
  const [isActive, setIsActive] = useState(schedule?.isActive ?? true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync state when dialog opens or schedule changes
  useEffect(() => {
    if (open) {
      const parsed = parseCronToVisualState(schedule?.cronExpression || '0 0 2 * * ?');
      setVisualState(parsed);
      setDescription(schedule?.description || getCronDescriptionInVietnamese(parsed));
      setIsActive(schedule?.isActive ?? true);
      setError(null);
    }
  }, [open, schedule]);

  const currentCron = buildCronFromVisualState(visualState);
  const descriptionPreview = getCronDescriptionInVietnamese(visualState);

  const handleTypeChange = (newType: string) => {
    const updated = { ...visualState, type: newType as ScheduleType };
    setVisualState(updated);
    // Auto populate description if empty or default
    if (!description || description.startsWith('Sao lưu')) {
      setDescription(getCronDescriptionInVietnamese(updated));
    }
  };

  const handleSubmit = async () => {
    setError(null);
    const cronToSend = buildCronFromVisualState(visualState);

    if (!cronToSend.trim()) {
      setError('Biểu thức Cron không được để trống');
      return;
    }

    try {
      setLoading(true);
      await onSave({
        cronExpression: cronToSend,
        description: description || descriptionPreview,
        isActive,
      });
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lưu lịch sao lưu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-[560px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5 text-emerald-600" />
            {schedule ? 'Chỉnh sửa lịch sao lưu' : 'Thiết lập lịch sao lưu tự động'}
          </DialogTitle>
          <DialogDescription>
            Lựa chọn thời gian thực hiện sao lưu trực quan hoặc nhập biểu thức Cron tùy chỉnh.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5 py-2">
          {/* Schedule Frequency Selector Tabs */}
          <div className="space-y-2">
            <Label className="text-sm font-medium">Tần suất sao lưu</Label>
            <Tabs value={visualState.type} onValueChange={handleTypeChange} className="w-full">
              <TabsList className="grid w-full grid-cols-5">
                <TabsTrigger value="daily">Hàng ngày</TabsTrigger>
                <TabsTrigger value="weekly">Hàng tuần</TabsTrigger>
                <TabsTrigger value="monthly">Hàng tháng</TabsTrigger>
                <TabsTrigger value="interval">Định kỳ</TabsTrigger>
                <TabsTrigger value="custom">Nâng cao</TabsTrigger>
              </TabsList>
            </Tabs>
          </div>

          {/* Visual Configuration Controls based on Frequency */}
          <div className="rounded-lg border bg-card p-4 space-y-4">
            {/* Daily Mode */}
            {visualState.type === 'daily' && (
              <div className="space-y-2">
                <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                  Thời gian thực hiện trong ngày
                </Label>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="daily-hour" className="text-xs">Giờ</Label>
                    <Select
                      value={String(visualState.hour)}
                      onValueChange={(v) =>
                        v != null && setVisualState({ ...visualState, hour: parseInt(v, 10) })
                      }
                    >
                      <SelectTrigger id="daily-hour">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {HOURS.map((h) => (
                          <SelectItem key={h.value} value={String(h.value)}>
                            {h.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1">
                    <Label htmlFor="daily-min" className="text-xs">Phút</Label>
                    <Select
                      value={String(visualState.minute)}
                      onValueChange={(v) =>
                        v != null && setVisualState({ ...visualState, minute: parseInt(v, 10) })
                      }
                    >
                      <SelectTrigger id="daily-min">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="max-h-60">
                        {MINUTES.map((m) => (
                          <SelectItem key={m.value} value={String(m.value)}>
                            {m.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </div>
            )}

            {/* Weekly Mode */}
            {visualState.type === 'weekly' && (
              <div className="space-y-3">
                <div className="space-y-1">
                  <Label htmlFor="weekly-dow" className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    Thứ trong tuần
                  </Label>
                  <Select
                    value={visualState.dayOfWeek}
                    onValueChange={(v) => v != null && setVisualState({ ...visualState, dayOfWeek: v })}
                  >
                    <SelectTrigger id="weekly-dow">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {DAY_OF_WEEK_OPTIONS.map((dow) => (
                        <SelectItem key={dow.value} value={dow.value}>
                          {dow.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="weekly-hour" className="text-xs">Giờ thực hiện</Label>
                    <Select
                      value={String(visualState.hour)}
                      onValueChange={(v) =>
                        v != null && setVisualState({ ...visualState, hour: parseInt(v, 10) })
                      }
                    >
                      <SelectTrigger id="weekly-hour">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {HOURS.map((h) => (
                          <SelectItem key={h.value} value={String(h.value)}>
                            {h.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1">
                    <Label htmlFor="weekly-min" className="text-xs">Phút</Label>
                    <Select
                      value={String(visualState.minute)}
                      onValueChange={(v) =>
                        v != null && setVisualState({ ...visualState, minute: parseInt(v, 10) })
                      }
                    >
                      <SelectTrigger id="weekly-min">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="max-h-60">
                        {MINUTES.map((m) => (
                          <SelectItem key={m.value} value={String(m.value)}>
                            {m.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </div>
            )}

            {/* Monthly Mode */}
            {visualState.type === 'monthly' && (
              <div className="space-y-3">
                <div className="space-y-1">
                  <Label htmlFor="monthly-dom" className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    Ngày trong tháng
                  </Label>
                  <Select
                    value={String(visualState.dayOfMonth)}
                    onValueChange={(v) =>
                      v != null && setVisualState({ ...visualState, dayOfMonth: parseInt(v, 10) })
                    }
                  >
                    <SelectTrigger id="monthly-dom">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent className="max-h-60">
                      {DAYS_OF_MONTH.map((d) => (
                        <SelectItem key={d.value} value={String(d.value)}>
                          {d.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="monthly-hour" className="text-xs">Giờ thực hiện</Label>
                    <Select
                      value={String(visualState.hour)}
                      onValueChange={(v) =>
                        v != null && setVisualState({ ...visualState, hour: parseInt(v, 10) })
                      }
                    >
                      <SelectTrigger id="monthly-hour">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {HOURS.map((h) => (
                          <SelectItem key={h.value} value={String(h.value)}>
                            {h.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1">
                    <Label htmlFor="monthly-min" className="text-xs">Phút</Label>
                    <Select
                      value={String(visualState.minute)}
                      onValueChange={(v) =>
                        v != null && setVisualState({ ...visualState, minute: parseInt(v, 10) })
                      }
                    >
                      <SelectTrigger id="monthly-min">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="max-h-60">
                        {MINUTES.map((m) => (
                          <SelectItem key={m.value} value={String(m.value)}>
                            {m.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </div>
            )}

            {/* Interval Mode */}
            {visualState.type === 'interval' && (
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <Label htmlFor="interval-hours" className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    Tần suất lặp lại
                  </Label>
                  <Select
                    value={String(visualState.intervalHours)}
                    onValueChange={(v) =>
                      v != null && setVisualState({ ...visualState, intervalHours: parseInt(v, 10) })
                    }
                  >
                    <SelectTrigger id="interval-hours">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {INTERVAL_HOUR_OPTIONS.map((opt) => (
                        <SelectItem key={opt.value} value={String(opt.value)}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1">
                  <Label htmlFor="interval-min" className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    Phút thực hiện
                  </Label>
                  <Select
                    value={String(visualState.minute)}
                    onValueChange={(v) =>
                      v != null && setVisualState({ ...visualState, minute: parseInt(v, 10) })
                    }
                  >
                    <SelectTrigger id="interval-min">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent className="max-h-60">
                      {MINUTES.map((m) => (
                        <SelectItem key={m.value} value={String(m.value)}>
                          {m.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            )}

            {/* Custom Mode */}
            {visualState.type === 'custom' && (
              <div className="space-y-2">
                <Label htmlFor="custom-cron" className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                  Biểu thức Cron thủ công (Spring Format - 6 trường)
                </Label>
                <Input
                  id="custom-cron"
                  value={visualState.customCron}
                  onChange={(e) =>
                    setVisualState({ ...visualState, customCron: e.target.value })
                  }
                  placeholder="VD: 0 0 2 * * ?"
                  className="font-mono text-sm"
                />
                <p className="text-xs text-muted-foreground">
                  Ví dụ: <code className="bg-muted px-1 rounded">0 0 2 * * ?</code> (2 giờ sáng hàng ngày)
                </p>
              </div>
            )}

            {/* Realtime Cron & Description Live Preview Card */}
            <div className="mt-3 border-t pt-3 space-y-2">
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span className="flex items-center gap-1">
                  <Code2 className="h-3.5 w-3.5 text-blue-500" />
                  Biểu thức Cron sinh ra:
                </span>
                <code className="bg-muted font-mono font-bold px-2 py-0.5 rounded text-primary text-xs">
                  {currentCron}
                </code>
              </div>

              <div className="flex items-start gap-1.5 text-xs text-emerald-600 bg-emerald-50 dark:bg-emerald-950/30 p-2.5 rounded-md border border-emerald-200 dark:border-emerald-800">
                <Sparkles className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-medium">{descriptionPreview}</span>
              </div>
            </div>
          </div>

          {/* Description Textarea */}
          <div className="space-y-2">
            <Label htmlFor="desc">Mô tả lịch sao lưu</Label>
            <Textarea
              id="desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Nhập mô tả ngắn cho lịch sao lưu..."
              rows={2}
            />
          </div>

          {/* Active Switch */}
          <div className="flex items-center justify-between rounded-lg border p-3">
            <div className="space-y-0.5">
              <Label htmlFor="active" className="text-sm font-medium">
                Trạng thái kích hoạt
              </Label>
              <p className="text-xs text-muted-foreground">
                Bật để hệ thống tự động chạy lịch sao lưu theo thời gian đã cài đặt.
              </p>
            </div>
            <Switch id="active" checked={isActive} onCheckedChange={setIsActive} />
          </div>

          {error && <p className="text-sm text-red-500 font-medium">{error}</p>}
        </div>

        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Hủy
          </Button>
          <Button onClick={handleSubmit} disabled={loading} variant="create">
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {schedule ? 'Cập nhật' : 'Tạo mới'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};