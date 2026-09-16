import React, { useState } from 'react';
import { Controller, useForm, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Loader2, Save } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';

import { createFarmLog } from '@/api/farmLogApi';
import { ChainEventType } from '@/enums/chainEventType';
import { themNhatKyCho, type NhatKyChoMoi } from '@/lib/offline/farmLogDb';
import { HOAT_DONG_CANH_TAC_OPTIONS } from '@/utils/farmLogActivity';
import { getLocalDateString } from '@/utils/dateTime';
import {
  farmLogOfflineSchema,
  type FarmLogOfflineFormValues,
} from '@/utils/validators';

interface RecordFarmLogFormProps {
  /** Lô chọn được: online lấy từ API, offline lấy từ cache IndexedDB. */
  danhSachLo: Array<{ id: string; ten: string }>;
  isOnline: boolean;
  onSuccess?: () => void;
}

const giaTriMacDinh = (): FarmLogOfflineFormValues => ({
  productionLotId: '',
  activityType: 'WATERING',
  material: '',
  quantity: undefined,
  unit: '',
  executedDate: getLocalDateString(),
  notes: '',
});

const laLoiMang = (error: unknown): boolean => {
  if (typeof error !== 'object' || error === null) return false;
  const loi = error as { code?: string; message?: string };
  return loi.code === 'ERR_NETWORK' || (loi.message?.includes('Network') ?? false);
};

/**
 * Form ghi nhật ký canh tác trên mobile (NCL-10-CN-012, MVP).
 *
 * - Online → gọi `POST /farm-logs` trực tiếp; lỗi mạng thì lưu tạm.
 * - Offline → lưu vào IndexedDB, đồng bộ sau qua `POST /chain-events/sync`.
 * - MVP chưa nhập ảnh khi ngoại tuyến.
 */
export const RecordFarmLogForm: React.FC<RecordFarmLogFormProps> = ({
  danhSachLo,
  isOnline,
  onSuccess,
}) => {
  const [dangGui, setDangGui] = useState(false);

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FarmLogOfflineFormValues>({
    // preprocess của quantity làm kiểu input khác output nên ép kiểu tường minh
    resolver: zodResolver(
      farmLogOfflineSchema,
    ) as unknown as Resolver<FarmLogOfflineFormValues>,
    defaultValues: giaTriMacDinh(),
  });

  const homNay = getLocalDateString();

  const xayDungBanGhiCho = (data: FarmLogOfflineFormValues): NhatKyChoMoi => ({
    productionLotId: data.productionLotId,
    eventType: ChainEventType.FARM_LOG,
    recordedAt: new Date().toISOString(),
    latitude: 0,
    longitude: 0,
    images: [],
    deviceSource: 'MOBILE',
    eventData: {
      activityType: data.activityType,
      ...(data.material ? { material: data.material } : {}),
      ...(data.quantity !== undefined ? { quantity: data.quantity } : {}),
      ...(data.unit ? { unit: data.unit } : {}),
      executedDate: data.executedDate,
      ...(data.notes ? { notes: data.notes } : {}),
    },
  });

  const luuTam = async (data: FarmLogOfflineFormValues): Promise<void> => {
    try {
      await themNhatKyCho(xayDungBanGhiCho(data));
      toast.info('Nhật ký đã được lưu tạm và sẽ đồng bộ khi có mạng.');
      reset(giaTriMacDinh());
      onSuccess?.();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Không thể lưu tạm.');
    }
  };

  const onSubmit = async (
    data: FarmLogOfflineFormValues,
    buocLuuTam = false,
  ): Promise<void> => {
    if (!isOnline || buocLuuTam) {
      await luuTam(data);
      return;
    }

    setDangGui(true);
    try {
      await createFarmLog({
        productionLotId: data.productionLotId,
        activityType: data.activityType,
        material: data.material || null,
        quantity: data.quantity ?? null,
        unit: data.unit || null,
        executedDate: data.executedDate,
        notes: data.notes || null,
      });
      toast.success('Ghi nhật ký thành công!');
      reset(giaTriMacDinh());
      onSuccess?.();
    } catch (error: unknown) {
      if (laLoiMang(error)) {
        await luuTam(data);
        return;
      }
      const thongBao =
        (error as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || 'Ghi nhật ký thất bại';
      toast.error(thongBao);
    } finally {
      setDangGui(false);
    }
  };

  return (
    <Card className="mx-auto max-w-md">
      <CardHeader>
        <CardTitle>Ghi nhật ký canh tác</CardTitle>
        <CardDescription>
          {isOnline
            ? 'Nhập thông tin hoạt động canh tác tại ruộng'
            : 'Đang ngoại tuyến — bản ghi sẽ được lưu tạm trên thiết bị'}
        </CardDescription>
      </CardHeader>

      <CardContent>
        <form onSubmit={handleSubmit((d) => onSubmit(d))} className="space-y-4">
          <div className="space-y-2">
            <Label>Lô sản xuất *</Label>
            <Controller
              name="productionLotId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={dangGui}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn lô sản xuất" />
                  </SelectTrigger>
                  <SelectContent>
                    {danhSachLo.map((lo) => (
                      <SelectItem key={lo.id} value={lo.id}>
                        {lo.ten}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.productionLotId && (
              <p className="text-sm text-red-500">{errors.productionLotId.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Loại hoạt động *</Label>
            <Controller
              name="activityType"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={dangGui}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn hoạt động" />
                  </SelectTrigger>
                  <SelectContent>
                    {HOAT_DONG_CANH_TAC_OPTIONS.map((o) => (
                      <SelectItem key={o.value} value={o.value}>
                        {o.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.activityType && (
              <p className="text-sm text-red-500">{errors.activityType.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Vật tư sử dụng</Label>
            <Input
              placeholder="Ví dụ: NPK 16-16-8"
              disabled={dangGui}
              {...register('material')}
            />
            {errors.material && (
              <p className="text-sm text-red-500">{errors.material.message}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label>Số lượng</Label>
              <Input
                type="number"
                inputMode="decimal"
                min="0"
                step="any"
                placeholder="25"
                disabled={dangGui}
                {...register('quantity')}
              />
              {errors.quantity && (
                <p className="text-sm text-red-500">{errors.quantity.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label>Đơn vị</Label>
              <Input placeholder="kg" disabled={dangGui} {...register('unit')} />
              {errors.unit && (
                <p className="text-sm text-red-500">{errors.unit.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label>Ngày thực hiện *</Label>
            <Input
              type="date"
              max={homNay}
              disabled={dangGui}
              {...register('executedDate')}
            />
            {errors.executedDate && (
              <p className="text-sm text-red-500">{errors.executedDate.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Ghi chú</Label>
            <Textarea
              placeholder="Ghi chú thêm về hoạt động..."
              disabled={dangGui}
              {...register('notes')}
            />
            {errors.notes && (
              <p className="text-sm text-red-500">{errors.notes.message}</p>
            )}
          </div>

          <div className="flex gap-2">
            <Button type="submit" className="flex-1" disabled={dangGui}>
              {dangGui && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {isOnline ? 'Gửi ngay' : 'Lưu tạm'}
            </Button>
            {isOnline && (
              <Button
                type="button"
                variant="outline"
                disabled={dangGui}
                onClick={handleSubmit((d) => onSubmit(d, true))}
              >
                <Save className="mr-2 h-4 w-4" />
                Lưu tạm
              </Button>
            )}
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
