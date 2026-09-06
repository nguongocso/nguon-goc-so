import type { ReactNode } from "react";
import { CheckCircle2 } from "lucide-react";

export interface LotLookupItem {
  label: string;
  value: ReactNode;
}

interface LotLookupResultProps {
  items: LotLookupItem[];
  title?: string;
}

/**
 * Khối kết quả tra cứu mã truy xuất dùng chung (màu xanh dương thống nhất).
 * Chỉ khác dữ liệu truyền vào qua `items`, ví dụ:
 * - Bảo quản: Lô / Sản phẩm / Vùng trồng / Trạng thái.
 * - Nhập kho: Lô / Đơn vị / Số lượng khai báo.
 */
export function LotLookupResult({
  items,
  title = "Đã tìm thấy lô sản xuất",
}: LotLookupResultProps) {
  return (
    <div className="space-y-2 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2.5">
      <p className="flex items-center gap-1.5 text-sm font-semibold text-blue-800">
        <CheckCircle2 className="size-4" />
        {title}
      </p>
      <div className="grid grid-cols-1 gap-x-4 gap-y-1 text-sm text-blue-800 sm:grid-cols-2">
        {items.map((item) => (
          <p key={item.label}>
            <span className="font-medium">{item.label}:</span> {item.value}
          </p>
        ))}
      </div>
    </div>
  );
}
