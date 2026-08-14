import { Calendar, Package, Truck, Sprout, Clipboard } from 'lucide-react';
import type { PublicChainEventItem } from '@/types/publicTrace';
import {
  getEventTypeLabel,
  getTranslatedEventData,
  formatDisplayDateTime,
} from '@/utils/eventFormatter';
import type { ComponentType } from 'react';

const EVENT_ICONS: Record<string, ComponentType<{ className?: string }>> = {
  HARVEST: Sprout,
  PACKAGING: Package,
  TRANSPORT: Truck,
  PROCUREMENT: Clipboard,
  CORRECTION: Calendar,
};

interface TimelineProps {
  events: PublicChainEventItem[];
}

export const Timeline = ({ events }: TimelineProps) => {
  if (!events || events.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        <p>Chưa có sự kiện nào được ghi nhận cho lô hàng này.</p>
      </div>
    );
  }

  return (
    <div className="relative pl-6 border-l-2 border-border space-y-6">
      {events.map((event, index) => {
        const Icon = EVENT_ICONS[event.eventType] || Calendar;
        const label = getEventTypeLabel(event.eventType);
        const translatedData = getTranslatedEventData(
          event.eventType,
          (event.eventData as Record<string, unknown>) || {},
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
                    <span className="font-semibold text-foreground">{label}</span>
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <Calendar className="h-3 w-3" />
                      {formatDisplayDateTime(event.recordedAt)}
                    </span>
                  </div>
                  {entries.length > 0 && (
                    <div className="mt-1 text-sm text-muted-foreground space-y-1">
                      {entries.map(([fieldLabel, value]) => (
                        <div key={fieldLabel} className="flex gap-2">
                          <span className="font-medium text-muted-foreground">
                            {fieldLabel}:
                          </span>
                          <span className="text-foreground break-words">{value}</span>
                        </div>
                      ))}
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