import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  AlertCircle,
  BadgeCheck,
  Check,
  ChevronDown,
  LoaderCircle,
  Search,
} from "lucide-react";

import { cn } from "@/lib/utils";
import type { TestingUnit } from "@/types/certification";

/**
 * Lấy ngày hiện tại theo định dạng ISO YYYY-MM-DD (giờ địa phương).
 */
const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

interface TestingUnitSelectProps {
  /** Danh sách đơn vị kiểm nghiệm còn hiệu lực (đã lọc isActive từ backend). */
  units: TestingUnit[];
  /** ID đơn vị đang chọn ("" nếu chưa chọn). */
  value: string;
  /** Callback khi người dùng chọn / bỏ chọn một đơn vị. */
  onChange: (unit: TestingUnit | null) => void;
  disabled?: boolean;
  /** Hiển thị viền đỏ (trạng thái lỗi validation). */
  invalid?: boolean;
  loading?: boolean;
  placeholder?: string;
  id?: string;
}

/**
 * Dropdown có tìm kiếm chọn đơn vị kiểm nghiệm từ danh mục dùng chung
 * (NCL-11-CN-006 Phase 1). Hiển thị mã công nhận và cảnh báo sắp hết hạn.
 */
export const TestingUnitSelect: React.FC<TestingUnitSelectProps> = ({
  units,
  value,
  onChange,
  disabled = false,
  invalid = false,
  loading = false,
  placeholder = "Chọn đơn vị kiểm nghiệm...",
  id,
}) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const today = useMemo(() => toISODate(new Date()), []);

  // Đóng dropdown khi click ra ngoài hoặc nhấn Escape
  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const filteredUnits = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return units;
    return units.filter(
      (unit) =>
        unit.name.toLowerCase().includes(keyword) ||
        unit.accreditationCode.toLowerCase().includes(keyword)
    );
  }, [units, search]);

  const selectedUnit = units.find((unit) => unit.id === value) || null;

  const isExpired = (unit: TestingUnit) =>
    !!unit.accreditationExpiryDate && unit.accreditationExpiryDate < today;

  const isExpiringSoon = (unit: TestingUnit) =>
    !!unit.accreditationExpiryDate &&
    !isExpired(unit) &&
    unit.accreditationExpiryDate <=
      toISODate(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000));

  const handleSelect = (unit: TestingUnit | null) => {
    onChange(unit);
    setOpen(false);
    setSearch("");
  };

  return (
    <div ref={containerRef} className="relative">
      {/* Trigger */}
      <button
        type="button"
        id={id}
        disabled={disabled || loading}
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="listbox"
        aria-expanded={open}
        className={cn(
          "flex h-10 w-full items-center justify-between gap-2 rounded-xl border bg-white px-3 text-xs transition-all",
          "focus:outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/20",
          invalid ? "border-red-400 ring-2 ring-red-100" : "border-input hover:border-emerald-300",
          disabled && "cursor-not-allowed bg-muted text-muted-foreground"
        )}
      >
        <span className="min-w-0 flex-1 truncate text-left">
          {loading ? (
            <span className="flex items-center gap-2 text-muted-foreground">
              <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
              Đang tải danh mục đơn vị kiểm nghiệm...
            </span>
          ) : selectedUnit ? (
            <span className="font-medium text-foreground">{selectedUnit.name}</span>
          ) : (
            <span className="text-muted-foreground">{placeholder}</span>
          )}
        </span>
        <ChevronDown
          className={cn(
            "h-4 w-4 shrink-0 text-muted-foreground transition-transform",
            open && "rotate-180"
          )}
        />
      </button>

      {open && (
        <div
          role="listbox"
          className="absolute z-50 mt-1 w-full overflow-hidden rounded-xl border border-border bg-white shadow-lg"
        >
          <div className="border-b border-border p-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
              <input
                autoFocus
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm theo tên hoặc mã công nhận..."
                className="h-8 w-full rounded-lg border border-input bg-white pl-8 pr-2 text-xs outline-none focus:border-emerald-400"
              />
            </div>
          </div>

          <div className="max-h-56 overflow-y-auto p-1">
            {filteredUnits.length === 0 ? (
              <p className="px-3 py-4 text-center text-xs text-muted-foreground">
                Không tìm thấy đơn vị kiểm nghiệm phù hợp.
              </p>
            ) : (
              filteredUnits.map((unit) => {
                const isSelected = unit.id === value;
                return (
                  <button
                    key={unit.id}
                    type="button"
                    role="option"
                    aria-selected={isSelected}
                    onClick={() => handleSelect(isSelected ? null : unit)}
                    className={cn(
                      "flex w-full items-start gap-2 rounded-lg px-2.5 py-2 text-left text-xs transition-colors",
                      isSelected ? "bg-emerald-50" : "hover:bg-muted/60",
                      isExpired(unit) && "opacity-70"
                    )}
                  >
                    <span
                      className={cn(
                        "mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full border",
                        isSelected
                          ? "border-emerald-500 bg-emerald-500 text-white"
                          : "border-input"
                      )}
                    >
                      {isSelected && <Check className="h-2.5 w-2.5" />}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-medium text-foreground">
                        {unit.name}
                      </span>
                      <span className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-muted-foreground">
                        <span className="inline-flex items-center gap-0.5 font-mono">
                          <BadgeCheck className="h-3 w-3" />
                          {unit.accreditationCode}
                        </span>
                        {unit.accreditationExpiryDate && (
                          <span
                            className={cn(
                              isExpired(unit)
                                ? "font-medium text-red-600"
                                : isExpiringSoon(unit)
                                  ? "font-medium text-amber-600"
                                  : ""
                            )}
                          >
                            Hết hạn: {unit.accreditationExpiryDate}
                            {isExpired(unit) && " (đã hết hạn)"}
                            {isExpiringSoon(unit) && " (sắp hết hạn)"}
                          </span>
                        )}
                      </span>
                    </span>
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* Cảnh báo hết hạn của đơn vị đã chọn */}
      {selectedUnit && isExpired(selectedUnit) && (
        <p className="mt-1 flex items-center gap-1 text-xs font-medium text-red-600">
          <AlertCircle className="h-3.5 w-3.5" />
          Đơn vị này đã hết hạn công nhận, hệ thống sẽ không cho phép gửi yêu cầu.
        </p>
      )}
    </div>
  );
};