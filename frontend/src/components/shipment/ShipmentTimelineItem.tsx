import { Calendar, MapPin, Package, Truck, Sprout, Clipboard, Pencil, Wheat, AlertTriangle } from 'lucide-react';
import type { ChainEventResponse } from '@/types/packaging';
import type { PreprocessingEventResponse } from '@/types/preprocessing';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';
import {
  getEventTypeLabel,
  formatFieldLabel,
  formatEventValue,
  isEventValueEmpty,
  formatDisplayDateTime,
} from '@/utils/eventFormatter';
import type { ComponentType } from 'react';
import { maskId } from '@/lib/utils';

const EVENT_ICONS: Record<string, ComponentType<{ className?: string }>> = {
  HARVEST: Sprout,
  PREPROCESSING: Wheat,
  PACKAGING: Package,
  TRANSPORT: Truck,
  PROCUREMENT: Clipboard,
  CORRECTION: Pencil,
};

interface Props {
  event: ChainEventResponse;
  index: number;
  total: number;
}

/**
 * A single event entry in the shipment timeline.
 *
 * Displays:
 *  - Event type label & icon (Vietnamese)
 *  - Formatted timestamp (vi-VN)
 *  - Recorded-by name (Vietnamese label)
 *  - Dynamic event data with human-readable Vietnamese labels and formatted values
 *  - GPS coordinates with Vietnamese label when available
 */
export const ShipmentTimelineItem = ({ event, index, total }: Props) => {
  const navigate = useNavigate();
  const canCorrectPreprocessing = usePermission(
    ROLE_ACCESS.preprocessingEventCorrect,
  );
  const Icon = EVENT_ICONS[event.eventType] || Calendar;
  const label = getEventTypeLabel(event.eventType);
  const timestamp = formatDisplayDateTime(event.recordedAt);

  const isEarlyHarvest =
    event.eventType === 'HARVEST' &&
    (event.eventData?.['earlyHarvest'] === true || event.eventData?.['earlyHarvest'] === 'true');

  // Extract and sort event data entries; hide internal fields
  const dataEntries = event.eventData
    ? Object.entries(event.eventData).filter(
        ([key, value]) =>
          key !== 'productionLotId' &&
          key !== 'shipmentId' &&
          key !== 'deviceSource' &&
          key !== 'images' &&
          !(key === 'earlyHarvest' && (value === false || value === 'false')) &&
          !(key === 'unmatchedMaterials' && Array.isArray(value) && value.length === 0),
      )
    : [];

  const productionLotId = event.eventData?.['productionLotId'] as string | undefined;
  const hasCoordinates = event.latitude != null && event.longitude != null;
  const isOriginalPreprocessingEvent =
    event.eventType === 'PREPROCESSING' &&
    !event.eventData?.['parentEventId'];

  return (
    <div className="relative pl-8 pb-6 last:pb-0">
      {/* Vertical connector line */}
      {index < total - 1 && (
        <div className="absolute left-3 top-5 bottom-0 w-0.5 bg-gray-200" />
      )}

      {/* Icon circle */}
      <div className="absolute left-0 top-1.5 flex h-6 w-6 items-center justify-center rounded-full border-2 border-emerald-500 bg-emerald-100">
        <Icon className="h-3.5 w-3.5 text-emerald-600" />
      </div>

      {/* Card */}
      <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md">
        {/* Header: event type + timestamp */}
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-gray-900">{label}</span>
            {isEarlyHarvest && (
              <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-700 border border-amber-200">
                <AlertTriangle className="h-3 w-3 text-amber-600" />
                Thu hoạch sớm
              </span>
            )}
          </div>
          <div className="flex items-center gap-1.5 text-xs text-gray-400">
            <Calendar className="h-3 w-3" />
            <span>{timestamp}</span>
          </div>
        </div>

        {/* Recorded by */}
        {event.recordedByName && (
          <div className="mt-1 text-xs text-gray-400">
            Người ghi nhận:{' '}
            <span className="font-medium text-gray-600">{event.recordedByName}</span>
          </div>
        )}

        {/* Divider */}
        {dataEntries.length > 0 && (
          <div className="my-3 border-t border-gray-100" />
        )}

        {/* Event Details */}
        {dataEntries.length > 0 && (
          <div className="space-y-1.5">
            {dataEntries.map(([key, value]) => {
              const formattedValue = formatEventValue(value);
              if (isEventValueEmpty(value) || !formattedValue) return null;

              return (
                <div key={key} className="flex flex-wrap gap-x-2 text-sm">
                  <span className="font-medium text-gray-500">
                    {formatFieldLabel(key)}
                  </span>
                  <span className="break-words text-gray-700">
                    {formattedValue}
                  </span>
                </div>
              );
            })}
          </div>
        )}

        {/* Production Lot ID (secondary metadata, muted) */}
        {productionLotId && (
          <div className="mt-2 text-xs text-gray-300 break-all">
            Mã: {maskId(productionLotId)}
          </div>
        )}

        {/* Location */}
        {hasCoordinates && (
          <div className="mt-2 flex items-center gap-1 text-xs text-gray-400">
            <MapPin className="h-3 w-3 shrink-0" />
            <span>
              Vị trí: {event.latitude!.toFixed(6)}, {event.longitude!.toFixed(6)}
            </span>
          </div>
        )}

        {canCorrectPreprocessing && isOriginalPreprocessingEvent && (
          <div className="mt-3 border-t border-gray-100 pt-3">
            <Button
              type="button"
              size="sm"
              variant="edit"
              onClick={() =>
                navigate(`/preprocessing-events/${event.id}/correct`, {
                  state: {
                    preprocessingEvent: event as PreprocessingEventResponse,
                  },
                })
              }
            >
              <Pencil className="size-4" />
              Đính chính sơ chế
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
