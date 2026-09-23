import { Camera } from 'lucide-react';

import { ScanCodeField } from '@/components/common/ScanCodeField';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  MAX_TRANSPORT_IMAGES,
  type TransportEventController,
} from './useTransportEventForm';

interface TransportEventFieldsProps {
  controller: TransportEventController;
}

/** Hiển thị các trường nhập và ảnh của sự kiện vận chuyển. */
export function TransportEventFields({ controller }: TransportEventFieldsProps) {
  const {
    codeValue, errors, handleImageChange, imageFiles, imagePreviews,
    isSubmitting, register, removeImage, setCodeValue,
  } = controller;

  return (
    <>
      <ScanCodeField
        value={codeValue}
        onChange={setCodeValue}
        error={errors.codeValue?.message}
      />
      <div className="grid gap-6 md:grid-cols-2">
        <LocationField
          id="fromLocation"
          label="Điểm đi *"
          placeholder="Ví dụ: Xã Long Cốc, huyện Tân Sơn, Phú Thọ"
          error={errors.fromLocation?.message}
          inputProps={register('fromLocation')}
        />
        <LocationField
          id="toLocation"
          label="Điểm đến *"
          placeholder="Ví dụ: Kho trung chuyển Việt Trì, Phú Thọ"
          error={errors.toLocation?.message}
          inputProps={register('toLocation')}
        />
      </div>
      <div className="max-w-sm space-y-2">
        <Label htmlFor="transportTime">Thời gian vận chuyển *</Label>
        <Input id="transportTime" type="datetime-local" {...register('transportTime')} />
        {errors.transportTime && (
          <p className="text-sm text-destructive">{errors.transportTime.message}</p>
        )}
      </div>
      <div className="space-y-2">
        <Label>Hình ảnh thực địa (tối đa {MAX_TRANSPORT_IMAGES})</Label>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => document.getElementById('transport-image-input')?.click()}
            disabled={isSubmitting || imageFiles.length >= MAX_TRANSPORT_IMAGES}
          >
            <Camera className="mr-1 h-4 w-4" /> Chọn ảnh
          </Button>
          <span className="text-sm text-muted-foreground">
            {imageFiles.length}/{MAX_TRANSPORT_IMAGES}
          </span>
          <input
            id="transport-image-input"
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
              <div key={source} className="relative h-16 w-16 overflow-hidden rounded border">
                <img
                  src={source}
                  alt={`preview-${index}`}
                  className="h-full w-full object-cover"
                />
                <button
                  type="button"
                  className={[
                    'absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center',
                    'rounded-full bg-red-500 text-xs text-white hover:bg-red-600',
                  ].join(' ')}
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

interface LocationFieldProps {
  id: 'fromLocation' | 'toLocation';
  label: string;
  placeholder: string;
  error?: string;
  inputProps: ReturnType<TransportEventController['register']>;
}

function LocationField({ id, label, placeholder, error, inputProps }: LocationFieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} placeholder={placeholder} {...inputProps} />
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
