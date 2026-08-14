import { useState } from "react";
import { ShoppingCart } from "lucide-react";
import { ProcurementShipmentList } from "@/components/shipment/ProcurementShipmentList";
import { RecordProcurementDialog } from "@/components/procurement/RecordProcurementDialog";
import { Button } from "@/components/ui/button";

/**
 * Dashboard dành cho Doanh nghiệp thu mua (VT‑04).
 * Hiển thị danh sách lô hàng đã kích hoạt tem, sẵn sàng ghi nhận thu mua.
 */
export function ProcurementDashboard() {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedShipmentId, setSelectedShipmentId] = useState<
    string | undefined
  >();

  const handleRecordProcurement = (shipmentId: string) => {
    setSelectedShipmentId(shipmentId);
    setDialogOpen(true);
  };

  const handleOpenNewProcurement = () => {
    setSelectedShipmentId(undefined);
    setDialogOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Thu mua nông sản
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Xem danh sách lô hàng đã kích hoạt tem và thực hiện ghi nhận thu
            mua.
          </p>
        </div>
        <Button onClick={handleOpenNewProcurement}>
          <ShoppingCart className="mr-2 size-4" />
          Ghi nhận thu mua mới
        </Button>
      </div>

      <ProcurementShipmentList
        onRecordProcurement={handleRecordProcurement}
      />

      <RecordProcurementDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initialShipmentId={selectedShipmentId}
        onSuccess={() => {
          // Dialog đã đóng, toast đã hiển thị từ hook
        }}
      />
    </div>
  );
}