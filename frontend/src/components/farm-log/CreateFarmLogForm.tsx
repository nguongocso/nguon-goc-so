import axios from 'axios';
import {
  ArrowLeft,
  CheckCircle2,
  ClipboardList,
  FileText,
  Info,
  Paperclip,
  Sprout,
  Upload,
  X,
} from 'lucide-react';
import {
  useMemo,
  useState,
  type FormEvent,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

import { uploadAttachment } from '@/api/attachmentApi';
import { AttachmentManager } from './AttachmentManager';

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
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>(() =>
    createInitialForm(initialProductionLotId),
  );
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitError, setSubmitError] = useState('');
  const [createdLog, setCreatedLog] =
    useState<FarmLogResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const MAX_ATTACHMENTS = 5;
  const [attachmentFiles, setAttachmentFiles] = useState<File[]>([]);
  const [filePreviews, setFilePreviews] = useState<string[]>([]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;

    const fileArray = Array.from(files);
    const validTypes = ['image/jpeg', 'image/png', 'application/pdf'];

    for (const f of fileArray) {
      if (!validTypes.includes(f.type)) {
        toast.error(`Tệp "${f.name}" không hỗ trợ. Chỉ nhận JPG, PNG, PDF.`);
        return;
      }
      if (f.size > 5 * 1024 * 1024) {
        toast.error(`Tệp "${f.name}" vượt quá 5MB.`);
        return;
      }
    }

    if (attachmentFiles.length + fileArray.length > MAX_ATTACHMENTS) {
      toast.error(`Chỉ được chọn tối đa ${MAX_ATTACHMENTS} chứng từ.`);
      return;
    }

    setAttachmentFiles((prev) => [...prev, ...fileArray]);
    const newPreviews = fileArray.map((f) =>
      f.type.startsWith('image/') ? URL.createObjectURL(f) : ''
    );
    setFilePreviews((prev) => [...prev, ...newPreviews]);
  };

  const removeFile = (index: number) => {
    setAttachmentFiles((prev) => prev.filter((_, i) => i !== index));
    setFilePreviews((prev) => {
      if (prev[index]) URL.revokeObjectURL(prev[index]);
      return prev.filter((_, i) => i !== index);
    });
  };

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
  };

  const validate = (): boolean => {
    const nextErrors: FormErrors = {};

    if (!form.productionLotId) {
      nextErrors.productionLotId = 'Vui lòng chọn lô sản xuất.';
    }

    if (!form.activityType) {
      nextErrors.activityType = 'Vui lòng chọn loại hoạt động.';
    }

    if (!form.executedDate) {
      nextErrors.executedDate = 'Vui lòng chọn ngày thực hiện.';
    } else if (
      new Date(`${form.executedDate}T00:00:00`) > new Date()
    ) {
      nextErrors.executedDate =
        'Ngày thực hiện không được vượt quá ngày hiện tại.';
    }

    if (form.quantity) {
      const parsed = Number.parseFloat(form.quantity);
      if (Number.isNaN(parsed) || parsed < 0) {
        nextErrors.quantity = 'Số lượng phải là số lớn hơn hoặc bằng 0.';
      }
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setSubmitError('');

    const payload: CreateFarmLogRequest = {
      productionLotId: form.productionLotId,
      activityType: form.activityType as FarmActivityType,
      material: form.material.trim() || null,
      quantity: form.quantity
        ? Number.parseFloat(form.quantity)
        : null,
      unit: form.unit.trim() || null,
      executedDate: form.executedDate,
      notes: form.notes.trim() || null,
    };

    try {
      const created = await onSubmit(payload);

      if (attachmentFiles.length > 0) {
        for (const file of attachmentFiles) {
          try {
            await uploadAttachment(created.id, file);
          } catch {
            toast.error(`Lỗi khi tải đính kèm "${file.name}"`);
          }
        }
      }

      setCreatedLog(created);
      // Toast đã được xóa – component cha sẽ xử lý thông báo thành công
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        const data = error.response?.data as ApiErrorResponse | undefined;

        if (data?.message) {
          setSubmitError(data.message);
        } else if (data?.errors) {
          const firstKey = Object.keys(data.errors)[0];
          setSubmitError(
            data.errors[firstKey] ??
              'Dữ liệu nhập vào chưa hợp lệ. Vui lòng kiểm tra lại.',
          );
        } else {
          setSubmitError('Có lỗi xảy ra khi tạo nhật ký. Vui lòng thử lại.');
        }
      } else {
        setSubmitError('Có lỗi không xác định xảy ra.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetFormToCreateAnother = () => {
    setCreatedLog(null);
    setForm(createInitialForm(initialProductionLotId));
    setErrors({});
    setSubmitError('');
    setAttachmentFiles([]);
    setFilePreviews([]);
  };

  if (createdLog) {
    return (
      <Card className="mx-auto max-w-2xl border-emerald-200 bg-emerald-50/40 shadow-sm">
        <CardHeader className="border-b border-emerald-100 pb-5">
          <div className="flex items-center gap-3 text-emerald-700">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100">
              <CheckCircle2 className="size-6 text-emerald-600" />
            </div>
            <div>
              <CardTitle className="text-xl font-bold">
                Tạo nhật ký thành công
              </CardTitle>
              <CardDescription className="text-emerald-700">
                Nhật ký hoạt động nông nghiệp đã được ghi nhận vào hệ thống.
              </CardDescription>
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-6 pt-6">
          <div className="rounded-xl border border-emerald-200 bg-white p-5 shadow-xs">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Hoạt động
                </p>
                <p className="text-lg font-bold text-slate-900">
                  {ACTIVITY_OPTIONS.find((a) => a.value === createdLog.activityType)?.label ?? createdLog.activityType}
                </p>
              </div>
              <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-700">
                Mã lô: {createdLog.productionLotId.slice(0, 8)}...
              </span>
            </div>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div>
                <dt className="text-xs font-semibold uppercase text-slate-400">
                  Vùng trồng
                </dt>
                <dd className="mt-0.5 text-sm font-semibold text-slate-800">
                  {selectedLot?.farmAreaName ?? '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-semibold uppercase text-slate-400">
                  Ngày thực hiện
                </dt>
                <dd className="mt-0.5 text-sm font-semibold text-slate-800">
                  {createdLog.executedDate}
                </dd>
              </div>
              {createdLog.material && (
                <div>
                  <dt className="text-xs font-semibold uppercase text-slate-400">
                    Vật tư
                  </dt>
                  <dd className="mt-0.5 text-sm font-semibold text-slate-800">
                    {createdLog.material}{' '}
                    {createdLog.quantity ? `(${createdLog.quantity} ${createdLog.unit ?? ''})` : ''}
                  </dd>
                </div>
              )}
            </div>

            {createdLog.notes && (
              <div className="mt-3 border-t border-slate-100 pt-3">
                <dt className="text-xs font-semibold uppercase text-slate-400">
                  Ghi chú
                </dt>
                <dd className="mt-1 text-sm text-slate-700">
                  {createdLog.notes}
                </dd>
              </div>
            )}
          </div>

          <AttachmentManager logId={createdLog.id} />
        </CardContent>

        <CardFooter className="flex flex-col gap-3 border-t border-emerald-100 pt-4 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={() => {
              if (createdLog.productionLotId) {
                navigate(
                  `/production-lots/${createdLog.productionLotId}/farm-logs`,
                );
              } else {
                onCancel();
              }
            }}
            className="group inline-flex w-full items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium text-emerald-700 underline-offset-4 transition-colors hover:text-emerald-900 hover:underline sm:w-auto"
          >
            <ArrowLeft className="h-4 w-4 transition-transform group-hover:-translate-x-0.5" />
            Quay lại lịch sử nhật ký canh tác
          </button>
          <Button
            type="button"
            variant="create"
            onClick={resetFormToCreateAnother}
            className="w-full sm:w-auto"
          >
            Tạo nhật ký mới
          </Button>
        </CardFooter>
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
      <div className="lg:col-span-8">
        <Card className="border-slate-200 shadow-sm">
          <CardHeader className="border-b border-slate-100 pb-5">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <ClipboardList className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl font-bold text-slate-900">
                  Ghi nhật ký sản xuất
                </CardTitle>
                <CardDescription className="text-slate-500">
                  Ghi nhận hoạt động tác nghiệp nông nghiệp cho lô sản xuất.
                </CardDescription>
              </div>
            </div>
          </CardHeader>

          <form onSubmit={handleSubmit} noValidate>
            <CardContent className="space-y-6 pt-6">
              {submitError && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                  <p className="font-bold">Không thể lưu nhật ký</p>
                  <p className="mt-1">{submitError}</p>
                </div>
              )}

              <div className="space-y-2">
                <Label
                  htmlFor="productionLotId"
                  className="font-semibold text-slate-800"
                >
                  Chọn lô sản xuất <span className="text-red-500">*</span>
                </Label>
                <select
                  id="productionLotId"
                  value={form.productionLotId}
                  onChange={(e) =>
                    updateField('productionLotId', e.target.value)
                  }
                  className={selectClassName}
                  aria-invalid={Boolean(errors.productionLotId)}
                >
                  <option value="">-- Chọn lô sản xuất --</option>
                  {productionLots.map((lot) => (
                    <option key={lot.id} value={lot.id}>
                      {lot.name}
                    </option>
                  ))}
                </select>
                {errors.productionLotId && (
                  <p className="text-xs font-semibold text-red-500">
                    {errors.productionLotId}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label
                    htmlFor="activityType"
                    className="font-semibold text-slate-800"
                  >
                    Loại hoạt động <span className="text-red-500">*</span>
                  </Label>
                  <select
                    id="activityType"
                    value={form.activityType}
                    onChange={(e) =>
                      updateField(
                        'activityType',
                        e.target.value as FarmActivityType,
                      )
                    }
                    className={selectClassName}
                    aria-invalid={Boolean(errors.activityType)}
                  >
                    <option value="">-- Chọn hoạt động --</option>
                    {ACTIVITY_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                  {errors.activityType && (
                    <p className="text-xs font-semibold text-red-500">
                      {errors.activityType}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label
                    htmlFor="executedDate"
                    className="font-semibold text-slate-800"
                  >
                    Ngày thực hiện <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="executedDate"
                    type="date"
                    value={form.executedDate}
                    max={getToday()}
                    onChange={(e) =>
                      updateField('executedDate', e.target.value)
                    }
                    className="h-10 border-slate-200 focus:border-emerald-600 focus:ring-emerald-100"
                    aria-invalid={Boolean(errors.executedDate)}
                  />
                  {errors.executedDate && (
                    <p className="text-xs font-semibold text-red-500">
                      {errors.executedDate}
                    </p>
                  )}
                </div>
              </div>

              <div className="space-y-2">
                <Label
                  htmlFor="material"
                  className="font-semibold text-slate-800"
                >
                  Tên vật tư / Phân bón / Thuốc BVTV
                </Label>
                <Input
                  id="material"
                  type="text"
                  placeholder="VD: Phun phân bón NPK 16-16-8, Thuốc trừ sâu Trichoderma..."
                  value={form.material}
                  onChange={(e) => updateField('material', e.target.value)}
                  className="h-10 border-slate-200 focus:border-emerald-600 focus:ring-emerald-100"
                />
              </div>

              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label
                    htmlFor="quantity"
                    className="font-semibold text-slate-800"
                  >
                    Số lượng
                  </Label>
                  <Input
                    id="quantity"
                    type="number"
                    step="any"
                    placeholder="VD: 50, 1.5..."
                    value={form.quantity}
                    onChange={(e) => updateField('quantity', e.target.value)}
                    className="h-10 border-slate-200 focus:border-emerald-600 focus:ring-emerald-100"
                    aria-invalid={Boolean(errors.quantity)}
                  />
                  {errors.quantity && (
                    <p className="text-xs font-semibold text-red-500">
                      {errors.quantity}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label
                    htmlFor="unit"
                    className="font-semibold text-slate-800"
                  >
                    Đơn vị tính
                  </Label>
                  <Input
                    id="unit"
                    type="text"
                    placeholder="VD: kg, lít, bao, chai, ngày công..."
                    value={form.unit}
                    onChange={(e) => updateField('unit', e.target.value)}
                    className="h-10 border-slate-200 focus:border-emerald-600 focus:ring-emerald-100"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label
                  htmlFor="notes"
                  className="font-semibold text-slate-800"
                >
                  Ghi chú chi tiết
                </Label>
                <Textarea
                  id="notes"
                  rows={3}
                  placeholder="Mô tả chi tiết tình hình thời tiết, diện tích xử lý, phương pháp thực hiện..."
                  value={form.notes}
                  onChange={(e) => updateField('notes', e.target.value)}
                  className="border-slate-200 focus:border-emerald-600 focus:ring-emerald-100"
                />
              </div>

              <div className="space-y-3 rounded-xl border border-slate-200 bg-slate-50/50 p-4">
                <div className="flex items-center justify-between">
                  <Label className="flex items-center gap-2 font-semibold text-slate-800">
                    <Paperclip className="size-4 text-emerald-600" />
                    Chứng từ đính kèm (Hình ảnh, Hóa đơn, Chứng nhận)
                  </Label>
                  <span className="text-xs font-medium text-slate-500">
                    {attachmentFiles.length}/{MAX_ATTACHMENTS} tệp
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      document.getElementById('farm-log-attachment-input')?.click()
                    }
                    disabled={isSubmitting || attachmentFiles.length >= MAX_ATTACHMENTS}
                    className="border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                  >
                    <Upload className="mr-2 size-4 text-slate-500" />
                    Chọn tệp đính kèm
                  </Button>
                  <span className="text-xs text-slate-400">
                    Hỗ trợ JPG, PNG, PDF (Tối đa 5MB)
                  </span>
                  <input
                    id="farm-log-attachment-input"
                    type="file"
                    accept="image/jpeg,image/png,application/pdf"
                    multiple
                    className="hidden"
                    onChange={handleFileChange}
                    disabled={isSubmitting}
                  />
                </div>

                {attachmentFiles.length > 0 && (
                  <div className="grid grid-cols-2 gap-3 pt-2 sm:grid-cols-4">
                    {attachmentFiles.map((file, idx) => (
                      <div
                        key={idx}
                        className="group relative flex items-center gap-2 rounded-lg border border-slate-200 bg-white p-2 text-xs shadow-2xs"
                      >
                        {file.type.startsWith('image/') && filePreviews[idx] ? (
                          <img
                            src={filePreviews[idx]}
                            alt={file.name}
                            className="size-8 rounded object-cover"
                          />
                        ) : (
                          <FileText className="size-8 shrink-0 text-slate-400" />
                        )}
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-medium text-slate-700">
                            {file.name}
                          </p>
                          <p className="text-[10px] text-slate-400">
                            {(file.size / 1024 / 1024).toFixed(2)} MB
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeFile(idx)}
                          className="rounded-full p-1 text-slate-400 hover:bg-red-50 hover:text-red-500"
                        >
                          <X className="size-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardContent>

            <CardFooter className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="outline"
                onClick={onCancel}
                disabled={isSubmitting}
                className="w-full border-slate-300 text-slate-700 hover:bg-slate-50 sm:w-auto"
              >
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                variant="create"
                disabled={isSubmitting}
                className="w-full sm:w-auto"
              >
                {isSubmitting ? 'Đang ghi nhận...' : 'Lưu nhật ký'}
              </Button>
            </CardFooter>
          </form>
        </Card>
      </div>

      <aside className="space-y-6 lg:col-span-4">
        <Card className="border-slate-200 shadow-sm">
          <CardHeader className="border-b border-slate-100 bg-slate-50/50 pb-4">
            <CardTitle className="text-base font-bold text-slate-800">
              Thông tin lô sản xuất
            </CardTitle>
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
                      Ngày trồng
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
              <li>Chứng từ có thể chọn đính kèm trực tiếp khi tạo nhật ký hoặc bổ sung sau.</li>
            </ul>
          </div>
        </div>
      </aside>
    </div>
  );
}