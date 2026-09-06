import type { ReactNode } from "react";
import { AlertTriangle, CheckCircle2 } from "lucide-react";

export interface LotLookupItem {
  label: string;
  value: ReactNode;
}

interface LotLookupNotice {
  title: string;
  description: string;
}

interface LotLookupResultProps {
  items: LotLookupItem[];
  title?: string;
  blocked?: boolean;
  notice?: LotLookupNotice;
}

/**
 * Khối thông tin lô sau khi tra cứu mã truy xuất.
 * Có thể kèm theo trạng thái nghiệp vụ nếu người dùng chưa đủ điều kiện thao tác.
 */
export function LotLookupResult({
  items,
  title = "Thông tin lô hàng",
  blocked = false,
  notice,
}: LotLookupResultProps) {
  return (
    <div
      className={
        blocked
          ? "space-y-3 rounded-lg border border-amber-200 bg-amber-50/70 px-3.5 py-3"
          : "space-y-3 rounded-lg border border-slate-200 bg-slate-50/70 px-3.5 py-3"
      }
    >
      <div className="flex items-center justify-between gap-3">
        <p className="flex items-center gap-1.5 text-sm font-semibold text-slate-800">
          <CheckCircle2 className="size-4 text-emerald-600" />
          {title}
        </p>
        <span className="rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700">
          Đã tìm thấy
        </span>
      </div>

      <div className="grid grid-cols-1 gap-x-5 gap-y-1.5 text-sm text-slate-700 sm:grid-cols-2">
        {items.map((item) => (
          <p key={item.label}>
            <span className="font-medium text-slate-600">{item.label}:</span>{" "}
            {item.value}
          </p>
        ))}
      </div>

      {notice && (
        <div className="flex gap-2.5 rounded-md border border-amber-200 bg-amber-50 px-3 py-2.5 text-amber-900">
          <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" />
          <div className="min-w-0">
            <p className="text-sm font-semibold">{notice.title}</p>
            <p className="mt-0.5 text-xs leading-5 text-amber-800">
              {notice.description}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
