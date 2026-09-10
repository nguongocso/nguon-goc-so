import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, Loader2, PackagePlus, Plus, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

import {
  getPartnerOrganizations,
  getShipmentSplitPreview,
  splitShipment,
} from '@/api/shipmentSplitApi';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type {
  PartnerOrganization,
  ShipmentSplitAllocation,
  ShipmentSplitPreview,
} from '@/types/shipmentSplit';
import { validateShipmentSplit } from '@/utils/shipmentSplitValidation';

const EMPTY_ALLOCATION: ShipmentSplitAllocation = {
  recipientOrganizationId: '',
  name: '',
  quantity: 0,
  fromCode: '',
  toCode: '',
  packagingInfo: '',
};

function getErrorMessage(error: unknown, fallback: string): string {
  const apiError = error as { response?: { data?: { message?: string } }; message?: string };
  return apiError.response?.data?.message || apiError.message || fallback;
}

export default function SplitShipmentPage() {
  const { id: shipmentId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [preview, setPreview] = useState<ShipmentSplitPreview | null>(null);
  const [partners, setPartners] = useState<PartnerOrganization[]>([]);
  const [allocations, setAllocations] = useState<ShipmentSplitAllocation[]>([
    { ...EMPTY_ALLOCATION },
    { ...EMPTY_ALLOCATION },
  ]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useSetBreadcrumb(
    preview
      ? [
          { label: 'Lô sản xuất', href: '/production-lots' },
          { label: preview.shipmentName, href: `/shipments/${preview.shipmentId}` },
          { label: 'Tách lô hàng' },
        ]
      : null,
  );

  useEffect(() => {
    async function loadData() {
      if (!shipmentId) return;
      setLoading(true);
      setLoadError(null);
      try {
        const [previewData, partnerPage] = await Promise.all([
          getShipmentSplitPreview(shipmentId),
          getPartnerOrganizations(),
        ]);
        setPreview(previewData);
        setPartners(partnerPage.items);
      } catch (error) {
        setLoadError(getErrorMessage(error, 'Không thể tải dữ liệu tách lô hàng.'));
      } finally {
        setLoading(false);
      }
    }
    void loadData();
  }, [shipmentId]);

  const validation = useMemo(
    () => (preview ? validateShipmentSplit(preview, allocations) : null),
    [allocations, preview],
  );

  const updateAllocation = <K extends keyof ShipmentSplitAllocation>(
    index: number,
    field: K,
    value: ShipmentSplitAllocation[K],
  ) => {
    setAllocations((current) =>
      current.map((item, itemIndex) => (itemIndex === index ? { ...item, [field]: value } : item)),
    );
  };

  const removeAllocation = (index: number) => {
    if (allocations.length <= 2) return;
    setAllocations((current) => current.filter((_, itemIndex) => itemIndex !== index));
  };

  const handleSubmit = async () => {
    if (!shipmentId || !validation?.isValid) return;
    setSubmitting(true);
    try {
      const result = await splitShipment(shipmentId, {
        allocations: allocations.map((item) => ({
          ...item,
          name: item.name.trim(),
          fromCode: item.fromCode.trim(),
          toCode: item.toCode.trim(),
          packagingInfo: item.packagingInfo?.trim() || undefined,
        })),
      });
      toast.success(`Đã tách thành công ${result.childShipments.length} lô con.`);
      navigate(`/shipments/${shipmentId}`);
    } catch (error) {
      toast.error(getErrorMessage(error, 'Không thể tách lô hàng.'));
    } finally {
      setSubmitting(false);
      setConfirmOpen(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3 text-slate-500">
        <Loader2 className="size-8 animate-spin text-emerald-600" />
        <p>Đang kiểm tra điều kiện tách lô...</p>
      </div>
    );
  }

  if (loadError || !preview) {
    return (
      <Alert variant="destructive">
        <AlertCircle />
        <AlertTitle>Không thể mở màn hình tách lô</AlertTitle>
        <AlertDescription>{loadError || 'Không tìm thấy lô hàng.'}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6 pb-8">
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-emerald-100 p-2 text-emerald-700">
            <PackagePlus className="size-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Tách lô hàng</h1>
            <p className="text-sm text-slate-500">Phân toàn bộ tem chưa kích hoạt cho các đối tác nhận hàng.</p>
          </div>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Thông tin lô cha</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Summary label="Tên lô" value={preview.shipmentName} />
          <Summary label="Lô sản xuất" value={preview.productionLotName} />
          <Summary label="Số tem có thể phân" value={preview.assignableQuantity.toLocaleString('vi-VN')} />
          <Summary
            label="Dải mã hiện có"
            value={`${preview.availableCodeRange.fromCode} – ${preview.availableCodeRange.toCode}`}
          />
        </CardContent>
      </Card>

      {!preview.canSplit && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Lô hàng chưa đủ điều kiện tách</AlertTitle>
          <AlertDescription>{preview.blockMessage || 'Vui lòng kiểm tra trạng thái lô và mã tem.'}</AlertDescription>
        </Alert>
      )}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-4">
          <div>
            <CardTitle className="text-lg">Phương án phân bổ</CardTitle>
            <p className="mt-1 text-sm text-slate-500">Mỗi đối tác nhận một lô con và một khoảng mã liên tục.</p>
          </div>
          <Button
            type="button"
            variant="outline"
            onClick={() => setAllocations((current) => [...current, { ...EMPTY_ALLOCATION }])}
            disabled={!preview.canSplit}
          >
            <Plus className="mr-2 size-4" /> Thêm lô con
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {allocations.map((allocation, index) => (
            <div key={index} className="rounded-xl border border-slate-200 bg-slate-50/50 p-4">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="font-semibold text-slate-900">Lô con {index + 1}</h2>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label={`Xóa lô con ${index + 1}`}
                  disabled={allocations.length <= 2 || !preview.canSplit}
                  onClick={() => removeAllocation(index)}
                >
                  <Trash2 className="size-4 text-red-600" />
                </Button>
              </div>
              <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
                <Field label="Đối tác nhận" required>
                  <select
                    className="h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
                    value={allocation.recipientOrganizationId}
                    disabled={!preview.canSplit}
                    onChange={(event) => updateAllocation(index, 'recipientOrganizationId', event.target.value)}
                  >
                    <option value="">Chọn đối tác nhận hàng</option>
                    {partners.map((partner) => (
                      <option key={partner.id} value={partner.id}>{partner.name} ({partner.code})</option>
                    ))}
                  </select>
                </Field>
                <Field label="Tên lô con" required>
                  <Input
                    value={allocation.name}
                    maxLength={255}
                    disabled={!preview.canSplit}
                    placeholder="Ví dụ: Lô giao An Phú"
                    onChange={(event) => updateAllocation(index, 'name', event.target.value)}
                  />
                </Field>
                <Field label="Số lượng tem" required>
                  <Input
                    type="number"
                    min={1}
                    step={1}
                    value={allocation.quantity || ''}
                    disabled={!preview.canSplit}
                    onChange={(event) => updateAllocation(index, 'quantity', Number(event.target.value))}
                  />
                </Field>
                <Field label="Mã bắt đầu" required>
                  <Input
                    value={allocation.fromCode}
                    disabled={!preview.canSplit}
                    placeholder={preview.availableCodeRange.fromCode}
                    onChange={(event) => updateAllocation(index, 'fromCode', event.target.value)}
                  />
                </Field>
                <Field label="Mã kết thúc" required>
                  <Input
                    value={allocation.toCode}
                    disabled={!preview.canSplit}
                    placeholder={preview.availableCodeRange.toCode}
                    onChange={(event) => updateAllocation(index, 'toCode', event.target.value)}
                  />
                </Field>
                <Field label="Quy cách đóng gói">
                  <Textarea
                    value={allocation.packagingInfo}
                    maxLength={500}
                    disabled={!preview.canSplit}
                    placeholder="Không bắt buộc"
                    onChange={(event) => updateAllocation(index, 'packagingInfo', event.target.value)}
                  />
                </Field>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card className="sticky bottom-4 border-emerald-200 shadow-lg">
        <CardContent className="flex flex-col gap-4 pt-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="font-semibold text-slate-900">
              Đã phân bổ {validation?.allocatedQuantity.toLocaleString('vi-VN')} / {preview.assignableQuantity.toLocaleString('vi-VN')} tem
            </p>
            {validation?.errors.length ? (
              <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-red-600">
                {validation.errors.map((error) => <li key={error}>{error}</li>)}
              </ul>
            ) : (
              <p className="mt-1 text-sm text-emerald-700">Phương án phân bổ hợp lệ và phủ toàn bộ dải mã.</p>
            )}
          </div>
          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" onClick={() => navigate(`/shipments/${shipmentId}`)}>Hủy</Button>
            <Button
              type="button"
              disabled={!preview.canSplit || !validation?.isValid || submitting}
              onClick={() => setConfirmOpen(true)}
            >
              Xác nhận tách lô
            </Button>
          </div>
        </CardContent>
      </Card>

      <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xác nhận tách lô hàng?</AlertDialogTitle>
            <AlertDialogDescription>
              Lô cha sẽ chuyển sang trạng thái Đã tách và không tiếp tục nhận sự kiện mới. Thao tác này không thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={submitting}>Kiểm tra lại</AlertDialogCancel>
            <AlertDialogAction disabled={submitting} onClick={() => void handleSubmit()}>
              {submitting && <Loader2 className="mr-2 size-4 animate-spin" />}
              Tách {allocations.length} lô con
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

function Summary({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 break-words font-semibold text-slate-900">{value}</p>
    </div>
  );
}

function Field({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <div className="space-y-2">
      <Label>{label}{required && <span className="text-red-600"> *</span>}</Label>
      {children}
    </div>
  );
}
