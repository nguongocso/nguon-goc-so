import type { FieldErrors, UseFormRegister } from 'react-hook-form';
import { AlertCircle, Building2, Calendar, FileText, MapPin, Thermometer } from 'lucide-react';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { CoopWarehouseShipmentSection } from './CoopWarehouseShipmentSection';

import type { RecordWarehouseEntryFormValues } from '@/utils/validators/coopWarehouseEventSchema';

import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';
import type { ShipmentStatusItem } from './coopWarehouseUtils';

interface CoopWarehouseEntryFieldsProps {
  register: UseFormRegister<RecordWarehouseEntryFormValues>;
  errors: FieldErrors<RecordWarehouseEntryFormValues>;
  selectedItems: ShipmentStatusItem[];
  invalidItems: ShipmentStatusItem[];
  loadingShipments: boolean;
  serverError: string | null;
  currentPosition?: { lat: number; lng: number };
  onLocationSelect: (latitude: number, longitude: number) => void;
}

/** Các trường nhập liệu của sự kiện nhập kho hợp tác xã. */
export function CoopWarehouseEntryFields({
  register,
  errors,
  selectedItems,
  invalidItems,
  loadingShipments,
  serverError,
  currentPosition,
  onLocationSelect,
}: CoopWarehouseEntryFieldsProps) {
  return (
    <>
      {serverError && (
        <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
          <div>{serverError}</div>
        </div>
      )}
      <CoopWarehouseShipmentSection
        mode="entry"
        items={selectedItems}
        invalidItems={invalidItems}
        loading={loadingShipments}
      />
      <div className="space-y-4 pt-2">
        <div className="border-b border-slate-100 pb-1">
          <Label className="flex items-center gap-2 text-sm font-semibold text-slate-900">
            <Building2 className="h-4 w-4 text-emerald-600" />
            2. Thông tin kho lưu trữ & Thời điểm nhập
          </Label>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label
              htmlFor="warehouseName"
              className="flex items-center gap-1.5 font-medium text-slate-700"
            >
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
            <Label
              htmlFor="entryTime"
              className="flex items-center gap-1.5 font-medium text-slate-700"
            >
              <Calendar className="h-4 w-4 text-slate-400" />
              Thời điểm nhập kho <span className="text-red-500">*</span>
            </Label>
            <Input id="entryTime" type="datetime-local" {...register('entryTime')} />
            {errors.entryTime && <p className="text-sm text-red-600">{errors.entryTime.message}</p>}
          </div>
        </div>
        <div className="space-y-2">
          <Label
            htmlFor="storageCondition"
            className="flex items-center gap-1.5 font-medium text-slate-700"
          >
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
      <div className="space-y-2 pt-2">
        <Label className="flex items-center gap-2 border-b border-slate-100 pb-1 text-sm font-semibold">
          <MapPin className="h-4 w-4 text-emerald-600" />
          3. Vị trí kho (Click chọn trên bản đồ)
        </Label>
        <div className="overflow-hidden rounded-lg border border-slate-200">
          <LocationPicker
            onLocationSelect={onLocationSelect}
            initialPosition={currentPosition}
            height="260px"
          />
        </div>
      </div>
      <div className="space-y-2 pt-2">
        <Label
          htmlFor="notes"
          className="flex items-center gap-2 border-b border-slate-100 pb-1 text-sm font-semibold"
        >
          <FileText className="h-4 w-4 text-emerald-600" />
          4. Ghi chú bổ sung
        </Label>
        <Textarea
          id="notes"
          rows={3}
          placeholder="Nhập ghi chú bổ sung khi nhập kho..."
          {...register('notes')}
        />
        {errors.notes && <p className="text-sm text-red-600">{errors.notes.message}</p>}
      </div>
      <div className="space-y-1 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
        <p className="font-semibold">Lưu ý nghiệp vụ:</p>
        <p>
          • Thời gian lưu kho sẽ bắt đầu tính từ thời điểm nhập kho được ghi nhận ở đây.
        </p>
        <p>
          • Sự kiện sau khi tạo sẽ được liên kết trực tiếp vào chuỗi hash mã hóa của lô hàng.
        </p>
      </div>
    </>
  );
}
