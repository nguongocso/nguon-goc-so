import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { AlertCircle } from 'lucide-react';

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
            Lô hàng <strong>{shipmentName}</strong> chưa đáp ứng đủ các điều kiện sau:
          </DialogDescription>
        </DialogHeader>
        <ul className="list-disc pl-5 space-y-1 text-sm text-red-600">
          {missingDocs.map((doc, idx) => (
            <li key={idx}>{doc}</li>
          ))}
        </ul>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Đóng</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};