import { useEffect, useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { LoaderCircle, AlertCircle, Package } from 'lucide-react';
import { getShipmentTimeline } from '@/api/chainEventApi';
import type { ChainEventResponse } from '@/types/packaging';
import { ShipmentTimelineItem } from './ShipmentTimelineItem';

interface Props {
  open: boolean;
  onClose: () => void;
  shipmentId: string;
  shipmentName: string;
}

export const ShipmentTimelineDialog = ({ open, onClose, shipmentId, shipmentName }: Props) => {
  const [events, setEvents] = useState<ChainEventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !shipmentId) return;

    const fetchTimeline = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getShipmentTimeline(shipmentId);
        setEvents(data);
      } catch (err: any) {
        const msg =
          err.response?.data?.message ||
          err.message ||
          'Không thể tải dòng thời gian lô hàng.';
        setError(msg);
      } finally {
        setLoading(false);
      }
    };

    fetchTimeline();
  }, [open, shipmentId]);

  const eventCountText =
    events.length === 1 ? '1 sự kiện' : `${events.length} sự kiện`;

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="flex max-h-[80vh] flex-col overflow-hidden sm:max-w-lg md:max-w-xl lg:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Dòng thời gian lô hàng — {shipmentName}</DialogTitle>
          {!loading && !error && (
            <p className="text-sm text-muted-foreground">{eventCountText}</p>
          )}
        </DialogHeader>

        <div className="flex-1 overflow-y-auto pr-1">
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
        </div>
      </DialogContent>
    </Dialog>
  );
};