import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import {
  LogIn,
  AlertCircle,
  CheckCircle2,
  Package,
  Building2,
  Calendar,
  MapPin,
  FileText,
  Thermometer,
} from 'lucide-react';

import {
  getShipmentWarehouseStatus,
  recordWarehouseEntry,
} from '@/api/coopWarehouseApi';
import { getProductionLotById } from '@/api/productionLotApi';
import { getShipmentById, getShipmentsByProductionLot } from '@/api/shipmentApi';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardFooter,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';
import { useAutoGeolocation } from '@/hooks/useAutoGeolocation';
import type { Shipment } from '@/types/shipment';
import {
  recordWarehouseEntrySchema,
  type RecordWarehouseEntryFormValues,
} from '@/utils/validators/coopWarehouseEventSchema';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { HelpButton } from '@/components/help/HelpButton';

import {
  formatDateTimeWithSeconds,
  getCoopWarehouseErrorMessage,
  getCurrentDatetimeString,
  type ShipmentStatusItem,
} from './coopWarehouseUtils';

/**
 * Trang ghi nhận sự kiện nhập kho hợp tác xã cho các lô hàng
 */
export default function CreateCoopWarehouseEntryPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const productionLotId = searchParams.get('productionLotId') || '';
  const queryParam = searchParams.get('shipmentIds') || searchParams.get('shipmentId') || '';
  const initialShipmentIds = queryParam ? queryParam.split(',').filter(Boolean) : [];

  const [allAvailableShipments, setAllAvailableShipments] = useState<ShipmentStatusItem[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>(initialShipmentIds);
  const [loadingShipments, setLoadingShipments] = useState(true);
  const [serverError, setServerError] = useState<string | null>(null);
  const [lotName, setLotName] = useState<string | null>(null);

  useEffect(() => {
    if (!productionLotId) return;
    getProductionLotById(productionLotId)
      .then((lot) => setLotName(lot.name || lot.code || null))
      .catch(() => setLotName(null));
  }, [productionLotId]);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Lô sản xuất', href: '/production-lots' },
    ...(productionLotId
      ? [
        {
          label: lotName || 'Chi tiết lô sản xuất',
          href: `/production-lots/${productionLotId}`,
        },
      ]
      : []),
    { label: 'Ghi sự kiện nhập kho HTX' },
  ]);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RecordWarehouseEntryFormValues>({
    resolver: zodResolver(recordWarehouseEntrySchema),
    defaultValues: {
      shipmentId: initialShipmentIds[0] || '',
      entryTime: getCurrentDatetimeString(),
      warehouseName: '',
      storageCondition: '',
      notes: '',
      latitude: undefined,
      longitude: undefined,
    },
  });

  const latitude = watch('latitude');
  const longitude = watch('longitude');

  const currentPosition =
    typeof latitude === 'number' &&
      Number.isFinite(latitude) &&
      typeof longitude === 'number' &&
      Number.isFinite(longitude)
      ? { lat: latitude, lng: longitude }
      : undefined;

  const handleLocationSelect = useCallback(
    (nextLat: number, nextLng: number) => {
      setValue('latitude', nextLat, {
        shouldDirty: true,
        shouldValidate: true,
      });
      setValue('longitude', nextLng, {
        shouldDirty: true,
        shouldValidate: true,
      });
    },
    [setValue],
  );

  useAutoGeolocation({
    onLocation: (nextLat, nextLng) => {
      handleLocationSelect(nextLat, nextLng);
      toast.success('Đã lấy vị trí hiện tại');
    },
    onError: (message) => {
      toast.error(`Không thể lấy vị trí hiện tại: ${message}`);
    },
  });

  // Tải tất cả lô hàng khả dụng & kiểm tra trạng thái ngay lập tức
  useEffect(() => {
    async function loadShipmentsAndStatuses() {
      try {
        setLoadingShipments(true);
        setServerError(null);

        let list: Shipment[] = [];
        if (productionLotId) {
          list = await getShipmentsByProductionLot(productionLotId);
        } else if (initialShipmentIds.length > 0) {
          const loaded = await Promise.all(
            initialShipmentIds.map((id) => getShipmentById(id).catch(() => null)),
          );
          list = loaded.filter((s): s is Shipment => s !== null);
        }

        // Tải trạng thái kho từng lô hàng
        const itemsWithStatus = await Promise.all(
          list.map(async (s) => {
            const status = await getShipmentWarehouseStatus(s.id);
            return { shipment: s, warehouseStatus: status };
          }),
        );

        setAllAvailableShipments(itemsWithStatus);

        // Mặc định chọn tất cả nếu từ URL truyền sang, hoặc chọn lô đầu tiên nếu chưa chọn
        if (initialShipmentIds.length > 0) {
          setSelectedIds(initialShipmentIds);
          setValue('shipmentId', initialShipmentIds[0]);
        } else if (itemsWithStatus.length > 0) {
          setSelectedIds([itemsWithStatus[0].shipment.id]);
          setValue('shipmentId', itemsWithStatus[0].shipment.id);
        }
      } catch {
        setServerError('Không thể tải thông tin hoặc kiểm tra trạng thái lô hàng.');
      } finally {
        setLoadingShipments(false);
      }
    }
    void loadShipmentsAndStatuses();
  }, [productionLotId, queryParam, setValue]);

  const selectedItems = allAvailableShipments.filter((item) =>
    selectedIds.includes(item.shipment.id),
  );

  // KIỂM TRA LỖI VI PHẠM TC-04 (Lô đã ở trong kho)
  const invalidItems = selectedItems.filter(
    (item) => item.warehouseStatus === 'IN_WAREHOUSE',
  );
  const hasValidationError = invalidItems.length > 0;

  const onSubmit = async (values: RecordWarehouseEntryFormValues) => {
    if (selectedItems.length === 0) {
      toast.error('Vui lòng chọn ít nhất một lô hàng để ghi sự kiện.');
      return;
    }

    if (hasValidationError) {
      toast.error('Vui lòng bỏ chọn các lô hàng đang ở trong kho trước khi nhập kho mới.');
      return;
    }

    try {
      setServerError(null);
      let successCount = 0;
      let failMessage = '';

      for (const item of selectedItems) {
        const formattedEntryTime = formatDateTimeWithSeconds(values.entryTime);
        const payload = {
          ...values,
          entryTime: formattedEntryTime,
          shipmentId: item.shipment.id,
        };
        const res = await recordWarehouseEntry(payload);
        if (res && res.success) {
          successCount++;
        } else {
          failMessage = res?.message || `Lỗi khi ghi nhập kho cho lô ${item.shipment.name}`;
          break;
        }
      }

      if (successCount === selectedItems.length) {
        toast.success(`Ghi sự kiện nhập kho HTX thành công cho ${successCount} lô hàng!`);
        navigate(-1);
      } else {
        setServerError(failMessage || 'Không thể ghi nhận nhập kho cho tất cả lô hàng.');
        toast.error(failMessage || 'Có lỗi xảy ra khi ghi sự kiện nhập kho HTX.');
      }
    } catch (err: unknown) {
      const msg = getCoopWarehouseErrorMessage(
        err,
        'Có lỗi xảy ra khi ghi sự kiện nhập kho HTX.',
      );
      setServerError(msg);
      toast.error(msg);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header chuẩn dự án */}
      <ListPageHeader
        icon={LogIn}
        title="Ghi sự kiện nhập kho HTX"
        description="Ghi nhận thời điểm các lô hàng rời xưởng đóng gói và nhập vào kho lưu trữ hợp tác xã."
        actions={<HelpButton screenKey="coop-warehouse-entry" />}
      />

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-6 pt-6">
            {serverError && (
              <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
                <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
                <div>{serverError}</div>
              </div>
            )}

            {/* MỤC 1: DANH SÁCH LÔ HÀNG ĐÃ CHỌN (ĐÃ CHỌN BÊN NGOÀI) */}
            <div className="space-y-3">
              <div className="flex items-center justify-between border-b border-slate-100 pb-1">
                <Label className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <Package className="h-4 w-4 text-emerald-600" />
                  1. Danh sách lô hàng thực hiện nhập kho ({selectedItems.length} lô)
                </Label>
              </div>

              {loadingShipments ? (
                <div className="animate-pulse rounded-lg border bg-slate-50 p-4 text-sm text-slate-600">
                  Đang tải thông tin các lô hàng...
                </div>
              ) : selectedItems.length === 0 ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                  Chưa có lô hàng nào được chọn. Vui lòng quay lại danh sách để chọn lô hàng.
                </div>
              ) : (
                <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
                  {selectedItems.map(({ shipment, warehouseStatus }) => {
                    const isInWarehouse = warehouseStatus === 'IN_WAREHOUSE';

                    return (
                      <div
                        key={shipment.id}
                        className={`flex items-center justify-between rounded-lg border p-3.5 text-sm ${isInWarehouse
                          ? 'border-red-200 bg-red-50/70 text-red-950'
                          : 'border-emerald-200 bg-emerald-50/50 text-emerald-950'
                          }`}
                      >
                        <div className="space-y-0.5">
                          <p className="font-semibold text-slate-900">
                            {shipment.name}
                          </p>
                          <p className="text-xs text-slate-600">
                            Số lượng: <span className="font-medium">{shipment.totalQuantity}</span> | Quy cách:{' '}
                            <span className="font-medium">{shipment.packagingInfo || '—'}</span>
                          </p>
                        </div>

                        <div>
                          {isInWarehouse ? (
                            <span className="inline-flex items-center gap-1 rounded-full border border-red-300 bg-red-100 px-2.5 py-1 text-xs font-semibold text-red-800">
                              <AlertCircle className="h-3.5 w-3.5" />
                              Đang ở trong kho
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 rounded-full border border-emerald-300 bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800">
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              Sẵn sàng nhập kho
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* BÁO LỖI TỨC THÌ KHI CHỌN VÀO LÔ KHÔNG HỢP LỆ (TC-04 Instant Validation Alert) */}
              {hasValidationError && (
                <div className="space-y-1.5 rounded-lg border-2 border-red-400 bg-red-100/90 p-4 text-sm text-red-950 shadow-sm">
                  <p className="flex items-center gap-2 text-base font-bold text-red-900">
                    <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
                    Cảnh báo vi phạm ràng buộc nghiệp vụ:
                  </p>
                  {invalidItems.map(({ shipment }) => (
                    <p key={shipment.id} className="pl-7 font-medium text-red-900">
                      • Lô hàng <span className="font-bold text-red-950">"{shipment.name}"</span> hiện đang ở trong kho HTX, vui lòng quay lại ghi xuất kho trước khi nhập mới.
                    </p>
                  ))}
                </div>
              )}
            </div>

            {/* MỤC 2: THÔNG TIN KHO & THỜI ĐIỂM */}
            <div className="space-y-4 pt-2">
              <div className="border-b border-slate-100 pb-1">
                <Label className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <Building2 className="h-4 w-4 text-emerald-600" />
                  2. Thông tin kho lưu trữ & Thời điểm nhập
                </Label>
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="warehouseName" className="flex items-center gap-1.5 font-medium text-slate-700">
                    <Building2 className="h-4 w-4 text-slate-400" />
                    Tên kho lưu trữ <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="warehouseName"
                    placeholder="VD: Kho lạnh HTX Nông nghiệp Số 1"
                    {...register('warehouseName')}
                  />
                  {errors.warehouseName && (
                    <p className="text-sm text-red-600">{errors.warehouseName.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="entryTime" className="flex items-center gap-1.5 font-medium text-slate-700">
                    <Calendar className="h-4 w-4 text-slate-400" />
                    Thời điểm nhập kho <span className="text-red-500">*</span>
                  </Label>
                  <Input id="entryTime" type="datetime-local" {...register('entryTime')} />
                  {errors.entryTime && (
                    <p className="text-sm text-red-600">{errors.entryTime.message}</p>
                  )}
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="storageCondition" className="flex items-center gap-1.5 font-medium text-slate-700">
                  <Thermometer className="h-4 w-4 text-slate-400" />
                  Điều kiện bảo quản
                </Label>
                <Input
                  id="storageCondition"
                  placeholder="VD: Nhiệt độ 4°C - 8°C, Độ ẩm 85%"
                  {...register('storageCondition')}
                />
                {errors.storageCondition && (
                  <p className="text-sm text-red-600">{errors.storageCondition.message}</p>
                )}
              </div>
            </div>

            {/* MỤC 3: VỊ TRÍ BẢN ĐỒ */}
            <div className="space-y-2 pt-2">
              <div className="border-b border-slate-100 pb-1">
                <Label className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <MapPin className="h-4 w-4 text-emerald-600" />
                  3. Vị trí kho (Click chọn trên bản đồ)
                </Label>
              </div>
              <div className="overflow-hidden rounded-lg border border-slate-200">
                <LocationPicker
                  onLocationSelect={handleLocationSelect}
                  initialPosition={currentPosition}
                  height="260px"
                />
              </div>
            </div>

            {/* MỤC 4: GHI CHÚ BỔ SUNG */}
            <div className="space-y-2 pt-2">
              <div className="border-b border-slate-100 pb-1">
                <Label htmlFor="notes" className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                  <FileText className="h-4 w-4 text-emerald-600" />
                  4. Ghi chú bổ sung
                </Label>
              </div>
              <Textarea
                id="notes"
                rows={3}
                placeholder="Nhập ghi chú bổ sung khi nhập kho..."
                {...register('notes')}
              />
              {errors.notes && <p className="text-sm text-red-600">{errors.notes.message}</p>}
            </div>

            {/* Hộp lưu ý */}
            <div className="space-y-1 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              <p className="font-semibold">Lưu ý nghiệp vụ:</p>
              <p>• Thời gian lưu kho sẽ bắt đầu tính từ thời điểm nhập kho được ghi nhận ở đây.</p>
              <p>• Sự kiện sau khi tạo sẽ được liên kết trực tiếp vào chuỗi hash mã hóa của lô hàng.</p>
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-3 rounded-b-xl border-t border-slate-100 bg-slate-50/50 p-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || selectedItems.length === 0 || hasValidationError}
              className="bg-emerald-600 text-white hover:bg-emerald-700"
            >
              <LogIn className="mr-1.5 h-4 w-4" />
              {isSubmitting ? 'Đang xử lý...' : `Ghi nhập kho (${selectedItems.length} lô)`}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
