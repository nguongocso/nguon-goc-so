import { useMemo } from 'react';
import { MapPin, X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useAdministrativeUnits } from '@/hooks/useAdministrativeUnits';

interface ProvinceUnitMultiSelectProps {
  value: string[];
  onChange: (ids: string[]) => void;
  disabled?: boolean;
}

/**
 * Multi-select gọn theo cấp tỉnh cho bộ lọc báo cáo (NCL-742 §8).
 * Khác với AdministrativeUnitCascadeSelect (cây tỉnh→xã dùng cho màn hình
 * có không gian lớn), component này tự nạp danh mục và chỉ chọn tỉnh — đủ dùng
 * cho các dải filter hẹp; giá trị là mảng unitId gửi lên query `unitIds`.
 */
export function ProvinceUnitMultiSelect({
  value,
  onChange,
  disabled = false,
}: ProvinceUnitMultiSelectProps) {
  const { units, loading } = useAdministrativeUnits();

  const nameById = useMemo(
    () => new Map(units.map((province) => [province.id, province.name])),
    [units],
  );

  const toggleProvince = (provinceId: string) => {
    if (!provinceId) return;
    onChange(
      value.includes(provinceId)
        ? value.filter((id) => id !== provinceId)
        : [...value, provinceId],
    );
  };

  return (
    <div className="space-y-2" data-testid="province-multi-select">
      <Select
        value=""
        onValueChange={(provinceId) => toggleProvince(provinceId ?? '')}
        disabled={disabled || loading}
      >
        <SelectTrigger className="w-full">
          <SelectValue
            placeholder={loading ? 'Đang tải địa bàn...' : 'Chọn tỉnh (có thể chọn nhiều)'}
          />
        </SelectTrigger>
        <SelectContent>
          {units.map((province) => (
            <SelectItem key={province.id} value={province.id}>
              {value.includes(province.id) ? '✓ ' : ''}
              {province.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {value.length > 0 && (
        <div className="flex flex-wrap items-center gap-1.5">
          {value.map((provinceId) => (
            <Badge key={provinceId} variant="outline" className="gap-1 pr-1">
              <MapPin className="h-3 w-3 text-emerald-500" />
              {nameById.get(provinceId) ?? provinceId}
              <Button
                size="icon-xs"
                variant="ghost"
                aria-label={`Bỏ chọn ${nameById.get(provinceId) ?? provinceId}`}
                disabled={disabled}
                onClick={() => toggleProvince(provinceId)}
                className="h-4 w-4 p-0"
              >
                <X className="size-3" />
              </Button>
            </Badge>
          ))}
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-6 px-2 text-xs text-muted-foreground"
            disabled={disabled}
            onClick={() => onChange([])}
          >
            Xóa tất cả
          </Button>
        </div>
      )}
    </div>
  );
}

export default ProvinceUnitMultiSelect;
