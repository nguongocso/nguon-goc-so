import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Search, Loader2, Check, Plus, ShieldAlert, ChevronDown, X } from 'lucide-react';
import { getInputMaterials } from '@/api/inputMaterialApi';
import { MaterialGroup, MATERIAL_GROUP_LABELS } from '@/enums/materialGroup';
import type { InputMaterial } from '@/types/inputMaterial';
import { cn } from '@/lib/utils';

export interface InputMaterialSelectProps {
  value: string;
  onValueChange: (value: string) => void;
  onSelectMaterial?: (material: InputMaterial | null) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  id?: string;
}

const GROUP_BADGE_STYLES: Record<string, string> = {
  [MaterialGroup.PESTICIDE]: 'bg-rose-50 text-rose-700 border-rose-200',
  [MaterialGroup.FERTILIZER]: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  [MaterialGroup.BIOLOGICAL]: 'bg-cyan-50 text-cyan-700 border-cyan-200',
  [MaterialGroup.OTHER]: 'bg-slate-50 text-slate-700 border-slate-200',
};

export const InputMaterialSelect: React.FC<InputMaterialSelectProps> = ({
  value,
  onValueChange,
  onSelectMaterial,
  placeholder = 'T�m ki?m v?t tu, ph�n b�n, thu?c BVTV...',
  disabled = false,
  className,
  id,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState(value || '');
  const [options, setOptions] = useState<InputMaterial[]>([]);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setQuery(value || '');
  }, [value]);

  const fetchMaterials = useCallback(async (searchTerm: string) => {
    setLoading(true);
    try {
      const response = await getInputMaterials({
        keyword: searchTerm.trim() || undefined,
        isActive: true,
        size: 20,
      });
      setOptions(response.content || []);
    } catch {
      setOptions([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    const timer = setTimeout(() => fetchMaterials(query), 250);
    return () => clearTimeout(timer);
  }, [query, isOpen, fetchMaterials]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelectOption = (material: InputMaterial) => {
    onValueChange(material.name);
    if (onSelectMaterial) onSelectMaterial(material);
    setQuery(material.name);
    setIsOpen(false);
  };

  const handleSelectCustomText = (text: string) => {
    const trimmed = text.trim();
    onValueChange(trimmed);
    if (onSelectMaterial) onSelectMaterial(null);
    setQuery(trimmed);
    setIsOpen(false);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setQuery(val);
    onValueChange(val);
    if (onSelectMaterial) onSelectMaterial(null);
    if (!isOpen) setIsOpen(true);
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setQuery('');
    onValueChange('');
    if (onSelectMaterial) onSelectMaterial(null);
  };

  const isExactMatch = options.some(
    (opt) => opt.name.toLowerCase().trim() === query.toLowerCase().trim()
  );

  return (
    <div ref={containerRef} className={cn('relative w-full', className)}>
      <div className="relative flex items-center">
        <Search className="pointer-events-none absolute left-3 size-4 text-slate-400" />
        <input
          id={id}
          type="text"
          value={query}
          onChange={handleInputChange}
          onFocus={() => setIsOpen(true)}
          placeholder={placeholder}
          disabled={disabled}
          autoComplete="off"
          className={cn(
            'h-10 w-full rounded-lg border border-slate-200 bg-white pl-9 pr-16 text-sm text-slate-900 placeholder:text-slate-400 outline-none transition',
            'focus:border-emerald-600 focus:ring-3 focus:ring-emerald-100',
            'disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500'
          )}
        />
        <div className="absolute right-2.5 flex items-center gap-1">
          {query && !disabled && (
            <button type="button" onClick={handleClear} className="p-1 text-slate-400 hover:text-slate-600 rounded-full">
              <X className="size-3.5" />
            </button>
          )}
          <button type="button" onClick={() => !disabled && setIsOpen(!isOpen)} className="p-1 text-slate-400 hover:text-slate-600">
            <ChevronDown className={cn('size-4 transition-transform', isOpen && 'rotate-180')} />
          </button>
        </div>
      </div>

      {isOpen && (
        <div className="absolute left-0 right-0 top-full z-50 mt-1 max-h-72 overflow-y-auto rounded-xl border border-slate-200 bg-white p-1.5 shadow-lg">
          {loading ? (
            <div className="flex items-center justify-center py-6 text-sm text-slate-500 gap-2">
              <Loader2 className="size-4 animate-spin text-emerald-600" />
              <span>�ang t�m ki?m...</span>
            </div>
          ) : (
            <div className="space-y-1">
              {options.length > 0 ? (
                options.map((item) => {
                  const isSelected = item.name.toLowerCase() === value.toLowerCase();
                  const groupLabel = MATERIAL_GROUP_LABELS[item.materialGroup] || item.materialGroup;
                  const badgeStyle = GROUP_BADGE_STYLES[item.materialGroup] || GROUP_BADGE_STYLES[MaterialGroup.OTHER];

                  return (
                    <div
                      key={item.id}
                      onClick={() => handleSelectOption(item)}
                      className={cn(
                        'flex cursor-pointer flex-col gap-1 rounded-lg px-3 py-2 text-sm transition hover:bg-emerald-50/60',
                        isSelected && 'bg-emerald-50 font-medium text-emerald-900'
                      )}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2 min-w-0">
                          <span className="font-semibold text-slate-800 truncate">{item.name}</span>
                          <span className={cn('inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold border', badgeStyle)}>
                            {groupLabel}
                          </span>
                        </div>
                        {isSelected && <Check className="size-4 text-emerald-600 shrink-0" />}
                      </div>

                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                        {item.activeIngredient && <span>Hoạt chất: <strong className="text-slate-700">{item.activeIngredient}</strong></span>}
                        <span>�Đơn vị: <strong className="text-slate-700">{item.unit}</strong></span>
                        {item.quarantineDays > 0 ? (
                          <span className="inline-flex items-center gap-1 font-semibold text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded border border-amber-200">
                            <ShieldAlert className="size-3 text-amber-600" />
                            C�ch ly {item.quarantineDays} ng�y
                          </span>
                        ) : (
                          <span className="text-slate-400">Kh�ng y�u c?u c�ch ly</span>
                        )}
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="px-3 py-3 text-center text-sm text-slate-500">
                  Kh�ng t�m th?y v?t tu trong danh m?c.
                </div>
              )}

              {query.trim().length > 0 && !isExactMatch && (
                <div
                  onClick={() => handleSelectCustomText(query)}
                  className="flex cursor-pointer items-center gap-2 rounded-lg border border-dashed border-emerald-300 bg-emerald-50/30 px-3 py-2 text-sm text-emerald-800 hover:bg-emerald-100/50"
                >
                  <Plus className="size-4 text-emerald-600 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <span>S? d?ng v?t tu ngo�i danh m?c: </span>
                    <strong className="font-bold text-emerald-900">"{query.trim()}"</strong>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
