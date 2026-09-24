import { MapPin, Percent, Scale } from 'lucide-react';

import { LotValidationStatus } from '@/components/event-validation/LotValidationStatus';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import type { CreatePreprocessingController } from './useCreatePreprocessingForm';

import { getLocalDateString } from '@/utils/dateTime';
import { preventMouseUpCollapse, selectAllOnFocus } from '@/utils/inputUtils';

import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';

/** Các trường nghiệp vụ của biểu mẫu tạo sự kiện sơ chế. */
export function CreatePreprocessingFields({
  controller,
}: {
  controller: CreatePreprocessingController;
}) {
  const {
    currentPosition,
    formState: { errors, isSubmitting },
    handleLocationSelect,
    handleLotChange,
    lossRate,
    loadingLots,
    productionLots,
    register,
    selectedLot,
    selectedLotId,
    validation,
    validationError,
    validationLoading,
  } = controller;
  return (
    <>
      <section className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="productionLotId" className="font-semibold text-emerald-900">
            Chọn lô sản xuất (Đã thu hoạch) <span className="text-red-500">*</span>
          </Label>
          <Select
            value={selectedLotId}
            onValueChange={handleLotChange}
            disabled={loadingLots || isSubmitting}
          >
            <SelectTrigger id="productionLotId" className="w-full">
              <span>
                {selectedLot?.name ??
                  (loadingLots ? 'Đang tải danh sách lô...' : 'Chọn lô sản xuất')}
              </span>
            </SelectTrigger>
            <SelectContent>
              {productionLots.map((lot) => (
                <SelectItem key={lot.id} value={lot.id}>
                  {lot.name}
                  {lot.productCategoryName ? ` - ${lot.productCategoryName}` : ''}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {errors.productionLotId && (
            <p className="text-sm font-medium text-red-500">
              {errors.productionLotId.message}
            </p>
          )}
        </div>
        {selectedLotId && (
          <LotValidationStatus
            isValid={validation?.valid ?? null}
            message={validation?.message ?? validationError ?? ''}
            loading={validationLoading}
          />
        )}
      </section>
      <section className="space-y-4 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
        <h2 className="flex items-center gap-2 font-semibold text-emerald-800">
          <Scale className="size-4" /> Khối lượng & Phân loại
        </h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="inputQuantity">
              Khối lượng đưa vào sơ chế (kg) <span className="text-red-500">*</span>
            </Label>
            <Input
              id="inputQuantity"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="VD: 1000"
              disabled={isSubmitting}
              {...register('inputQuantity', { valueAsNumber: true })}
              onFocus={selectAllOnFocus}
              onMouseUp={preventMouseUpCollapse}
            />
            {errors.inputQuantity && (
              <p className="text-sm text-red-500">{errors.inputQuantity.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="outputQuantity">
              Khối lượng thu được sau sơ chế (kg){' '}
              <span className="text-red-500">*</span>
            </Label>
            <Input
              id="outputQuantity"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="VD: 950"
              disabled={isSubmitting}
              {...register('outputQuantity', { valueAsNumber: true })}
              onFocus={selectAllOnFocus}
              onMouseUp={preventMouseUpCollapse}
            />
            {errors.outputQuantity && (
              <p className="text-sm text-red-500">{errors.outputQuantity.message}</p>
            )}
          </div>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="rounded-lg border border-emerald-200 bg-white p-3 shadow-xs">
            <div className="flex items-center justify-between text-sm">
              <span className="flex items-center gap-1.5 text-muted-foreground">
                <Percent className="size-4 text-emerald-700" /> Tỷ lệ hao hụt dự kiến:
              </span>
              <span className="font-bold text-emerald-900">
                {lossRate !== null ? `${lossRate}%` : '--'}
              </span>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="grade">Phân loại phẩm cấp (Grade)</Label>
            <Input
              id="grade"
              placeholder="VD: Loại 1, Hạng A, Xuất khẩu..."
              disabled={isSubmitting}
              {...register('grade')}
            />
            {errors.grade && <p className="text-sm text-red-500">{errors.grade.message}</p>}
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="processingMethod">Phương pháp / Quy trình sơ chế</Label>
          <Textarea
            id="processingMethod"
            rows={3}
            placeholder={
              'VD: Rửa sạch, sấy lạnh ở 45 độ C trong 8 giờ, ' +
              'phân loại kích thước bằng sàng...'
            }
            disabled={isSubmitting}
            {...register('processingMethod')}
          />
          {errors.processingMethod && (
            <p className="text-sm text-red-500">{errors.processingMethod.message}</p>
          )}
        </div>
      </section>
      <section className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="preprocessingDate" className="font-semibold text-emerald-900">
            Ngày thực hiện sơ chế <span className="text-red-500">*</span>
          </Label>
          <Input
            id="preprocessingDate"
            type="date"
            max={getLocalDateString()}
            disabled={isSubmitting}
            {...register('preprocessingDate')}
          />
          {errors.preprocessingDate && (
            <p className="text-sm text-red-500">{errors.preprocessingDate.message}</p>
          )}
        </div>
        <div className="space-y-3 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
          <Label className="flex items-center gap-2 font-semibold text-emerald-800">
            <MapPin className="size-4" /> Vị trí sơ chế (Click trên bản đồ)
          </Label>
          <LocationPicker
            onLocationSelect={handleLocationSelect}
            initialPosition={currentPosition}
            height="280px"
          />
        </div>
      </section>
    </>
  );
}
