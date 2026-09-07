import React, { useMemo, useState } from 'react';
import {
  ChevronsUpDown,
  MapPin,
  Plus,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import type { AdministrativeUnitNode } from '@/types/administrativeUnit';

export interface AdministrativeAreaPickerProps {
  units: AdministrativeUnitNode[];
  value: string[]; // Danh sách commune unitId đã chọn
  onChange: (ids: string[]) => void;
  disabled?: boolean;
  loading?: boolean;
}

export const AdministrativeAreaPicker: React.FC<AdministrativeAreaPickerProps> = ({
  units,
  value,
  onChange,
  disabled = false,
  loading = false,
}) => {
  // Tỉnh đang được chọn để duyệt xã
  const [selectedProvinceId, setSelectedProvinceId] = useState<string>('');
  // Từ khóa tìm tỉnh
  const [provinceSearch, setProvinceSearch] = useState('');
  const [isProvinceOpen, setIsProvinceOpen] = useState(false);

  // Từ khóa tìm xã trong tỉnh đang chọn
  const [communeSearch, setCommuneSearch] = useState('');
  // Các xã đang được tick tạm thời (chờ bấm "Thêm vào danh sách")
  const [tempSelectedCommuneIds, setTempSelectedCommuneIds] = useState<Set<string>>(new Set());

  // Từ khóa lọc trong danh sách đã chọn
  const [selectedFilter, setSelectedFilter] = useState('');

  const selectedSet = useMemo(() => new Set(value), [value]);

  // Map tra cứu nhanh
  const { provinceMap, communeMap, communeToProvinceMap } = useMemo(() => {
    const pMap = new Map<string, AdministrativeUnitNode>();
    const cMap = new Map<string, AdministrativeUnitNode>();
    const c2pMap = new Map<string, AdministrativeUnitNode>();

    for (const province of units) {
      pMap.set(province.id, province);
      for (const commune of province.children) {
        cMap.set(commune.id, commune);
        c2pMap.set(commune.id, province);
      }
    }
    return { provinceMap: pMap, communeMap: cMap, communeToProvinceMap: c2pMap };
  }, [units]);

  // Tỉnh hiện tại đang chọn
  const currentProvince = useMemo(
    () => (selectedProvinceId ? provinceMap.get(selectedProvinceId) ?? null : null),
    [selectedProvinceId, provinceMap],
  );

  // Lọc danh sách tỉnh theo từ khóa
  const filteredProvinces = useMemo(() => {
    const q = provinceSearch.trim().toLowerCase();
    if (!q) return units;
    return units.filter(
      (p) => p.name.toLowerCase().includes(q) || p.code.includes(q),
    );
  }, [units, provinceSearch]);

  // Lọc danh sách xã của tỉnh đang chọn theo từ khóa
  const filteredCommunes = useMemo(() => {
    if (!currentProvince) return [];
    const q = communeSearch.trim().toLowerCase();
    if (!q) return currentProvince.children;
    return currentProvince.children.filter(
      (c) => c.name.toLowerCase().includes(q) || c.code.includes(q),
    );
  }, [currentProvince, communeSearch]);

  // Nhóm các địa bàn đã chọn theo tỉnh
  const groupedSelected = useMemo(() => {
    const groups: Record<
      string,
      { province: AdministrativeUnitNode; communes: AdministrativeUnitNode[] }
    > = {};

    const filterQ = selectedFilter.trim().toLowerCase();

    for (const communeId of value) {
      const commune = communeMap.get(communeId);
      const province = communeToProvinceMap.get(communeId);
      if (commune && province) {
        // Lọc nếu có từ khóa tìm trong danh sách đã chọn
        if (
          filterQ &&
          !commune.name.toLowerCase().includes(filterQ) &&
          !province.name.toLowerCase().includes(filterQ)
        ) {
          continue;
        }
        if (!groups[province.id]) {
          groups[province.id] = { province, communes: [] };
        }
        groups[province.id].communes.push(commune);
      }
    }

    return Object.values(groups);
  }, [value, communeMap, communeToProvinceMap, selectedFilter]);

  // Chọn tỉnh từ dropdown
  const handleSelectProvince = (provinceId: string) => {
    setSelectedProvinceId(provinceId);
    setIsProvinceOpen(false);
    setCommuneSearch('');
    setTempSelectedCommuneIds(new Set());
  };

  // Toggle tick một xã trong tỉnh đang chọn
  const handleToggleTempCommune = (communeId: string) => {
    if (disabled) return;
    setTempSelectedCommuneIds((prev) => {
      const next = new Set(prev);
      if (next.has(communeId)) {
        next.delete(communeId);
      } else {
        next.add(communeId);
      }
      return next;
    });
  };

  // Chọn tất cả xã của tỉnh đang chọn (những xã chưa được thêm vào value)
  const handleSelectAllInProvince = () => {
    if (!currentProvince || disabled) return;
    const availableCommunes = currentProvince.children.filter(
      (c) => !selectedSet.has(c.id),
    );
    setTempSelectedCommuneIds(new Set(availableCommunes.map((c) => c.id)));
  };

  // Bỏ chọn tất cả xã đang tick tạm
  const handleDeselectAllInProvince = () => {
    setTempSelectedCommuneIds(new Set());
  };

  // Bấm "Thêm vào danh sách"
  const handleAddTempToSelected = () => {
    if (tempSelectedCommuneIds.size === 0 || disabled) return;
    const newSelected = Array.from(new Set([...value, ...tempSelectedCommuneIds]));
    onChange(newSelected);
    setTempSelectedCommuneIds(new Set());
  };

  // Xóa 1 xã khỏi danh sách đã chọn
  const handleRemoveCommune = (communeId: string) => {
    if (disabled) return;
    onChange(value.filter((id) => id !== communeId));
  };

  // Xóa toàn bộ danh sách đã chọn
  const handleClearAll = () => {
    if (disabled) return;
    onChange([]);
    setTempSelectedCommuneIds(new Set());
  };

  return (
    <div className="space-y-4" data-testid="area-picker">
      {/* ── Tiêu đề & Thống kê đã chọn ── */}
      <div className="flex items-center justify-between border-b pb-2">
        <span className="text-sm font-semibold text-foreground">
          Chọn địa bàn (Tỉnh / Xã)
        </span>
        <Badge
          variant="secondary"
          className="border border-emerald-200 bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200"
        >
          Đã chọn: {value.length} xã/phường
        </Badge>
      </div>

      {/* ── Khu vực 1: Chọn Tỉnh và Xã/Phường ── */}
      <div className="space-y-3 rounded-lg border bg-muted/20 p-3">
        {/* a) Chọn Tỉnh / Thành phố */}
        <div className="space-y-1.5">
          <label className="text-xs font-medium text-foreground">
            1. Chọn Tỉnh / Thành phố
          </label>
          <div className="relative">
            <button
              type="button"
              data-testid="province-select-trigger"
              aria-label="Chọn tỉnh/thành phố"
              disabled={disabled || loading}
              onClick={() => setIsProvinceOpen((prev) => !prev)}
              className="flex w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-foreground shadow-2xs hover:bg-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <span className={currentProvince ? 'font-medium' : 'text-muted-foreground'}>
                {loading
                  ? 'Đang tải danh mục địa bàn...'
                  : currentProvince
                    ? currentProvince.name
                    : 'Chọn tỉnh/thành phố...'}
              </span>
              <ChevronsUpDown className="h-4 w-4 shrink-0 text-muted-foreground" />
            </button>

            {isProvinceOpen && (
              <div className="absolute z-50 mt-1 max-h-60 w-full overflow-hidden rounded-md border bg-popover text-popover-foreground shadow-lg">
                <div className="border-b p-2">
                  <div className="relative">
                    <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      aria-label="Tìm tỉnh thành"
                      placeholder="Tìm kiếm tỉnh/thành phố..."
                      value={provinceSearch}
                      onChange={(e) => setProvinceSearch(e.target.value)}
                      className="h-8 pl-8 text-xs"
                      autoFocus
                    />
                  </div>
                </div>
                <div className="max-h-48 overflow-y-auto p-1">
                  {filteredProvinces.length === 0 ? (
                    <p className="py-3 text-center text-xs text-muted-foreground">
                      Không tìm thấy tỉnh/thành phù hợp.
                    </p>
                  ) : (
                    filteredProvinces.map((province) => {
                      const isSelected = province.id === selectedProvinceId;
                      return (
                        <button
                          key={province.id}
                          type="button"
                          data-testid={`province-option-${province.name}`}
                          onClick={() => handleSelectProvince(province.id)}
                          className={`flex w-full items-center justify-between rounded-sm px-2.5 py-1.5 text-left text-xs transition-colors ${
                            isSelected
                              ? 'bg-emerald-100 text-emerald-900 font-medium dark:bg-emerald-950 dark:text-emerald-200'
                              : 'hover:bg-muted'
                          }`}
                        >
                          <span className="flex items-center gap-1.5">
                            <MapPin className="h-3 w-3 text-emerald-600 shrink-0" />
                            {province.name}
                          </span>
                          <span className="text-[10px] text-muted-foreground">
                            {province.children.length} xã/phường
                          </span>
                        </button>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* b) Chọn Xã / Phường thuộc tỉnh đang chọn */}
        {currentProvince ? (
          <div className="space-y-2 border-t pt-3">
            <div className="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:justify-between">
              <label className="text-xs font-medium text-foreground">
                2. Chọn Xã / Phường thuộc <span className="font-semibold text-emerald-700 dark:text-emerald-400">{currentProvince.name}</span>
              </label>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleSelectAllInProvince}
                  disabled={disabled}
                  className="h-6 text-[11px] px-1.5 text-emerald-700 hover:text-emerald-800 hover:bg-emerald-50"
                >
                  Chọn tất cả
                </Button>
                <span className="text-muted-foreground text-xs">|</span>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleDeselectAllInProvince}
                  disabled={disabled || tempSelectedCommuneIds.size === 0}
                  className="h-6 text-[11px] px-1.5 text-muted-foreground hover:text-foreground"
                >
                  Bỏ chọn
                </Button>
              </div>
            </div>

            {/* Thanh tìm kiếm xã */}
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
              <Input
                aria-label="Tìm xã/phường"
                placeholder="Lọc xã/phường theo tên..."
                value={communeSearch}
                onChange={(e) => setCommuneSearch(e.target.value)}
                className="h-8 pl-8 text-xs"
              />
            </div>

            {/* Danh sách checkbox xã */}
            <div className="max-h-48 space-y-1 overflow-y-auto rounded-md border bg-background p-1.5">
              {filteredCommunes.length === 0 ? (
                <p className="py-4 text-center text-xs text-muted-foreground">
                  Không tìm thấy xã/phường phù hợp.
                </p>
              ) : (
                filteredCommunes.map((commune) => {
                  const isAlreadyAdded = selectedSet.has(commune.id);
                  const isTempChecked = tempSelectedCommuneIds.has(commune.id);

                  return (
                    <div
                      key={commune.id}
                      className={`flex items-center justify-between rounded px-2 py-1.5 text-xs transition-colors ${
                        isAlreadyAdded
                          ? 'bg-muted/40 text-muted-foreground'
                          : isTempChecked
                            ? 'bg-emerald-50 text-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-100'
                            : 'hover:bg-muted/50'
                      }`}
                    >
                      <div className="flex items-center gap-2 min-w-0">
                        <Checkbox
                          checked={isAlreadyAdded || isTempChecked}
                          disabled={disabled || isAlreadyAdded}
                          onCheckedChange={() => handleToggleTempCommune(commune.id)}
                          aria-label={commune.name}
                        />
                        <span
                          className={`truncate select-none ${isAlreadyAdded ? 'cursor-not-allowed opacity-75' : 'cursor-pointer'}`}
                          onClick={() => !isAlreadyAdded && handleToggleTempCommune(commune.id)}
                        >
                          {commune.name}
                        </span>
                      </div>
                      {isAlreadyAdded && (
                        <span className="shrink-0 text-[10px] text-emerald-600 dark:text-emerald-400 font-medium">
                          ✓ Đã thêm
                        </span>
                      )}
                    </div>
                  );
                })
              )}
            </div>

            {/* c) Nút "Thêm vào danh sách" */}
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={disabled || tempSelectedCommuneIds.size === 0}
              onClick={handleAddTempToSelected}
              className="w-full text-xs gap-1.5 border-emerald-300 text-emerald-700 hover:bg-emerald-50 dark:border-emerald-800 dark:text-emerald-300"
            >
              <Plus className="h-3.5 w-3.5" />
              Thêm vào danh sách ({tempSelectedCommuneIds.size})
            </Button>
          </div>
        ) : (
          <p className="text-center py-2 text-xs text-muted-foreground">
            Vui lòng chọn Tỉnh / Thành phố ở trên để xem danh sách xã/phường.
          </p>
        )}
      </div>

      {/* ── Khu vực 2: Danh sách địa bàn đã chọn (chờ gán) ── */}
      <div className="space-y-2 rounded-lg border p-3">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-foreground">
            Địa bàn đã chọn ({value.length}):
          </span>
          {value.length > 0 && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={disabled}
              onClick={handleClearAll}
              className="h-6 text-[11px] px-1 text-rose-600 hover:text-rose-700 hover:bg-rose-50"
            >
              <Trash2 className="h-3 w-3 mr-1" />
              Xóa tất cả
            </Button>
          )}
        </div>

        {value.length > 5 && (
          <div className="relative">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input
              aria-label="Lọc địa bàn đã chọn"
              placeholder="Tìm trong danh sách đã chọn..."
              value={selectedFilter}
              onChange={(e) => setSelectedFilter(e.target.value)}
              className="h-7 pl-8 text-xs"
            />
          </div>
        )}

        {value.length === 0 ? (
          <div className="rounded-md border border-dashed py-6 text-center text-xs text-muted-foreground">
            Chưa có địa bàn nào được chọn. Hãy chọn tỉnh và xã/phường ở trên rồi bấm "Thêm vào danh sách".
          </div>
        ) : groupedSelected.length === 0 ? (
          <p className="py-4 text-center text-xs text-muted-foreground">
            Không tìm thấy địa bàn đã chọn khớp từ khóa.
          </p>
        ) : (
          <div className="max-h-52 space-y-2 overflow-y-auto pr-1" data-testid="pending-area-list">
            {groupedSelected.map(({ province, communes }) => (
              <div key={province.id} className="rounded-md border bg-card p-2 space-y-1.5">
                <div className="flex items-center justify-between text-xs font-medium text-emerald-800 dark:text-emerald-300">
                  <span className="flex items-center gap-1">
                    <MapPin className="h-3 w-3 text-emerald-600" />
                    {province.name}
                  </span>
                  <span className="text-[10px] text-muted-foreground">
                    {communes.length} xã/phường
                  </span>
                </div>

                <ul className="flex flex-wrap gap-1.5 pl-2">
                  {communes.map((commune) => (
                    <li key={commune.id}>
                      <Badge
                        variant="secondary"
                        className="flex items-center gap-1 py-0.5 pl-2 pr-1 text-xs border border-emerald-200 bg-emerald-50 text-emerald-900 dark:bg-emerald-950/40 dark:border-emerald-800 dark:text-emerald-200"
                      >
                        <span>{commune.name}</span>
                        <button
                          type="button"
                          aria-label={`Xóa ${commune.name}`}
                          disabled={disabled}
                          onClick={() => handleRemoveCommune(commune.id)}
                          className="h-4 w-4 rounded-full p-0 flex items-center justify-center text-muted-foreground hover:bg-rose-100 hover:text-rose-700 dark:hover:bg-rose-950 transition-colors"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </Badge>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdministrativeAreaPicker;
