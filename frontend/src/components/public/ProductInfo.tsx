import { Layers, Package, Sprout, Tag } from 'lucide-react';

import { useLanguage } from '@/context/LanguageContext';

interface ProductInfoProps {
  productName?: string | null;
  productNameEn?: string | null;
  lotName?: string | null;
  lotCode?: string | null;
  shipmentCode?: string | null;
  status: string;
}

/**
 * Hiển thị thông tin tổng quan sản phẩm và mã truy xuất.
 */
export function ProductInfo({
  productName,
  productNameEn,
  lotName,
  shipmentCode,
  status,
}: ProductInfoProps) {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';

  const displayName = isEn
    ? productNameEn || productName || t('not_updated')
    : productName || t('not_updated');

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

  return (
    <div className="bg-card rounded-xl border border-border shadow-card p-5 space-y-4">
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
    </div>
  );
}

export default ProductInfo;
