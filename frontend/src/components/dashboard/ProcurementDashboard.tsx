import { useState } from "react";
import { ShoppingCart } from "lucide-react";
import { ProcurementShipmentList } from "@/components/shipment/ProcurementShipmentList";
import { RecordProcurementDialog } from "@/components/procurement/RecordProcurementDialog";
import { HelpButton } from "@/components/help/HelpButton";
import { ListPageHeader } from "@/components/common/ListPageHeader";

/**
 * Dashboard dành cho Doanh nghiệp thu mua (VT‑04).
 * Hiển thị danh sách lô hàng liên quan tới tổ chức: đã thu mua, được bàn giao
 * hoặc đã nhập kho. Ghi nhận thu mua dành cho lô đã nhận (có phiếu ACCEPTED).
 * Lối vào "Phiếu bàn giao nhận" nằm trong menu Thu mua (sidebar), không đặt
 * nút trên header để tránh trùng lối vào.
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
        description="Danh sách lô hàng đã thu mua, được bàn giao hoặc đã nhập kho của tổ chức bạn; ghi nhận thu mua cho các lô đã xác nhận nhận."
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