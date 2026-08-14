import { useEffect, useState } from 'react';
import { resolveScanAnomalyAlert } from '@/api/scanAnomalyAlertApi';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type { ScanAnomalyAlert } from '@/types/scanAnomalyAlert';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface Props {
  alert: ScanAnomalyAlert | null;
  onClose: () => void;
  onResolved: () => void;
}

export function ResolveScanAnomalyAlertDialog({
  alert,
  onClose,
  onResolved,
}: Props) {
  const [resolutionNote, setResolutionNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (alert) setResolutionNote('');
  }, [alert]);

  const handleSubmit = async () => {
    if (!alert) return;

    try {
      setSubmitting(true);
      await resolveScanAnomalyAlert(alert.id, {
        resolutionNote: resolutionNote.trim() || undefined,
      });
      toast.success('Đã đánh dấu cảnh báo là đã xử lý');
      onResolved();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || 'Không thể xử lý cảnh báo lúc này',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog
      open={Boolean(alert)}
      onOpenChange={(open) => !open && !submitting && onClose()}
    >
      <DialogContent className="sm:max-w-lg" showCloseButton={!submitting}>
        {alert && (
          <>
            <DialogHeader>
              <div className="flex items-start gap-3 pr-8">
                <div className="rounded-full bg-amber-100 p-2 text-amber-700">
                  <AlertTriangle className="h-5 w-5" />
                </div>
                <div className="space-y-1">
                  <DialogTitle>Xác nhận xử lý cảnh báo</DialogTitle>
                  <DialogDescription>
                    Thao tác này sẽ chuyển cảnh báo sang trạng thái “Đã xử lý” và ghi vào lịch sử hoạt động.
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            <div className="rounded-lg border bg-muted/20 p-3 text-sm">
              <p>
                <span className="text-muted-foreground">TraceCode:</span>{' '}
                <span className="break-all font-mono">{alert.relatedEntityId}</span>
              </p>
              <p className="mt-1">
                <span className="text-muted-foreground">Dữ liệu:</span>{' '}
                {alert.details.scanCount} lượt quét tại {alert.details.locations.length} vị trí
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="resolutionNote">Ghi chú xử lý (không bắt buộc)</Label>
              <Textarea
                id="resolutionNote"
                value={resolutionNote}
                onChange={(event) => setResolutionNote(event.target.value)}
                placeholder="Ví dụ: Đã liên hệ hợp tác xã và xác minh điểm quét..."
                rows={4}
                disabled={submitting}
              />
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={onClose} disabled={submitting}>
                Hủy
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={submitting}
                variant="edit"
              >
                {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                {submitting ? 'Đang xử lý...' : 'Xác nhận xử lý'}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}