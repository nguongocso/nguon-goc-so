import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { AlertCircle, FileX } from 'lucide-react';

interface Props {
  open: boolean;
  onClose: () => void;
  missingDocs: string[];
  shipmentName: string;
}

export const DossierIneligibleDialog = ({ open, onClose, missingDocs, shipmentName }: Props) => {
  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2 text-amber-600">
            <AlertCircle className="h-5 w-5" />
            <DialogTitle>Không đủ điều kiện xuất hồ sơ</DialogTitle>
          </div>
          <DialogDescription>
            Lô hàng <strong>{shipmentName || 'được chọn'}</strong> chưa đáp ứng đủ các điều kiện sau:
          </DialogDescription>
        </DialogHeader>
        {missingDocs && missingDocs.length > 0 ? (
          <ul className="list-disc pl-5 space-y-1.5 text-sm text-rose-600 bg-rose-50 p-3 rounded-lg border border-rose-100">
            {missingDocs.map((doc, idx) => (
              <li key={idx} className="leading-snug">{doc}</li>
            ))}
          </ul>
        ) : (
          <div className="flex items-center gap-2 p-3 bg-amber-50 rounded-lg text-sm text-amber-700">
            <FileX className="h-4 w-4 shrink-0" />
            <span>Lô hàng chưa hoàn tất hoặc thiếu chứng từ bắt buộc để xuất hồ sơ.</span>
          </div>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Đóng</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};