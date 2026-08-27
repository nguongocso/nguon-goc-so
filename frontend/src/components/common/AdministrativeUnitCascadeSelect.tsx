import { useMemo, useState } from 'react';
import { ChevronDown, ChevronRight, MapPin, Search, X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import type { AdministrativeUnitNode } from '@/types/administrativeUnit';

interface AdministrativeUnitCascadeSelectProps {
  units: AdministrativeUnitNode[];
  value: string[];
  onChange: (ids: string[]) => void;
  disabled?: boolean;
  loading?: boolean;
}

/**
 * Multi-select cây 2 cấp tỉnh → xã/phường (không có huyện, mô hình 2025).
 * Tái sử dụng cho màn hình gán địa bàn và bộ lọc báo cáo.
 */
export function AdministrativeUnitCascadeSelect({
  units,
  value,
  onChange,
  disabled = false,
  loading = false,
}: AdministrativeUnitCascadeSelectProps) {
  const [keyword, setKeyword] = useState('');
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const selectedSet = useMemo(() => new Set(value), [value]);

  const filteredProvinces = useMemo(() => filterUnits(units, keyword), [units, keyword]);

  const toggleExpanded = (provinceId: string) => {
    if (disabled) return;
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(provinceId)) {
        next.delete(provinceId);
      } else {
        next.add(provinceId);
      }
      return next;
    });
  };

  /** Đảm bảo không trùng: mọi thay đổi đi qua Set rồi xuất mảng mới. */
  const addIds = (ids: string[]) => {
    if (ids.length === 0) return;
    const next = new Set(selectedSet);
    ids.forEach((id) => next.add(id));
    onChange(Array.from(next));
  };

  const removeId = (id: string) => {
    const next = new Set(selectedSet);
    next.delete(id);
    onChange(Array.from(next));
  };

  const toggleCommune = (commune: AdministrativeUnitNode) => {
    if (selectedSet.has(commune.id)) {
      removeId(commune.id);
    } else {
      addIds([commune.id]);
    }
  };

  const toggleProvinceAll = (province: AdministrativeUnitNode) => {
    const communeIds = province.children.map((child) => child.id);
    const allSelected =
      communeIds.length > 0 && communeIds.every((id) => selectedSet.has(id));
    if (allSelected) {
      const next = new Set(selectedSet);
      communeIds.forEach((id) => next.delete(id));
      onChange(Array.from(next));
    } else {
      addIds(communeIds);
    }
  };

  const selectedNodes = useMemo(
    () =>
      units
        .flatMap((province) => [province, ...province.children])
        .filter((node) => selectedSet.has(node.id)),
    [units, selectedSet],
  );

  return (
    <div className="space-y-3" data-testid="cascade-select">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          aria-label="Tìm kiếm địa bàn"
          placeholder="Tìm tỉnh / xã, phường..."
          value={keyword}
          disabled={disabled}
          onChange={(event) => setKeyword(event.target.value)}
          className="pl-9"
        />
      </div>

      <div className="max-h-72 space-y-1 overflow-y-auto rounded-lg border p-2">
        {loading ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            Đang tải danh mục địa bàn...
          </p>
        ) : filteredProvinces.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            Không tìm thấy địa bàn phù hợp.
          </p>
        ) : (
          filteredProvinces.map((province) => {
            const expanded = expandedIds.has(province.id);
            const communeIds = province.children.map((child) => child.id);
            const allSelected =
              communeIds.length > 0 && communeIds.every((id) => selectedSet.has(id));

            return (
              <div key={province.id} className="rounded-md">
                <div className="flex items-center gap-2 rounded-md px-1 py-1.5 hover:bg-emerald-50/60">
                  <button
                    type="button"
                    aria-label={`${expanded ? 'Đóng' : 'Mở'} ${province.name}`}
                    disabled={disabled}
                    onClick={() => toggleExpanded(province.id)}
                    className="rounded p-0.5 text-emerald-600 hover:bg-emerald-100 disabled:opacity-50"
                  >
                    {expanded ? (
                      <ChevronDown className="h-4 w-4" />
                    ) : (
                      <ChevronRight className="h-4 w-4" />
                    )}
                  </button>
                  <Checkbox
                    aria-label={`Chọn tất cả xã phường thuộc ${province.name}`}
                    checked={allSelected}
                    disabled={disabled || communeIds.length === 0}
                    onCheckedChange={() => toggleProvinceAll(province)}
                  />
                  <button
                    type="button"
                    disabled={disabled}
                    onClick={() => toggleExpanded(province.id)}
                    className="flex flex-1 items-center gap-2 text-left text-sm font-medium text-foreground disabled:cursor-not-allowed"
                  >
                    <MapPin className="h-4 w-4 text-emerald-500" />
                    {province.name}
                    <span className="text-xs font-normal text-muted-foreground">
                      ({province.children.length} xã/phường)
                    </span>
                  </button>
                </div>

                {expanded && (
                  <div className="ml-8 space-y-0.5 py-1">
                    {province.children.map((commune) => (
                      <div
                        key={commune.id}
                        className="flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-emerald-50/60"
                      >
                        <Checkbox
                          aria-label={commune.name}
                          checked={selectedSet.has(commune.id)}
                          disabled={disabled}
                          onCheckedChange={() => toggleCommune(commune)}
                        />
                        <span className="text-sm text-foreground">{commune.name}</span>
                        <span className="ml-auto font-mono text-xs text-muted-foreground">
                          {commune.code}
                        </span>
                      </div>
                    ))}
                    {province.children.length === 0 && (
                      <p className="px-2 py-1 text-xs text-muted-foreground">
                        Chưa có xã/phường trong danh mục.
                      </p>
                    )}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {selectedNodes.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground">
            Đã chọn {selectedNodes.length} địa bàn:
          </p>
          <div className="flex flex-wrap gap-1.5">
            {selectedNodes.map((node) => (
              <Badge key={node.id} variant="outline" className="gap-1 pr-1">
                {node.level === 'PROVINCE' ? 'Tỉnh: ' : ''}
                {node.name}
                <Button
                  size="icon-xs"
                  variant="ghost"
                  aria-label={`Bỏ chọn ${node.name}`}
                  disabled={disabled}
                  onClick={() => removeId(node.id)}
                  className="h-4 w-4 p-0"
                >
                  <X className="size-3" />
                </Button>
              </Badge>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function filterUnits(
  units: AdministrativeUnitNode[],
  keyword: string,
): AdministrativeUnitNode[] {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) return units;
  return units
    .map((province) => {
      const provinceMatched = province.name.toLowerCase().includes(normalized);
      const matchedChildren = province.children.filter((child) =>
        child.name.toLowerCase().includes(normalized),
      );
      if (provinceMatched) return province;
      if (matchedChildren.length === 0) return null;
      return { ...province, children: matchedChildren };
    })
    .filter((unit): unit is AdministrativeUnitNode => unit !== null);
}

export default AdministrativeUnitCascadeSelect;
