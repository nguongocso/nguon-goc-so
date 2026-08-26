import { useEffect, useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { getShipmentTimeline } from '@/api/chainEventApi';
import type { ChainEventResponse } from '@/types/packaging';
import { ShipmentTimelineContent } from './ShipmentTimelineContent';

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
          <ShipmentTimelineContent events={events} loading={loading} error={error} />
        </div>
      </DialogContent>
    </Dialog>
  );
};