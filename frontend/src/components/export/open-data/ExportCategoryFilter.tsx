import React from 'react';
import { Controller } from 'react-hook-form';
import type { Control, UseFormSetValue, UseFormWatch } from 'react-hook-form';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { X } from 'lucide-react';
import type { ProductCategory } from '@/types/productCategory';
import type { ExportOpenDataFormValues } from '@/utils/validators';

/** Props cho bộ lọc danh mục sản phẩm */
export interface ExportCategoryFilterProps {
  categories: ProductCategory[];
  control: Control<ExportOpenDataFormValues>;
  setValue: UseFormSetValue<ExportOpenDataFormValues>;
  watch: UseFormWatch<ExportOpenDataFormValues>;
  submitting: boolean;
  errorMessage?: string;
}

/** Component lựa chọn nhiều danh mục sản phẩm với giao diện badge trực quan và nút xóa nhanh. */
export const ExportCategoryFilter: React.FC<ExportCategoryFilterProps> = ({
  categories,
  control,
  setValue,
  watch,
  submitting,
  errorMessage,
}) => {
  const selectedCategoryIds = watch('productCategoryIds') || [];

  const removeCategory = (id: string) => {
    setValue(
      'productCategoryIds',
      selectedCategoryIds.filter((v) => v !== id),
      { shouldValidate: true }
    );
  };

  const clearAllCategories = () => {
    setValue('productCategoryIds', [], { shouldValidate: true });
  };

  const addCategory = (id: string) => {
    if (!id) return;
    if (selectedCategoryIds.includes(id)) {
      removeCategory(id);
    } else {
      setValue('productCategoryIds', [...selectedCategoryIds, id], {
        shouldValidate: true,
      });
    }
  };

  const getCategoryName = (id: string) => {
    return categories.find((c) => c.id === id)?.name || id;
  };

  return (
    <div className="space-y-2">
      <Label htmlFor="productCategoryIds">Danh mục sản phẩm</Label>
      <Controller
        name="productCategoryIds"
        control={control}
        render={({ field }) => (
          <Select
            value=""
            onValueChange={(val) => {
              if (val) addCategory(val);
            }}
            disabled={submitting}
          >
            <SelectTrigger id="productCategoryIds">
              <SelectValue placeholder="Chọn danh mục (có thể chọn nhiều)" />
            </SelectTrigger>
            <SelectContent>
              {categories.map((cat) => (
                <SelectItem key={cat.id} value={cat.id}>
                  {field.value?.includes(cat.id) ? '✓ ' : ''}
                  {cat.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      />
      {errorMessage && (
        <p className="text-sm text-red-500">{errorMessage}</p>
      )}

      {selectedCategoryIds.length > 0 && (
        <div className="mt-2 space-y-2">
          <div className="flex flex-wrap gap-2">
            {selectedCategoryIds.map((id) => (
              <Badge
                key={id}
                variant="secondary"
                className="flex items-center gap-1 pl-3 pr-1 py-1"
              >
                {getCategoryName(id)}
                <button
                  type="button"
                  onClick={() => removeCategory(id)}
                  className="ml-1 rounded-full hover:bg-muted-foreground/20 p-0.5"
                  aria-label={`Xóa ${getCategoryName(id)}`}
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ))}
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-6 px-2 text-xs text-muted-foreground"
              onClick={clearAllCategories}
            >
              Xóa tất cả
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Đã chọn {selectedCategoryIds.length} danh mục
          </p>
        </div>
      )}
    </div>
  );
};
