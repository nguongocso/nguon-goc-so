import { Hash, Tag } from "lucide-react";

interface ProductInfoProps {
  productName: string;
  shipmentCode: string;
  status: string;
}

export const ProductInfo = ({ productName, shipmentCode, status }: ProductInfoProps) => {
  const statusLabel: Record<string, string> = {
    ACTIVE: 'Đang hoạt động',
    RECALLED: 'Đã thu hồi',
    DRAFT: 'Nháp',
    CODE_PRINTED: 'Đã in mã',
  };

  const statusColor: Record<string, string> = {
    ACTIVE: 'text-success bg-success-bg',
    RECALLED: 'text-destructive bg-error-bg',
    DRAFT: 'text-muted-foreground bg-muted',
    CODE_PRINTED: 'text-info bg-info-bg',
  };

  return (
    <div className="bg-card rounded-xl border border-border shadow-card p-5 space-y-3">
      <h1 className="text-2xl font-bold text-foreground">{productName}</h1>
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-muted-foreground">
        <div className="flex items-center gap-1">
          <Hash className="h-4 w-4" />
          <span className="font-mono">{shipmentCode}</span>
        </div>
        <div className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium ${statusColor[status] || 'bg-muted text-muted-foreground'}`}>
          <Tag className="h-3 w-3" />
          {statusLabel[status] || status}
        </div>
      </div>
    </div>
  );
};