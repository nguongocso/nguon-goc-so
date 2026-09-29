import React, { useState } from 'react';
import { Layers, Package, Sprout, Tag, ZoomIn } from 'lucide-react';
import { useLanguage } from '@/context/LanguageContext';
import { getAssetUrl } from '@/config/runtimeConfig';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';

interface ProductInfoProps {
  productName?: string | null;
  productNameEn?: string | null;
  productImageUrl?: string | null;
  lotName?: string | null;
  lotCode?: string | null;
  shipmentCode?: string | null;
  status: string;
}

export const ProductInfo: React.FC<ProductInfoProps> = ({
  productName,
  productNameEn,
  productImageUrl,
  lotName,
  shipmentCode,
  status,
}) => {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';
  const [zoomOpen, setZoomOpen] = useState(false);
  const [imageError, setImageError] = useState(false);

  const displayName = isEn ? (productNameEn || productName || t('not_updated')) : (productName || t('not_updated'));

  const statusLabelMap: Record<string, string> = {
    ACTIVE: t('status_active'),
    ACTIVATED: t('status_active'),
    SUSPECTED: t('status_suspected'),
    LOCKED: t('status_locked'),
    CANCELLED: t('status_cancelled'),
    RECALLED: t('status_recalled'),
    RECALLING: t('status_recalling'),
    DRAFT: t('status_draft'),
    CODE_PRINTED: t('status_code_printed'),
  };

  const statusColorMap: Record<string, string> = {
    ACTIVE: 'text-emerald-700 bg-emerald-50 border-emerald-200',
    ACTIVATED: 'text-emerald-700 bg-emerald-50 border-emerald-200',
    SUSPECTED: 'text-rose-700 bg-rose-50 border-rose-200',
    LOCKED: 'text-rose-700 bg-rose-50 border-rose-200',
    CANCELLED: 'text-gray-700 bg-gray-100 border-gray-200',
    RECALLED: 'text-red-700 bg-red-50 border-red-200',
    RECALLING: 'text-amber-700 bg-amber-50 border-amber-200',
    DRAFT: 'text-gray-700 bg-gray-100 border-gray-200',
    CODE_PRINTED: 'text-blue-700 bg-blue-50 border-blue-200',
  };

  const rawUrl = productImageUrl && !imageError ? getAssetUrl(productImageUrl) : null;

  return (
    <div className="bg-card rounded-xl border border-border shadow-card p-5 space-y-4">
      {/* Ảnh Hero Banner sản phẩm cao cấp (Hiển thị đẹp cả mobile và desktop) */}
      {rawUrl && (
        <div className="relative w-full overflow-hidden rounded-xl bg-slate-100 border border-slate-200/80 group">
          <img
            src={rawUrl}
            alt={displayName}
            onError={() => setImageError(true)}
            onClick={() => setZoomOpen(true)}
            className="w-full aspect-[16/9] sm:h-72 sm:aspect-auto object-cover cursor-pointer transition-transform duration-300 group-hover:scale-[1.02]"
          />
          <button
            type="button"
            onClick={() => setZoomOpen(true)}
            aria-label="Phóng to ảnh"
            className="absolute bottom-2.5 right-2.5 flex items-center gap-1.5 rounded-lg bg-black/60 px-2.5 py-1 text-xs font-medium text-white backdrop-blur-xs transition hover:bg-black/80 shadow-xs"
          >
            <ZoomIn className="h-3.5 w-3.5" />
            <span>Phóng to</span>
          </button>
        </div>
      )}

      {/* Tiêu đề và trạng thái */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-border pb-3">
        <div>
          <span className="text-xs uppercase font-medium tracking-wider text-muted-foreground">
            {t('product_info_title')}
          </span>
          <h1 className="text-xl sm:text-2xl font-bold text-foreground mt-0.5">
            {displayName}
          </h1>
        </div>
        <div>
          <span
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border ${
              statusColorMap[status] || 'bg-muted text-muted-foreground border-border'
            }`}
          >
            <Tag className="h-3.5 w-3.5" />
            {statusLabelMap[status] || status}
          </span>
        </div>
      </div>

      {/* Lưới thông tin chi tiết */}
      <div
        className={`grid grid-cols-1 ${
          shipmentCode ? 'sm:grid-cols-3' : 'sm:grid-cols-2'
        } gap-4 text-sm`}
      >
        <div className="p-3 bg-muted/40 rounded-lg border border-border/50">
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground mb-1">
            <Sprout className="h-3.5 w-3.5 text-emerald-600" />
            {t('product_name_label')}
          </span>
          <p className="font-semibold text-foreground">
            {displayName}
          </p>
        </div>

        <div className="p-3 bg-muted/40 rounded-lg border border-border/50">
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground mb-1">
            <Layers className="h-3.5 w-3.5 text-blue-600" />
            {t('lot_name_label')}
          </span>
          <p className="font-semibold text-foreground flex items-center flex-wrap gap-1">
            <span>{lotName || 'N/A'}</span>
            {isEn && lotName && (
              <span
                className="inline-flex items-center rounded-md bg-amber-50 px-1.5 py-0.5 text-[10px] font-medium text-amber-700 ring-1 ring-inset ring-amber-600/20"
                title={t('original_badge_tooltip')}
              >
                [{t('original_badge')}]
              </span>
            )}
          </p>
        </div>

        {shipmentCode && (
          <div className="p-3 bg-muted/40 rounded-lg border border-border/50">
            <span className="flex items-center gap-1.5 text-xs text-muted-foreground mb-1">
              <Package className="h-3.5 w-3.5 text-amber-600" />
              {t('shipment_code_label')}
            </span>
            <p className="font-semibold text-foreground break-all flex items-center flex-wrap gap-1">
              <span>{shipmentCode}</span>
              {isEn && (
                <span
                  className="inline-flex items-center rounded-md bg-amber-50 px-1.5 py-0.5 text-[10px] font-medium text-amber-700 ring-1 ring-inset ring-amber-600/20"
                  title={t('original_badge_tooltip')}
                >
                  [{t('original_badge')}]
                </span>
              )}
            </p>
          </div>
        )}
      </div>

      {/* Lightbox xem phóng to toàn màn hình */}
      {rawUrl && (
        <Dialog open={zoomOpen} onOpenChange={setZoomOpen}>
          <DialogContent className="max-w-3xl p-3 bg-black/95 border-none text-white shadow-2xl">
            <DialogHeader className="sr-only">
              <DialogTitle>{displayName}</DialogTitle>
            </DialogHeader>
            <div className="flex flex-col items-center justify-center p-1">
              <img
                src={rawUrl}
                alt={displayName}
                className="max-h-[80vh] w-auto max-w-full rounded-lg object-contain"
              />
              <p className="mt-3 text-center text-sm font-medium text-slate-200">
                {displayName}
              </p>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
};
