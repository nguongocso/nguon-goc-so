import axios from 'axios';
import {
  CheckCircle2,
  ClipboardList,
  Info,
  Sprout,
} from 'lucide-react';
import {
  useMemo,
  useState,
  type FormEvent,
} from 'react';

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
  CreateFarmLogRequest,
  FarmActivityType,
  FarmLogResponse,
} from '@/types/farmLog';
import type { ProductionLot } from '@/types/productionLot';

interface CreateFarmLogFormProps {
  productionLots: ProductionLot[];
  initialProductionLotId?: string;
  onCancel: () => void;
  onSubmit: (
    payload: CreateFarmLogRequest,
  ) => Promise<FarmLogResponse>;
}

interface FormState {
  productionLotId: string;
  activityType: FarmActivityType | '';
  material: string;
  quantity: string;
  unit: string;
  executedDate: string;
  notes: string;
}

interface FormErrors {
  productionLotId?: string;
  activityType?: string;
  material?: string;
  quantity?: string;
  unit?: string;
  executedDate?: string;
  notes?: string;
}

interface ApiErrorResponse {
  message?: string;
  errors?: Record<string, string>;
}

const ACTIVITY_OPTIONS: Array<{
  value: FarmActivityType;
  label: string;
}> = [
  { value: 'PLANTING', label: 'Gieo trồng' },
  { value: 'WATERING', label: 'Tưới nước' },
  { value: 'FERTILIZING', label: 'Bón phân' },
  { value: 'PESTICIDE', label: 'Phun thuốc' },
  { value: 'WEEDING', label: 'Làm cỏ' },
  { value: 'HARVESTING', label: 'Thu hoạch' },
  { value: 'OTHER', label: 'Khác' },
];

const getToday = () => {
  const now = new Date();
  const localDate = new Date(
    now.getTime() - now.getTimezoneOffset() * 60_000,
  );

  return localDate.toISOString().slice(0, 10);
};

const createInitialForm = (
  initialProductionLotId?: string,
): FormState => ({
  productionLotId: initialProductionLotId ?? '',
  activityType: '',
  material: '',
  quantity: '',
  unit: '',
  executedDate: getToday(),
  notes: '',
});

const selectClassName =
  'h-10 w-full rounded-lg border border-input bg-white px-3 text-sm outline-none transition focus:border-emerald-600 focus:ring-3 focus:ring-emerald-100 aria-invalid:border-red-500 aria-invalid:ring-red-100';

const formatDate = (value: string | null) => {
  if (!value) return '—';

  return new Intl.DateTimeFormat('vi-VN').format(
    new Date(`${value}T00:00:00`),
  );
};

const formatQuantity = (value: number) =>
  value.toLocaleString('vi-VN', {
    maximumFractionDigits: 2,
  });

export function CreateFarmLogForm({
  productionLots,
  initialProductionLotId,
  onCancel,
  onSubmit,
}: CreateFarmLogFormProps) {
  const [form, setForm] = useState<FormState>(() =>
    createInitialForm(initialProductionLotId),
  );
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitError, setSubmitError] = useState('');
  const [createdLog, setCreatedLog] =
    useState<FarmLogResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedLot = useMemo(
    () =>
      productionLots.find(
        (lot) => lot.id === form.productionLotId,
      ),
    [form.productionLotId, productionLots],
  );

  const updateField = <K extends keyof FormState>(
    field: K,
    value: FormState[K],
  ) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
    setErrors((current) => ({
      ...current,
      [field]: undefined,
    }));
    setSubmitError('');
    setCreatedLog(null);
  };

  const validate = () => {
    const nextErrors: FormErrors = {};

    if (!form.productionLotId) {
      nextErrors.productionLotId =
        'Vui lòng chọn lô sản xuất.';
    }

    if (!form.activityType) {
      nextErrors.activityType =
        'Vui lòng chọn loại hoạt động.';
    }

    if (!form.executedDate) {
      nextErrors.executedDate =
        'Vui lòng chọn ngày thực hiện.';
    }

    if (form.material.trim().length > 255) {
      nextErrors.material =
        'Tên vật tư không được vượt quá 255 ký tự.';
    }

    if (form.quantity) {
      const quantity = Number(form.quantity);

      if (!Number.isFinite(quantity) || quantity <= 0) {
        nextErrors.quantity =
          'Số lượng phải lớn hơn 0.';
      }
    }

    if (form.unit.trim().length > 50) {
      nextErrors.unit =
        'Đơn vị không được vượt quá 50 ký tự.';
    }

    if (form.notes.trim().length > 1000) {
      nextErrors.notes =
        'Ghi chú không được vượt quá 1000 ký tự.';
    }

    setErrors(nextErrors);

    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();
    setSubmitError('');
    setCreatedLog(null);

    if (!validate() || !form.activityType) return;

    setIsSubmitting(true);

    try {
      const result = await onSubmit({
        productionLotId: form.productionLotId,
        activityType: form.activityType,
        material: form.material.trim() || null,
        quantity: form.quantity
          ? Number(form.quantity)
          : null,
        unit: form.unit.trim() || null,
        executedDate: form.executedDate,
        notes: form.notes.trim() || null,
      });

      setCreatedLog(result);
      setForm((current) => ({
        ...createInitialForm(current.productionLotId),
        executedDate: current.executedDate,
      }));
      setErrors({});
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const responseData = error.response?.data;

        if (responseData?.errors) {
          setErrors((current) => ({
            ...current,
            productionLotId:
              responseData.errors?.productionLotId,
            activityType:
              responseData.errors?.activityType,
            material: responseData.errors?.material,
            quantity: responseData.errors?.quantity,
            unit: responseData.errors?.unit,
            executedDate:
              responseData.errors?.executedDate,
            notes: responseData.errors?.notes,
          }));
        }

        setSubmitError(
          responseData?.message
            || 'Không thể lưu nhật ký. Vui lòng kiểm tra lại dữ liệu.',
        );
      } else {
        setSubmitError(
          'Không thể kết nối đến máy chủ. Vui lòng thử lại.',
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
      <Card className="border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 px-6 pb-5 sm:px-8">
          <CardTitle className="flex items-center gap-2 text-lg font-bold">
            <ClipboardList className="size-5 text-emerald-700" />
            Thông tin nhật ký
          </CardTitle>
          <CardDescription>
            Các trường có dấu{' '}
            <span className="text-red-600">*</span> là bắt
            buộc.
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit} noValidate>
          <CardContent className="space-y-7 px-6 py-6 sm:px-8">
            <div className="space-y-2">
              <Label htmlFor="productionLotId">
                Lô sản xuất{' '}
                <span className="text-red-600">*</span>
              </Label>
              <select
                id="productionLotId"
                className={selectClassName}
                value={form.productionLotId}
                onChange={(event) =>
                  updateField(
                    'productionLotId',
                    event.target.value,
                  )
                }
                aria-invalid={Boolean(
                  errors.productionLotId,
                )}
              >
                <option value="">Chọn lô sản xuất</option>
                {productionLots.map((lot) => (
                  <option key={lot.id} value={lot.id}>
                    {lot.name} ·{' '}
                    {lot.status === 'APPROVED'
                      ? 'Đã duyệt'
                      : 'Đã thu hoạch'}
                  </option>
                ))}
              </select>
              <p className="text-xs leading-5 text-slate-500">
                Chỉ hiển thị lô đã duyệt hoặc đã thu hoạch
                trong tổ chức của bạn.
              </p>
              {errors.productionLotId && (
                <p className="text-xs text-red-600">
                  {errors.productionLotId}
                </p>
              )}
            </div>

            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="activityType">
                  Loại hoạt động{' '}
                  <span className="text-red-600">*</span>
                </Label>
                <select
                  id="activityType"
                  className={selectClassName}
                  value={form.activityType}
                  onChange={(event) =>
                    updateField(
                      'activityType',
                      event.target.value as
                        | FarmActivityType
                        | '',
                    )
                  }
                  aria-invalid={Boolean(
                    errors.activityType,
                  )}
                >
                  <option value="">
                    Chọn loại hoạt động
                  </option>
                  {ACTIVITY_OPTIONS.map((activity) => (
                    <option
                      key={activity.value}
                      value={activity.value}
                    >
                      {activity.label}
                    </option>
                  ))}
                </select>
                {errors.activityType && (
                  <p className="text-xs text-red-600">
                    {errors.activityType}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="executedDate">
                  Ngày thực hiện{' '}
                  <span className="text-red-600">*</span>
                </Label>
                <Input
                  id="executedDate"
                  type="date"
                  value={form.executedDate}
                  onChange={(event) =>
                    updateField(
                      'executedDate',
                      event.target.value,
                    )
                  }
                  aria-invalid={Boolean(
                    errors.executedDate,
                  )}
                />
                {errors.executedDate && (
                  <p className="text-xs text-red-600">
                    {errors.executedDate}
                  </p>
                )}
              </div>
            </div>

            <section className="border-t border-slate-100 pt-7">
              <h3 className="mb-5 flex items-center gap-2 font-bold">
                <Sprout className="size-4 text-emerald-700" />
                Vật tư sử dụng
                <span className="text-sm font-normal text-slate-400">
                  (không bắt buộc)
                </span>
              </h3>

              <div className="grid gap-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(140px,0.7fr)_minmax(120px,0.6fr)]">
                <div className="space-y-2">
                  <Label htmlFor="material">
                    Tên vật tư
                  </Label>
                  <Input
                    id="material"
                    maxLength={255}
                    value={form.material}
                    onChange={(event) =>
                      updateField(
                        'material',
                        event.target.value,
                      )
                    }
                    aria-invalid={Boolean(errors.material)}
                    placeholder="Ví dụ: NPK 16-16-8"
                  />
                  {errors.material && (
                    <p className="text-xs text-red-600">
                      {errors.material}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="quantity">
                    Số lượng
                  </Label>
                  <Input
                    id="quantity"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.quantity}
                    onChange={(event) =>
                      updateField(
                        'quantity',
                        event.target.value,
                      )
                    }
                    aria-invalid={Boolean(errors.quantity)}
                    placeholder="Ví dụ: 25"
                  />
                  {errors.quantity && (
                    <p className="text-xs text-red-600">
                      {errors.quantity}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="unit">Đơn vị</Label>
                  <Input
                    id="unit"
                    maxLength={50}
                    value={form.unit}
                    onChange={(event) =>
                      updateField(
                        'unit',
                        event.target.value,
                      )
                    }
                    aria-invalid={Boolean(errors.unit)}
                    placeholder="Ví dụ: kg"
                  />
                  {errors.unit && (
                    <p className="text-xs text-red-600">
                      {errors.unit}
                    </p>
                  )}
                </div>
              </div>
            </section>

            <div className="space-y-2">
              <div className="flex items-center justify-between gap-4">
                <Label htmlFor="notes">Ghi chú</Label>
                <span className="text-xs text-slate-400">
                  {form.notes.length}/1000
                </span>
              </div>
              <Textarea
                id="notes"
                rows={5}
                maxLength={1000}
                className="min-h-32 resize-y"
                value={form.notes}
                onChange={(event) =>
                  updateField('notes', event.target.value)
                }
                aria-invalid={Boolean(errors.notes)}
                placeholder="Ví dụ: Bón phân lần 1 cho lô sản xuất..."
              />
              {errors.notes && (
                <p className="text-xs text-red-600">
                  {errors.notes}
                </p>
              )}
            </div>

            {submitError && (
              <div
                role="alert"
                className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
              >
                {submitError}
              </div>
            )}

            {createdLog && (
              <div
                role="status"
                className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
              >
                <CheckCircle2 className="mt-0.5 size-5 shrink-0" />
                <div>
                  <p className="font-bold">
                    Lưu nhật ký thành công
                  </p>
                  <p className="mt-1 leading-6">
                    Hoạt động đã được ghi cho lô{' '}
                    <strong>
                      {createdLog.productionLotName}
                    </strong>
                    . Bạn có thể tiếp tục ghi hoạt động khác
                    cho cùng lô.
                  </p>
                </div>
              </div>
            )}
          </CardContent>

          <CardFooter className="flex-col-reverse gap-3 border-t border-slate-100 px-6 py-5 sm:flex-row sm:justify-end sm:px-8">
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="w-full sm:w-auto"
              onClick={onCancel}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              size="lg"
              className="w-full bg-emerald-700 text-white hover:bg-emerald-800 sm:w-auto"
              disabled={isSubmitting}
              variant="create"
            >
              {isSubmitting
                ? 'Đang lưu...'
                : 'Lưu nhật ký'}
            </Button>
          </CardFooter>
        </form>
      </Card>

      <aside className="space-y-4 xl:sticky xl:top-6">
        <Card className="border-slate-200 bg-white shadow-sm">
          <CardHeader className="border-b border-slate-100 pb-4">
            <CardTitle className="text-base">
              Thông tin lô đã chọn
            </CardTitle>
            <CardDescription>
              Kiểm tra đúng lô trước khi lưu nhật ký.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5">
            {selectedLot ? (
              <div className="space-y-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                    Tên lô
                  </p>
                  <p className="mt-1 font-bold text-slate-900">
                    {selectedLot.name}
                  </p>
                </div>

                <dl className="divide-y divide-slate-100 text-sm">
                  <div className="flex justify-between gap-4 py-3">
                    <dt className="text-slate-500">
                      Vùng trồng
                    </dt>
                    <dd className="text-right font-semibold">
                      {selectedLot.farmAreaName ?? '—'}
                    </dd>
                  </div>
                  <div className="flex justify-between gap-4 py-3">
                    <dt className="text-slate-500">
                      Nông sản
                    </dt>
                    <dd className="text-right font-semibold">
                      {selectedLot.productCategoryName ?? '—'}
                    </dd>
                  </div>
                  <div className="flex justify-between gap-4 py-3">
                    <dt className="text-slate-500">
                      Ngày gieo trồng
                    </dt>
                    <dd className="text-right font-semibold">
                      {formatDate(selectedLot.plantingDate)}
                    </dd>
                  </div>
                  <div className="flex justify-between gap-4 py-3">
                    <dt className="text-slate-500">
                      Sản lượng dự kiến
                    </dt>
                    <dd className="text-right font-semibold">
                      {formatQuantity(
                        selectedLot.expectedQuantity,
                      )}{' '}
                      {selectedLot.expectedQuantityUnit}
                    </dd>
                  </div>
                  <div className="flex items-center justify-between gap-4 py-3">
                    <dt className="text-slate-500">
                      Trạng thái
                    </dt>
                    <dd>
                      <span
                        className={
                          selectedLot.status === 'APPROVED'
                            ? 'rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-bold text-emerald-700'
                            : 'rounded-full bg-lime-100 px-2.5 py-1 text-xs font-bold text-lime-700'
                        }
                      >
                        {selectedLot.status === 'APPROVED'
                          ? 'Đã duyệt'
                          : 'Đã thu hoạch'}
                      </span>
                    </dd>
                  </div>
                </dl>
              </div>
            ) : (
              <div className="py-8 text-center">
                <Sprout className="mx-auto size-9 text-slate-300" />
                <p className="mt-3 text-sm font-semibold text-slate-600">
                  Chưa chọn lô sản xuất
                </p>
                <p className="mt-1 text-xs leading-5 text-slate-400">
                  Thông tin lô sẽ xuất hiện tại đây.
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        <div className="flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-900">
          <Info className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="text-sm font-bold">
              Điều kiện ghi nhật ký
            </p>
            <ul className="mt-2 space-y-2 text-sm leading-6 text-emerald-800">
              <li>Chỉ áp dụng cho lô đã duyệt hoặc đã thu hoạch.</li>
              <li>Nhật ký được ghi theo tài khoản VT-03 hiện tại.</li>
              <li>Chứng từ được bổ sung sau khi nhật ký đã được lưu.</li>
            </ul>
          </div>
        </div>
      </aside>
    </div>
  );
}
