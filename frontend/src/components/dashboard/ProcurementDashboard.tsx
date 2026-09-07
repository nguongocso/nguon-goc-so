import { useState } from "react";
import { ShoppingCart } from "lucide-react";
import { ProcurementShipmentList } from "@/components/shipment/ProcurementShipmentList";
import { RecordProcurementDialog } from "@/components/procurement/RecordProcurementDialog";
import { HelpButton } from "@/components/help/HelpButton";
import { ListPageHeader } from "@/components/common/ListPageHeader";

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

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={ShoppingCart}
        iconBoxClassName="bg-emerald-500/10"
        title="Thu mua nông sản"
        description="Xem danh sách lô hàng đã kích hoạt tem và thực hiện ghi nhận thu mua."
        actions={<HelpButton screenKey="dashboard" />}
      />

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