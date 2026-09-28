import React, { useMemo } from 'react';
import { Label } from '@/components/ui/label';
import type { AdministrativeUnitNode } from '@/types/administrativeUnit';

export interface AdministrativeUnitSingleSelectProps {
  units: AdministrativeUnitNode[];
  provinceId?: string | null;
  communeId?: string | null;
  onProvinceChange: (provinceId: string) => void;
  onCommuneChange: (communeId: string) => void;
  disabled?: boolean;
  loading?: boolean;
}

/**
 * Component chọn đơn vị hành chính 2 cấp (Tỉnh/Thành phố → Xã/Phường).
 * Đồng bộ với danh mục hành chính 2 cấp (bỏ cấp huyện theo mô hình 2025).
 */
export const AdministrativeUnitSingleSelect: React.FC<AdministrativeUnitSingleSelectProps> = ({
  units,
  provinceId,
  communeId,
  onProvinceChange,
  onCommuneChange,
  disabled = false,
  loading = false,
}) => {
  // Danh sách các tỉnh/thành phố (level = PROVINCE)
  const provinces = useMemo(() => {
    return units.filter((u) => u.level === 'PROVINCE');
  }, [units]);

  // Tỉnh hiện tại đang chọn
  const selectedProvince = useMemo(() => {
    return provinces.find((p) => p.id === provinceId) ?? null;
  }, [provinces, provinceId]);

  // Danh sách xã/phường trực thuộc tỉnh đang chọn (level = COMMUNE)
  const communes = useMemo(() => {
    if (!selectedProvince) return [];
    return selectedProvince.children || [];
  }, [selectedProvince]);

  const handleProvinceSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newProvinceId = e.target.value;
    onProvinceChange(newProvinceId);
    // Khi đổi tỉnh, reset xã
    onCommuneChange('');
  };

  const handleCommuneSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onCommuneChange(e.target.value);
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Chọn Tỉnh / Thành phố */}
        <div className="space-y-2">
          <Label htmlFor="select-province" className="text-sm font-medium text-slate-700 dark:text-slate-200">
            Tỉnh / Thành phố
          </Label>
          <div className="relative">
            <select
              id="select-province"
              value={provinceId || ''}
              onChange={handleProvinceSelect}
              disabled={disabled || loading}
              className="flex h-11 w-full rounded-lg border border-input bg-white dark:bg-slate-950 px-4 py-2 text-sm text-foreground shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:border-emerald-500 disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 transition-colors"
            >
              <option value="">-- Chọn Tỉnh / Thành phố --</option>
              {provinces.map((province) => (
                <option key={province.id} value={province.id}>
                  {province.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Chọn Xã / Phường */}
        <div className="space-y-2">
          <Label htmlFor="select-commune" className="text-sm font-medium text-slate-700 dark:text-slate-200">
            Xã / Phường
          </Label>
          <div className="relative">
            <select
              id="select-commune"
              value={communeId || ''}
              onChange={handleCommuneSelect}
              disabled={disabled || loading || !provinceId || communes.length === 0}
              className="flex h-11 w-full rounded-lg border border-input bg-white dark:bg-slate-950 px-4 py-2 text-sm text-foreground shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:border-emerald-500 disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 transition-colors"
            >
              <option value="">
                {!provinceId
                  ? '-- Vui lòng chọn Tỉnh/Thành phố trước --'
                  : communes.length === 0
                  ? '-- Không có xã/phường trực thuộc --'
                  : '-- Chọn Xã / Phường --'}
              </option>
              {communes.map((commune) => (
                <option key={commune.id} value={commune.id}>
                  {commune.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>
    </div>
  );
};
