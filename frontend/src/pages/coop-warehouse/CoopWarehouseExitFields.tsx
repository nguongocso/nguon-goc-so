import type { FieldErrors, UseFormRegister } from 'react-hook-form';
import { AlertCircle, Building2, Calendar, FileText, MapPin, Truck } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';
import type { RecordWarehouseExitFormValues } from '@/utils/validators/coopWarehouseEventSchema';
import type { ShipmentStatusItem } from './coopWarehouseUtils';
import { CoopWarehouseShipmentSection } from './CoopWarehouseShipmentSection';

interface CoopWarehouseExitFieldsProps {
  register: UseFormRegister<RecordWarehouseExitFormValues>;
  errors: FieldErrors<RecordWarehouseExitFormValues>;
  selectedItems: ShipmentStatusItem[];
  invalidItems: ShipmentStatusItem[];
  loadingShipments: boolean;
  serverError: string | null;
  currentPosition?: { lat: number; lng: number };
  onLocationSelect: (latitude: number, longitude: number) => void;
}

/** Các trường nhập liệu của sự kiện xuất kho hợp tác xã. */
export function CoopWarehouseExitFields({
  register,
  errors,
  selectedItems,
  invalidItems,
  loadingShipments,
  serverError,
  currentPosition,
  onLocationSelect,
}: CoopWarehouseExitFieldsProps) {
  return (
    <>
      {serverError && (
        <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
          <div>{serverError}</div>
        </div>
      )}
      <CoopWarehouseShipmentSection
        mode="exit"
        items={selectedItems}
        invalidItems={invalidItems}
        loading={loadingShipments}
      />
      <div className="space-y-4 pt-2">
        <Label className="flex items-center gap-2 border-b pb-1 text-sm font-semibold">
          <Building2 className="h-4 w-4 text-amber-600" />
          2. Thời điểm xuất kho & Nơi chuyển đến
        </Label>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="exitTime" className="flex items-center gap-1.5 text-slate-700">
              <Calendar className="h-4 w-4 text-slate-400" />
              Thời điểm xuất kho <span className="text-red-500">*</span>
            </Label>
            <Input id="exitTime" type="datetime-local" {...register('exitTime')} />
            {errors.exitTime && <p className="text-sm text-red-600">{errors.exitTime.message}</p>}
          </div>
          <div className="space-y-2">
            <Label htmlFor="destination" className="flex items-center gap-1.5 text-slate-700">
              <Truck className="h-4 w-4 text-slate-400" />
              Nơi chuyển đến / Đơn vị tiếp nhận
            </Label>
            <Input
              id="destination"
              placeholder="VD: Xe vận chuyển Công ty Thu Mua Chè Việt"
              {...register('destination')}
            />
            {errors.destination && (
              <p className="text-sm text-red-600">{errors.destination.message}</p>
            )}
          </div>
        </div>
      </div>
      <div className="space-y-2 pt-2">
        <Label className="flex items-center gap-2 border-b pb-1 text-sm font-semibold">
          <MapPin className="h-4 w-4 text-amber-600" />
          3. Vị trí xuất kho (Click chọn trên bản đồ)
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
        <Label htmlFor="notes" className="flex items-center gap-2 border-b pb-1 text-sm font-semibold">
          <FileText className="h-4 w-4 text-amber-600" />
          4. Ghi chú bổ sung
        </Label>
        <Textarea
          id="notes"
          rows={3}
          placeholder="Nhập ghi chú bổ sung khi xuất kho..."
          {...register('notes')}
        />
        {errors.notes && <p className="text-sm text-red-600">{errors.notes.message}</p>}
      </div>
      <div className="space-y-1 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
        <p className="font-semibold">Lưu ý nghiệp vụ:</p>
        <p>
          • Hệ thống tính thời gian lưu kho từ lần nhập gần nhất đến thời điểm xuất kho này.
        </p>
        <p>
          • Hệ thống cảnh báo nếu thời gian lưu kho vượt mức bảo quản tối đa của nông sản.
        </p>
      </div>
    </>
  );
}
