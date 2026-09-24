import { Camera, Trash2 } from 'lucide-react';

import { Button } from '@/components/ui/button';

import { MAX_PREPROCESSING_IMAGES } from '../preprocessingFormUtils';

/** Thuộc tính cho phần quản lý hình ảnh sơ chế thực địa. */
export interface PreprocessingImagesSectionProps {
  imageFiles: File[];
  imagePreviews: string[];
  isSubmitting: boolean;
  onImageChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onRemoveImage: (index: number) => void;
}

/** Thành phần hiển thị khu vực tải lên và xem trước ảnh thực địa cho sự kiện sơ chế. */
export function PreprocessingImagesSection({
  imageFiles,
  imagePreviews,
  isSubmitting,
  onImageChange,
  onRemoveImage,
}: PreprocessingImagesSectionProps) {
  return (
    <section className="space-y-3 rounded-lg border border-emerald-100 bg-emerald-50/20 p-4">
      <div>
        <h2
          id="preprocessing-images-heading"
          className="flex items-center gap-2 font-semibold text-emerald-800"
        >
          <Camera className="size-4" /> Hình ảnh thực địa
        </h2>
        <p className="text-sm text-muted-foreground">
          Tối đa {MAX_PREPROCESSING_IMAGES} ảnh, không quá 5 MB mỗi ảnh.
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button
          type="button"
          size="sm"
          variant="outline"
          disabled={isSubmitting || imageFiles.length >= MAX_PREPROCESSING_IMAGES}
          onClick={() => document.getElementById('preprocessing-images')?.click()}
        >
          <Camera className="mr-1 size-4" /> Chọn ảnh
        </Button>
        <span className="text-sm text-muted-foreground">
          {imageFiles.length}/{MAX_PREPROCESSING_IMAGES}
        </span>
        <input
          id="preprocessing-images"
          type="file"
          accept="image/*"
          multiple
          className="sr-only"
          onChange={onImageChange}
          disabled={isSubmitting}
        />
      </div>

      {imagePreviews.length > 0 && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
          {imagePreviews.map((preview, index) => (
            <div
              key={preview}
              className="group relative overflow-hidden rounded-lg border bg-muted"
            >
              <img
                src={preview}
                alt={`Ảnh sơ chế ${index + 1}`}
                className="aspect-square w-full object-cover"
              />
              <Button
                type="button"
                size="icon"
                variant="destructive"
                className="absolute right-1 top-1 size-8"
                aria-label={`Xóa ảnh ${index + 1}`}
                onClick={() => onRemoveImage(index)}
              >
                <Trash2 className="size-4" />
              </Button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
