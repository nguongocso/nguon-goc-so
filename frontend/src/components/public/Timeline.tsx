import React from 'react';
import {
  Calendar,
  Package,
  Truck,
  Sprout,
  AlertTriangle,
  FileSignature,
  ShoppingCart,
  PackageCheck,
  Warehouse,
  Thermometer,
  GitFork,
} from 'lucide-react';
import type { PublicChainEventItem } from '@/types/publicTrace';
import {
  getEventTypeLabel,
  getTranslatedEventData,
  formatDisplayDateTime,
} from '@/utils/eventFormatter';
import type { ComponentType } from 'react';
import { useLanguage } from '@/context/LanguageContext';

const EVENT_ICONS: Record<string, ComponentType<{ className?: string }>> = {
  HARVEST: Sprout,
  PREPROCESSING: PackageCheck,
  PACKAGING: Package,
  TRANSPORT: Truck,
  PROCUREMENT: ShoppingCart,
  CORRECTION: Calendar,
  HANDOVER: FileSignature,
  WAREHOUSE_RECEIPT: Warehouse,
  WAREHOUSE_ENTRY: Warehouse,
  WAREHOUSE_EXIT: Warehouse,
  STORAGE_CONDITION: Thermometer,
  SPLIT: GitFork,
};

const USER_TEXT_FIELDS = new Set([
  'notes',
  'correctionReason',
  'conditionNote',
  'reason',
  'earlyHarvestReason',
  'processingMethod',
  'fromLocation',
  'toLocation',
  'destination',
  'warehouseName',
]);

interface TimelineProps {
  events: PublicChainEventItem[];
}

export const Timeline: React.FC<TimelineProps> = ({ events }: TimelineProps) => {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';

  if (!events || events.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <p>{isEn ? "No events recorded for this shipment." : "Chưa có sự kiện nào được ghi nhận cho lô hàng này."}</p>
      </div>
    );
  }

  return (
    <div className="relative pl-6 border-l-2 border-border space-y-6">
      {events.map((event, index) => {
        const Icon = EVENT_ICONS[event.eventType] || Calendar;
        const rawLabel = getEventTypeLabel(event.eventType, lang);
        const eventTypeKey = `event_${event.eventType}` as any;
        const translatedLabel = t(eventTypeKey);
        const label = translatedLabel && !translatedLabel.startsWith('event_') ? translatedLabel : rawLabel;

        const isEarlyHarvest =
          event.eventType === 'HARVEST' &&
          (event.eventData?.['earlyHarvest'] === true || event.eventData?.['earlyHarvest'] === 'true');
        const translatedData = getTranslatedEventData(
          event.eventType,
          (event.eventData as Record<string, unknown>) || {},
          lang,
        );
        const entries = Object.entries(translatedData);

        return (
          <div key={index} className="relative pl-6">
            {/* Dot trên timeline */}
            <div className="absolute left-[-9px] top-1 w-4 h-4 rounded-full bg-primary border-2 border-white shadow-sm" />

            <div className="bg-card rounded-lg border border-border p-4 shadow-card transition-shadow">
              <div className="flex items-start gap-3">
                <div className="mt-0.5 p-1.5 bg-primary-light rounded-full">
                  <Icon className="h-4 w-4 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2 flex-wrap">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-foreground">{label}</span>
                      {isEarlyHarvest && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2 py-0.5 text-xs font-semibold text-amber-600 border border-amber-500/20">
                          <AlertTriangle className="h-3 w-3 text-amber-500" />
                          {isEn ? "Early Harvest" : "Thu hoạch sớm"}
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <Calendar className="h-3 w-3" />
                      {formatDisplayDateTime(event.recordedAt, lang)}
                    </span>
                  </div>
                  {entries.length > 0 && (
                    <div className="mt-1 text-sm text-muted-foreground space-y-1">
                      {entries.map(([fieldLabel, value]) => {
                        const rawKeys = Object.keys(event.eventData || {});
                        const matchingKey = rawKeys.find(k => USER_TEXT_FIELDS.has(k));
                        const isUserText = Boolean(matchingKey);

                        return (
                          <div key={fieldLabel} className="flex gap-2 items-center flex-wrap">
                            <span className="font-medium text-muted-foreground">
                              {fieldLabel}:
                            </span>
                            <span className="text-foreground break-words">{value}</span>
                            {isEn && isUserText && (
                              <span
                                className="inline-flex items-center rounded-md bg-amber-50 px-1.5 py-0.5 text-[10px] font-medium text-amber-700 ring-1 ring-inset ring-amber-600/20"
                                title={t('original_badge_tooltip')}
                              >
                                [{t('original_badge')}]
                              </span>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};