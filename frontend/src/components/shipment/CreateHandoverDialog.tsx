import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, Truck } from "lucide-react";
import { toast } from "sonner";
import type { Shipment } from "@/types/shipment";
import type { Organization } from "@/types/organization";
import { getOrganizations } from "@/api/organizationApi";
import { createHandover, getRemainingQuantity } from "@/api/handoverApi";

interface CreateHandoverDialogProps {
  open: boolean;
  shipment: Shipment | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateHandoverDialog = ({
  open,
  shipment,
  onClose,
  onSuccess,
}: CreateHandoverDialogProps) => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [remainingQuantity, setRemainingQuantity] = useState<number>(0);
  const [selectedOrgId, setSelectedOrgId] = useState<string>("");
  const [quantity, setQuantity] = useState<string>("");
  const [plannedAt, setPlannedAt] = useState<string>("");
  const [vehicleInfo, setVehicleInfo] = useState<string>("");
  const [carrierName, setCarrierName] = useState<string>("");
  const [note, setNote] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(false);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    if (open && shipment) {
      loadData();
    }
  }, [open, shipment]);

  const loadData = async () => {
    setLoadingData(true);
    setError("");
    try {
      const [orgs, remaining] = await Promise.all([
        getOrganizations(),
        getRemainingQuantity(shipment!.id),
      ]);
      setOrganizations(orgs.filter((o) => o.status === "ACTIVE"));
      setRemainingQuantity(remaining);
    } catch (err: any) {
      setError(err.message || "Không thể tải dữ liệu");
    } finally {
      setLoadingData(false);
    }
  };

  const handleClose = () => {
    setSelectedOrgId("");
    setQuantity("");
    setPlannedAt("");
    setVehicleInfo("");
    setCarrierName("");
    setNote("");
    setError("");
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!shipment || !selectedOrgId || !quantity) return;

    setLoading(true);
    setError("");

    try {
      await createHandover({
        shipmentId: shipment.id,
        toOrganizationId: selectedOrgId,
        quantity: Number(quantity),
        plannedAt: plannedAt || undefined,
        vehicleInfo: vehicleInfo || undefined,
        carrierName: carrierName || undefined,
        note: note || undefined,
      });
      toast.success("Tạo phiếu bàn giao thành công");
      onSuccess();
      handleClose();
    } catch (err: any) {
      setError(err.message || "Không thể tạo phiếu bàn giao");
    } finally {
      setLoading(false);
    }
  };

  const quantityNum = Number(quantity);
  const isValidQuantity = quantityNum > 0 && quantityNum <= remainingQuantity;

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && handleClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Tạo phiếu bàn giao</DialogTitle>
        </DialogHeader>
        {loadingData ? (
          <div className="flex justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label>Tổ chức nhận</Label>
              <Select value={selectedOrgId} onValueChange={(value) => setSelectedOrgId(value ?? "")}>
                <SelectTrigger>
                  <SelectValue placeholder="Chọn tổ chức nhận" />
                </SelectTrigger>
                <SelectContent>
                  {organizations.map((org) => (
                    <SelectItem key={org.id} value={org.id}>
                      {org.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Số lượng (kg)</Label>
              <Input
                type="number"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                placeholder="Nhập số lượng"
                min={1}
              />
              <p className="text-xs text-muted-foreground">
                Còn lại có thể bàn giao: {remainingQuantity} kg
              </p>
              {quantity && !isValidQuantity && (
                <p className="text-xs text-destructive">
                  Số lượng phải lớn hơn 0 và không vượt quá {remainingQuantity} kg
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label>Thời điểm dự kiến</Label>
              <Input
                type="datetime-local"
                value={plannedAt}
                onChange={(e) => setPlannedAt(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>Phương tiện</Label>
              <Input
                value={vehicleInfo}
                onChange={(e) => setVehicleInfo(e.target.value)}
                placeholder="Biển số xe, loại phương tiện..."
              />
            </div>
            <div className="space-y-2">
              <Label>Người áp tải</Label>
              <Input
                value={carrierName}
                onChange={(e) => setCarrierName(e.target.value)}
                placeholder="Tên người áp tải"
              />
            </div>
            <div className="space-y-2">
              <Label>Ghi chú</Label>
              <Textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Ghi chú thêm (nếu có)"
                rows={2}
              />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={handleClose}>
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={loading || !selectedOrgId || !isValidQuantity}
              >
                {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                <Truck className="mr-2 h-4 w-4" />
                Tạo phiếu
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
};
