import axios from 'axios';
import {
  AlertTriangle,
  CheckCircle2,
  LoaderCircle,
  PencilLine,
} from 'lucide-react';
import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';

import { correctFarmLog, getFarmLogById } from '@/api/farmLogApi';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type {
  FarmActivityType,
  FarmLog,
  FarmLogCorrectionData,
} from '@/types/farmLog';

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

const getActivityLabel = (value: FarmActivityType) =>
  ACTIVITY_OPTIONS.find((option) => option.value === value)?.label ?? value;

export default function CorrectFarmLogPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [log, setLog] = useState<FarmLog | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const [form, setForm] = useState<FormState>({
    activityType: '',
    material: '',
    quantity: '',
    unit: '',
    executedDate: '',
    notes: '',
    reason: '',
  });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const goBack = () => navigate(-1);

  useEffect(() => {
    if (!id) return;

    let cancelled = false;

    (async () => {
      try {
        const data = await getFarmLogById(id);
        if (cancelled) return;
        setLog(data);
        setForm({
          activityType: data.activityType,
          material: data.material ?? '',
          quantity: data.quantity != null ? String(data.quantity) : '',
          unit: data.unit ?? '',
          executedDate: data.executedDate,
          notes: data.notes ?? '',
          reason: '',
        });
      } catch (fetchError) {
        if (cancelled) return;
        setLoadError(
          getErrorMessage(
            fetchError,
            'Không thể tải thông tin nhật ký canh tác.',
          ),
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [id]);

  const breadcrumbItems = useMemo(() => {
    return [
      { label: 'Tổng quan', href: '/dashboard' },
      { label: 'Lô sản xuất', href: '/production-lots' },
      ...(log?.productionLotId
        ? [
            {
              label: log.productionLotName || 'Chi tiết lô',
              href: `/production-lots/${log.productionLotId}`,
            },
            {
              label: 'Nhật ký canh tác',
              href: `/production-lots/${log.productionLotId}`,
            },
          ]
        : []),
      { label: 'Đính chính' },
    ];
  }, [log]);

  useSetBreadcrumb(breadcrumbItems);

  /**
   * So sánh dữ liệu đang nhập với bản gốc để gửi đúng các trường thay đổi.
   * Giá trị rỗng được coi là bằng null của bản gốc.
   */
  const buildChangedFields = useMemo(() => {
    if (!log) return {};
    const changed: Partial<Record<
      | 'activityType'
      | 'material'
      | 'quantity'
      | 'unit'
      | 'executedDate'
      | 'notes',
      unknown
    >> = {};
    const originalMaterial = log.material ?? '';
    const originalUnit = log.unit ?? '';
    const originalNotes = log.notes ?? '';
    const originalQuantityText =
      log.quantity != null ? String(log.quantity) : '';

    if ((form.activityType || '') !== log.activityType) {
      changed.activityType = form.activityType as FarmActivityType;
    }
    if (form.material !== originalMaterial) {
      changed.material = form.material.trim();
    }
    if (form.quantity !== originalQuantityText) {
      const parsed =
        form.quantity.trim() === '' ? null : Number(form.quantity);
      if (parsed !== null && form.quantity.trim() !== '') {
        changed.quantity = parsed;
      }
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
      await correctFarmLog(id!, {
        correctionData: buildChangedFields as FarmLogCorrectionData,
        reason: form.reason.trim(),
      });

      toast.success('Đính chính nhật ký canh tác thành công');
      goBack();
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

  // Loading
  if (loading) {
    return (
      <Card className="border-slate-200 bg-white shadow-sm">
        <CardContent className="grid min-h-80 place-items-center p-8 text-center">
          <div>
            <LoaderCircle className="mx-auto size-8 animate-spin text-emerald-700" />
            <p className="mt-4 font-semibold">
              Đang tải thông tin nhật ký canh tác...
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Lỗi tải
  if (loadError || !log) {
    return (
      <Card className="border-red-200 bg-white shadow-sm">
        <CardContent className="grid min-h-80 place-items-center p-8 text-center">
          <div className="max-w-md">
            <AlertTriangle className="mx-auto size-10 text-red-500" />
            <h2 className="mt-4 text-lg font-bold text-red-800">
              Không thể tải nhật ký canh tác
            </h2>
            <p className="mt-2 text-sm leading-6 text-red-700">
              {loadError || 'Không tìm thấy nhật ký canh tác.'}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Đã bị đính chính - không cho phép sửa tiếp
  if (log.isCorrected) {
    return (
      <Card className="border-amber-200 bg-amber-50 shadow-sm">
        <CardContent className="grid min-h-80 place-items-center p-8 text-center">
          <div className="max-w-md">
            <CheckCircle2 className="mx-auto size-10 text-amber-600" />
            <h2 className="mt-4 text-lg font-bold text-amber-800">
              Nhật ký đã được đính chính
            </h2>
            <p className="mt-2 text-sm leading-6 text-amber-700">
              Bản ghi này đã bị thay thế hiệu lực bởi một bản đính chính khác.
              Vui lòng xem bản ghi đính chính mới nhất trong lịch sử nhật ký.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight text-slate-900">
          <PencilLine className="size-6 text-amber-600" />
          Đính chính nhật ký canh tác
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Bản gốc được giữ nguyên trong lịch sử. Bản đính chính sẽ thay thế giá
          trị hiệu lực và yêu cầu lý do bắt buộc.
        </p>
      </header>

      <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 text-sm text-blue-800">
        <p className="font-medium">
          📋 Hệ thống áp dụng nguyên tắc số hóa GAP
        </p>
        <p className="mt-1 leading-relaxed">
          Bản ghi gốc sẽ được giữ nguyên để lưu vết lịch sử và không bị xóa. Chỉ
          tạo bản đính chính mới với lý do rõ ràng. Mọi thay đổi đều được ghi
          nhận người sửa và thời gian.
        </p>
      </div>

      <form onSubmit={handleSubmit} noValidate>
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {/* Cột trái: giá trị gốc (chỉ đọc) */}
          <Card className="border-slate-200 bg-slate-50 shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">
                Giá trị gốc{' '}
                <span className="text-xs font-normal text-muted-foreground">
                  (chỉ đọc)
                </span>
              </CardTitle>
              <CardDescription>
                Thông tin được ghi nhận ban đầu, giữ nguyên sau khi đính chính.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <dl className="divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
                <div className="flex items-start justify-between gap-4 px-4 py-3">
                  <dt className="text-slate-500">Loại hoạt động</dt>
                  <dd className="text-right font-semibold">
                    {getActivityLabel(log.activityType)}
                  </dd>
                </div>
                <div className="flex items-start justify-between gap-4 px-4 py-3">
                  <dt className="text-slate-500">Ngày thực hiện</dt>
                  <dd className="text-right font-semibold">
                    {formatDate(log.executedDate)}
                  </dd>
                </div>
                <div className="flex items-start justify-between gap-4 px-4 py-3">
                  <dt className="text-slate-500">Vật tư</dt>
                  <dd className="text-right font-semibold">
                    {log.material ?? '—'}
                  </dd>
                </div>
                <div className="flex items-start justify-between gap-4 px-4 py-3">
                  <dt className="text-slate-500">Số lượng</dt>
                  <dd className="text-right font-semibold">
                    {log.quantity != null ? formatQuantity(log.quantity) : '—'}{' '}
                    {log.unit ?? ''}
                  </dd>
                </div>
                <div className="flex items-start justify-between gap-4 px-4 py-3">
                  <dt className="text-slate-500">Đơn vị</dt>
                  <dd className="text-right font-semibold">
                    {log.unit ?? '—'}
                  </dd>
                </div>
                <div className="flex flex-col gap-1 px-4 py-3">
                  <dt className="text-slate-500">Ghi chú</dt>
                  <dd className="whitespace-pre-wrap text-right font-semibold">
                    {log.notes ?? '—'}
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>


          {/* Cột phải: biểu mẫu đính chính */}
          <Card className="border-slate-200 bg-white shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">
                Thông tin đính chính
              </CardTitle>
              <CardDescription>
                Chỉ các trường thay đổi mới được gửi lên hệ thống. Trường không
                sửa sẽ giữ nguyên giá trị gốc.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="correction-activity-type">
                    Loại hoạt động
                  </Label>
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
                  <Label htmlFor="correction-executed-date">
                    Ngày thực hiện
                  </Label>
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
                      setForm((prev) => ({
                        ...prev,
                        material: event.target.value,
                      }))
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
                        setForm((prev) => ({
                          ...prev,
                          unit: event.target.value,
                        }))
                      }
                    />
                  </div>
                </div>

                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="correction-notes">Ghi chú</Label>
                  <Textarea
                    id="correction-notes"
                    rows={3}
                    value={form.notes}
                    onChange={(event) =>
                      setForm((prev) => ({
                        ...prev,
                        notes: event.target.value,
                      }))
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
                  rows={4}
                  maxLength={500}
                  placeholder="Mô tả rõ lý do cần đính chính bản ghi này..."
                  value={form.reason}
                  onChange={(event) =>
                    setForm((prev) => ({
                      ...prev,
                      reason: event.target.value,
                    }))
                  }
                />
                <p className="text-xs text-muted-foreground">
                  Lý do sẽ được lưu cùng bản ghi đính chính và hiển thị trong
                  hồ sơ truy xuất.
                </p>
              </div>

              {error && (
                <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                  {error}
                </p>
              )}
            </CardContent>
            <CardFooter className="flex justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={goBack}
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
            </CardFooter>
          </Card>
        </div>
      </form>
    </div>
  );
}
