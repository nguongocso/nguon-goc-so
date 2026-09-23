import { MapPin, Percent, Scale } from 'lucide-react';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type { CorrectPreprocessingController } from './useCorrectPreprocessingForm';

import { getLocalDateString } from '@/utils/dateTime';

import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';

/** Các trường nghiệp vụ của biểu mẫu đính chính sơ chế. */
export function CorrectPreprocessingFields({
  controller,
}: {
  controller: CorrectPreprocessingController;
}) {
  const {
    currentPosition,
    formState: { errors },
    handleLocationSelect,
    lossRate,
    register,
    watch,
  } = controller;
  return (
    <>
      <section className="space-y-4" aria-labelledby="correction-quantity-heading">
        <div>
          <h2
            id="correction-quantity-heading"
            className="flex items-center gap-2 font-semibold text-amber-800"
          >
            <Scale className="size-4" /> Khối lượng đính chính
          </h2>
          <p className="text-sm text-muted-foreground">
            Nhập lại giá trị chính xác của lần sơ chế cần đính chính.
          </p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="correctionInputQuantity">
              Khối lượng đưa vào (kg) <span className="text-red-500">*</span>
            </Label>
            <Input
              id="correctionInputQuantity"
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              aria-invalid={Boolean(errors.inputQuantity)}
              {...register('inputQuantity', { valueAsNumber: true })}
            />
            {errors.inputQuantity && (
              <p className="text-sm text-red-500" role="alert">
                {errors.inputQuantity.message}
              </p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="correctionOutputQuantity">
              Khối lượng sau sơ chế (kg) <span className="text-red-500">*</span>
            </Label>
            <Input
              id="correctionOutputQuantity"
              type="number"
              min="0"
              step="0.01"
              inputMode="decimal"
              aria-invalid={Boolean(errors.outputQuantity)}
              {...register('outputQuantity', { valueAsNumber: true })}
            />
            {errors.outputQuantity && (
              <p className="text-sm text-red-500" role="alert">
                {errors.outputQuantity.message}
              </p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3 rounded-xl border border-amber-100 bg-amber-50 p-4">
          <Percent className="size-5 text-amber-700" />
          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              Tỷ lệ hao hụt sau đính chính
            </p>
            <p className="text-xl font-semibold text-amber-800" aria-live="polite">
              {lossRate === null ? '—' : `${lossRate.toLocaleString('vi-VN')}%`}
            </p>
          </div>
        </div>
      </section>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="correctionGrade">Hạng phân loại</Label>
          <Input
            id="correctionGrade"
            maxLength={100}
            placeholder="VD: Hạng A, Loại 1"
            {...register('grade')}
          />
          {errors.grade && (
            <p className="text-sm text-red-500" role="alert">
              {errors.grade.message}
            </p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="correctionPreprocessingDate">
            Ngày sơ chế <span className="text-red-500">*</span>
          </Label>
          <Input
            id="correctionPreprocessingDate"
            type="date"
            max={getLocalDateString()}
            aria-invalid={Boolean(errors.preprocessingDate)}
            {...register('preprocessingDate')}
          />
          {errors.preprocessingDate && (
            <p className="text-sm text-red-500" role="alert">
              {errors.preprocessingDate.message}
            </p>
          )}
        </div>
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="correctionProcessingMethod">Phương pháp sơ chế</Label>
          <Textarea
            id="correctionProcessingMethod"
            rows={4}
            maxLength={500}
            {...register('processingMethod')}
          />
          {errors.processingMethod && (
            <p className="text-sm text-red-500" role="alert">
              {errors.processingMethod.message}
            </p>
          )}
        </div>
        <div className="space-y-2 sm:col-span-2">
          <Label htmlFor="correctionReason">
            Lý do đính chính <span className="text-red-500">*</span>
          </Label>
          <Textarea
            id="correctionReason"
            rows={3}
            maxLength={500}
            placeholder="Nêu rõ thông tin sai và căn cứ điều chỉnh..."
            aria-invalid={Boolean(errors.correctionReason)}
            {...register('correctionReason')}
          />
          <div className="flex justify-between gap-3 text-xs text-muted-foreground">
            <span className={errors.correctionReason ? 'text-red-500' : ''}>
              {errors.correctionReason?.message}
            </span>
            <span>{watch('correctionReason')?.length ?? 0}/500</span>
          </div>
        </div>
      </div>
      <section className="space-y-3" aria-labelledby="correction-location-heading">
        <div>
          <h2
            id="correction-location-heading"
            className="flex items-center gap-2 font-semibold text-amber-800"
          >
            <MapPin className="size-4" /> Vị trí đính chính
          </h2>
          <p className="text-sm text-muted-foreground">
            Chọn lại trên bản đồ nếu vị trí trong sự kiện gốc chưa chính xác.
          </p>
        </div>
        <LocationPicker
          onLocationSelect={handleLocationSelect}
          initialPosition={currentPosition}
          height="280px"
        />
      </section>
    </>
  );
}
