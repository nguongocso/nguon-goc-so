import axios from 'axios';
import { LoaderCircle, PencilLine } from 'lucide-react';
import { useMemo, useState, type FormEvent } from 'react';
import { toast } from 'sonner';

import { correctFarmLog } from '@/api/farmLogApi';
import { Button } from '@/components/ui/button';
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
import type {
  CorrectFarmLogRequest,
  FarmActivityType,
  FarmLog,
  FarmLogResponse,
} from '@/types/farmLog';

interface CorrectFarmLogDialogProps {
  log: FarmLog;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (response: FarmLogResponse) => void;
}

interface FormState {
  activityType: FarmActivityType | '';
  material: string;
  quantity: string;
  unit: string;
  executedDate: string;
  notes: string;
  reason: string;
}

interface ApiErrorResponse {
  message?: string;
}

const ACTIVITY_OPTIONS: Array<{ value: FarmActivityType; label: string }> = [
  { value: 'PLANTING', label: 'Gieo trồng' },
  { value: 'WATERING', label: 'Tưới nước' },
  { value: 'FERTILIZING', label: 'Bón phân' },
  { value: 'PESTICIDE', label: 'Phun thuốc' },
  { value: 'WEEDING', label: 'Làm cỏ' },
  { value: 'HARVESTING', label: 'Thu hoạch' },
  { value: 'OTHER', label: 'Khác' },
];

const getErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiErrorResponse | undefined;

    return data?.message || fallback;
  }

  return fallback;
};

const selectClassName =
  'h-10 w-full rounded-lg border border-input bg-white px-3 text-sm outline-none transition focus:border-emerald-600 focus:ring-3 focus:ring-emerald-100 aria-invalid:border-red-500 aria-invalid:ring-red-100';

const formatDate = (value: string | null) => {
  if (!value) return '—';

  return new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`));
};

const formatQuantity = (value: number) =>
  value.toLocaleString('vi-VN', { maximumFractionDigits: 2 });

export function CorrectFarmLogDialog({
  log,
  open,
  onOpenChange,
  onSuccess,
}: CorrectFarmLogDialogProps) {
  const [form, setForm] = useState<FormState>(() => ({
    activityType: log.activityType,
    material: log.material ?? '',
    quantity: log.quantity != null ? String(log.quantity) : '',
    unit: log.unit ?? '',
    executedDate: log.executedDate,
    notes: log.notes ?? '',
    reason: '',
  }));
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resetAndClose = () => {
    setForm({
      activityType: log.activityType,
      material: log.material ?? '',
      quantity: log.quantity != null ? String(log.quantity) : '',
      unit: log.unit ?? '',
      executedDate: log.executedDate,
      notes: log.notes ?? '',
      reason: '',
    });
    setError('');
    onOpenChange(false);
  };

  /**
   * So sánh dữ liệu đang nhập với bản gốc để gửi đúng các trường thay đổi.
   * Giá trị rỗng được coi là bằng null của bản gốc.
   */
  const buildChangedFields = useMemo(() => {
    const changed: Partial<CorrectFarmLogRequest['correctionData']> = {};
    const originalMaterial = log.material ?? '';
    const originalUnit = log.unit ?? '';
    const originalNotes = log.notes ?? '';

    if ((form.activityType || '') !== log.activityType) {
      changed.activityType = form.activityType as FarmActivityType;
    }
    if (form.material !== originalMaterial) {
      changed.material = form.material.trim();
    }
    const parsedQuantity =
      form.quantity.trim() === '' ? null : Number(form.quantity);
    const originalQuantityText =
      log.quantity != null ? String(log.quantity) : '';

    if (form.quantity !== originalQuantityText && parsedQuantity != null) {
      changed.quantity = parsedQuantity;
    }
    if (form.unit !== originalUnit) {
      changed.unit = form.unit.trim();
    }
    if (form.executedDate !== log.executedDate) {
      changed.executedDate = form.executedDate;
    }
    if (form.notes !== originalNotes) {
      changed.notes = form.notes.trim();
    }

    return changed;
  }, [form, log]);

  const validate = (): string | null => {
    if (!form.reason.trim()) {
      return 'Lý do đính chính không được để trống.';
    }
    if (form.reason.trim().length > 500) {
      return 'Lý do đính chính không được vượt quá 500 ký tự.';
    }
    if (
      form.quantity.trim() !== '' &&
      (Number.isNaN(Number(form.quantity)) || Number(form.quantity) <= 0)
    ) {
      return 'Số lượng phải lớn hơn 0.';
    }
    if (Object.keys(buildChangedFields).length === 0) {
      return 'Phải có ít nhất một trường được đính chính so với bản gốc.';
    }

    return null;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    const validationError = validate();

    if (validationError) {
      setError(validationError);
      return;
    }

    try {
      setIsSubmitting(true);
      const response = await correctFarmLog(log.id, {
        correctionData: buildChangedFields,
        reason: form.reason.trim(),
      });

      toast.success('Đính chính nhật ký canh tác thành công');
      onSuccess?.(response);
      resetAndClose();
    } catch (submitError) {
      setError(
        getErrorMessage(
          submitError,
          'Không thể đính chính nhật ký. Vui lòng thử lại.',
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <PencilLine className="size-5 text-amber-600" />
            Đính chính nhật ký canh tác
          </DialogTitle>
          <DialogDescription>
            Bản gốc được giữ nguyên trong lịch sử. Bản đính chính sẽ thay thế
            giá trị hiệu lực và yêu cầu lý do bắt buộc.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-5" noValidate>
          {/* Bản gốc - chỉ đọc để đối chiếu */}
          <section className="rounded-lg border border-slate-200 bg-slate-50 p-4">
            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
              Giá trị gốc (chỉ đọc)
            </p>
            <dl className="mt-3 grid grid-cols-1 gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500">Hoạt động</dt>
                <dd className="font-semibold">
                  {ACTIVITY_OPTIONS.find(
                    (option) => option.value === log.activityType,
                  )?.label ?? log.activityType}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500">Ngày thực hiện</dt>
                <dd className="font-semibold">{formatDate(log.executedDate)}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500">Vật tư</dt>
                <dd className="font-semibold">{log.material ?? '—'}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-500">Số lượng</dt>
                <dd className="font-semibold">
                  {log.quantity != null ? formatQuantity(log.quantity) : '—'}{' '}
                  {log.unit ?? ''}
                </dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-slate-500">Ghi chú</dt>
                <dd className="whitespace-pre-wrap font-semibold">
                  {log.notes ?? '—'}
                </dd>
              </div>
            </dl>
          </section>

          {/* Dữ liệu đính chính */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="correction-activity-type">Loại hoạt động</Label>
              <select
                id="correction-activity-type"
                className={selectClassName}
                value={form.activityType}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    activityType: event.target.value as FarmActivityType,
                  }))
                }
              >
                {ACTIVITY_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="correction-executed-date">Ngày thực hiện</Label>
              <Input
                id="correction-executed-date"
                type="date"
                value={form.executedDate}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    executedDate: event.target.value,
                  }))
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="correction-material">Vật tư</Label>
              <Input
                id="correction-material"
                value={form.material}
                placeholder="Ví dụ: NPK 16-16-8"
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, material: event.target.value }))
                }
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="correction-quantity">Số lượng</Label>
                <Input
                  id="correction-quantity"
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.quantity}
                  onChange={(event) =>
                    setForm((prev) => ({
                      ...prev,
                      quantity: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="correction-unit">Đơn vị</Label>
                <Input
                  id="correction-unit"
                  value={form.unit}
                  placeholder="kg"
                  onChange={(event) =>
                    setForm((prev) => ({ ...prev, unit: event.target.value }))
                  }
                />
              </div>
            </div>

            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="correction-notes">Ghi chú</Label>
              <Textarea
                id="correction-notes"
                rows={2}
                value={form.notes}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, notes: event.target.value }))
                }
              />
            </div>
          </div>

          {/* Lý do đính chính - bắt buộc */}
          <div className="space-y-2">
            <Label htmlFor="correction-reason">
              Lý do đính chính{' '}
              <span aria-hidden className="text-red-500">
                *
              </span>
            </Label>
            <Textarea
              id="correction-reason"
              required
              rows={3}
              maxLength={500}
              placeholder="Mô tả rõ lý do cần đính chính bản ghi này..."
              value={form.reason}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, reason: event.target.value }))
              }
            />
            <p className="text-xs text-muted-foreground">
              Lý do sẽ được lưu cùng bản ghi đính chính và hiển thị trong hồ sơ
              truy xuất.
            </p>
          </div>

          {error && (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <DialogFooter className="gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={resetAndClose}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={
                isSubmitting || Object.keys(buildChangedFields).length === 0
              }
            >
              {isSubmitting ? (
                <>
                  <LoaderCircle className="mr-2 size-4 animate-spin" />
                  Đang lưu...
                </>
              ) : (
                'Lưu đính chính'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
