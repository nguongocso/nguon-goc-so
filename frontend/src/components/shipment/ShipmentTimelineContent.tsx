import { LoaderCircle, AlertCircle, Package } from 'lucide-react';
import type { ChainEventResponse } from '@/types/packaging';
import { ShipmentTimelineItem } from './ShipmentTimelineItem';

interface Props {
  events: ChainEventResponse[];
  loading?: boolean;
  error?: string | null;
}

/**
 * Presentational body of the shipment timeline: loading / error / empty
 * states plus the vertical event stepper.
 *
 * Owns no data fetching — callers (ShipmentTimelineDialog today,
 * ShipmentDetailPage potentially later) keep their own lifecycle and pass
 * resolved state down. Rendering is identical to what previously lived
 * inline in ShipmentTimelineDialog.
 */
export function ShipmentTimelineContent({
  events,
  loading = false,
  error = null,
}: Props) {
  return (
    <>
      {/* Loading */}
      {loading && (
        <div className="flex justify-center py-12">
          <LoaderCircle className="h-8 w-8 animate-spin text-emerald-600" />
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="flex items-start gap-2 rounded-lg bg-red-50 p-4 text-red-600">
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Empty */}
      {!loading && !error && events.length === 0 && (
        <div className="flex flex-col items-center py-12 text-muted-foreground">
          <Package className="mb-3 h-10 w-10 text-gray-300" />
          <p className="text-lg font-semibold">Chưa có sự kiện</p>
          <p className="text-sm">
            Lô hàng này chưa có sự kiện nào được ghi nhận.
          </p>
        </div>
      )}

      {/* Timeline */}
      {!loading && !error && events.length > 0 && (
        <div className="relative py-2">
          {events.map((event, idx) => (
            <ShipmentTimelineItem
              key={event.id}
              event={event}
              index={idx}
              total={events.length}
            />
          ))}
        </div>
      )}
    </>
  );
}
