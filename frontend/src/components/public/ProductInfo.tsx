import { Layers, Package, Sprout, Tag } from 'lucide-react';

interface ProductInfoProps {
  productName?: string | null;
  lotName?: string | null;
  lotCode?: string | null;
  shipmentCode?: string | null;
  status: string;
}

export const ProductInfo = ({
  productName,
  lotName,
  shipmentCode,
  status,
}: ProductInfoProps) => {
  const statusLabel: Record<string, string> = {
    ACTIVE: 'Đang hoạt động',
    RECALLED: 'Đã thu hồi',
    DRAFT: 'Nháp',
    CODE_PRINTED: 'Đã in mã',
  };

  const statusColor: Record<string, string> = {
    ACTIVE: 'text-emerald-700 bg-emerald-50 border-emerald-200',
    RECALLED: 'text-red-700 bg-red-50 border-red-200',
    DRAFT: 'text-gray-700 bg-gray-100 border-gray-200',
    CODE_PRINTED: 'text-blue-700 bg-blue-50 border-blue-200',
  };

  return (
    <div className="bg-card rounded-xl border border-border shadow-card p-5 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-border pb-3">
        <div>
          <span className="text-xs uppercase font-medium tracking-wider text-muted-foreground">
            Thông tin sản phẩm & nguồn gốc
          </span>
          <h1 className="text-xl sm:text-2xl font-bold text-foreground mt-0.5">
            {productName || 'Chưa cập nhật'}
          </h1>
        </div>
        <div>
          <span
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border ${
              statusColor[status] || 'bg-muted text-muted-foreground border-border'
            }`}
          >
            <Tag className="h-3.5 w-3.5" />
            {statusLabel[status] || status}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
        <div className="p-3 bg-muted/40 rounded-lg border border-border/50">
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground mb-1">
            <Sprout className="h-3.5 w-3.5 text-emerald-600" />
            Tên nông sản
          </span>
          <p className="font-semibold text-foreground">
            {productName || 'Chưa cập nhật'}
          </p>
        </div>

        <div className="p-3 bg-muted/40 rounded-lg border border-border/50">
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground mb-1">
            <Layers className="h-3.5 w-3.5 text-blue-600" />
            Tên lô sản xuất
          </span>
          <p className="font-semibold text-foreground">
            {lotName || 'N/A'}
          </p>
        </div>

        {shipmentCode && (
          <div className="p-3 bg-muted/40 rounded-lg border border-border/50">
            <span className="flex items-center gap-1.5 text-xs text-muted-foreground mb-1">
              <Package className="h-3.5 w-3.5 text-amber-600" />
              Tên lô hàng
            </span>
            <p className="font-semibold text-foreground break-all">
              {shipmentCode}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};