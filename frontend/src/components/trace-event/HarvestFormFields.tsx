import { Calendar, Camera } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { LocationPicker } from '@/pages/packaging-event/components/LocationPicker';
import { cn } from '@/lib/utils';
import { preventMouseUpCollapse, selectAllOnFocus } from '@/utils/inputUtils';
import { HarvestEligibilityAlert } from './HarvestEligibilityAlert';
import type { HarvestFormController } from './useHarvestForm';

/** Các trường nghiệp vụ của biểu mẫu thu hoạch. */
export function HarvestFormFields({
  controller,
}: {
  controller: HarvestFormController;
}) {
  const {
    canOverride,
    currentPosition,
    eligibility,
    formState: { errors },
    handleImageChange,
    handleLocationSelect,
    imageFiles,
    imagePreviews,
    isEarlyHarvest,
    isOverrideBlocked,
    isSubmitting,
    loadingEligibility,
    maxImages,
    register,
    removeImage,
    selectedHarvestDate,
  } = controller;
  return (
    <>
      <HarvestEligibilityAlert
        loading={loadingEligibility}
        eligibility={eligibility}
        isEarlyHarvest={isEarlyHarvest}
        isOverrideBlocked={isOverrideBlocked}
        selectedHarvestDate={selectedHarvestDate}
      />
      <div className="space-y-2">
        <Label htmlFor="harvestDate">
          Ngày thu hoạch <span className="text-red-500">*</span>
        </Label>
        <div className="relative">
          <Calendar className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            id="harvestDate"
            type="date"
            className="pl-10"
            {...register('harvestDate')}
          />
        </div>
        {errors.harvestDate && (
          <p className="text-sm text-red-500">{errors.harvestDate.message}</p>
        )}
      </div>
      {canOverride && (
        <div className="space-y-2 rounded-lg border border-amber-300 bg-amber-50/60 p-3.5">
          <Label
            htmlFor="earlyHarvestReason"
            className="flex items-center gap-1 font-semibold text-amber-950"
          >
            Lý do thu hoạch sớm <span className="text-red-500">*</span>
          </Label>
          <Textarea
            id="earlyHarvestReason"
            rows={3}
            placeholder={
              'Nhập lý do bắt buộc giải trình thu hoạch trước thời gian cách ly ' +
              '(ví dụ: bão lũ, thời tiết bất lợi, v.v.)...'
            }
            className="border-amber-300 bg-white focus-visible:ring-amber-400"
            {...register('earlyHarvestReason')}
          />
          {errors.earlyHarvestReason && (
            <p className="text-sm text-red-500">{errors.earlyHarvestReason.message}</p>
          )}
        </div>
      )}
      <div className="space-y-2">
        <Label htmlFor="quantity">
          Sản lượng thu hoạch (kg) <span className="text-red-500">*</span>
        </Label>
        <Input
          id="quantity"
          type="number"
          step="0.01"
          min="0.01"
          placeholder="Nhập sản lượng thực tế"
          {...register('quantity', { valueAsNumber: true })}
          onFocus={selectAllOnFocus}
          onMouseUp={preventMouseUpCollapse}
        />
        {errors.quantity && (
          <p className="text-sm text-red-500">{errors.quantity.message}</p>
        )}
      </div>
      <div className="space-y-2">
        <Label>Vị trí thu hoạch (click trên bản đồ)</Label>
        <LocationPicker
          onLocationSelect={handleLocationSelect}
          initialPosition={currentPosition}
          height="300px"
        />
      </div>
      <div className="space-y-2">
        <Label>Hình ảnh thực địa (tối đa {maxImages})</Label>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => document.getElementById('harvest-image-input')?.click()}
            disabled={isSubmitting || imageFiles.length >= maxImages}
          >
            <Camera className="mr-1 h-4 w-4" /> Chọn ảnh
          </Button>
          <span className="text-sm text-muted-foreground">
            {imageFiles.length}/{maxImages}
          </span>
          <input
            id="harvest-image-input"
            type="file"
            accept="image/*"
            multiple
            className="hidden"
            onChange={handleImageChange}
            disabled={isSubmitting}
          />
        </div>
        {imagePreviews.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-2">
            {imagePreviews.map((source, index) => (
              <div
                key={source}
                className="relative h-16 w-16 overflow-hidden rounded border"
              >
                <img
                  src={source}
                  alt={`Ảnh thu hoạch ${index + 1}`}
                  className="h-full w-full object-cover"
                />
                <button
                  type="button"
                  aria-label={`Xóa ảnh ${index + 1}`}
                  className={cn(
                    'absolute -right-1 -top-1 flex h-5 w-5 items-center',
                    'justify-center rounded-full bg-red-500 text-xs text-white',
                    'hover:bg-red-600',
                  )}
                  onClick={() => removeImage(index)}
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
