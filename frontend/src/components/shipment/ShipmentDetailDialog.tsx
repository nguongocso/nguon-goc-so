import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import type { Shipment } from "@/types/shipment";
import {
  BadgeCheck,
  Ban,
  FileText,
  History,
  Package,
  Trash2,
} from "lucide-react";
import { maskId } from "@/lib/utils";
import { QrCodeGrid } from "./QrCodeGrid";
import { ShipmentStatusBadge } from "./ShipmentStatusBadge";
import { DetailSection } from "@/components/common/detail/DetailSection";
import { DetailField } from "@/components/common/detail/DetailField";

interface ShipmentDetailDialogProps {
  open: boolean;
  shipment: Shipment | null;
  onClose: () => void;
  /** Permission & status-aware action callbacks */
  canActivate: boolean;
  canRecall: boolean;
  onActivate: (shipment: Shipment) => void;
  onRecall: (shipment: Shipment) => void;
  onExportDossier: (shipment: Shipment) => void;
  onDeleteDraft: (shipment: Shipment) => void;
  onViewTimeline: (shipment: Shipment) => void;
}

const formatDateTime = (value: string): string => {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("vi-VN");
};

export const ShipmentDetailDialog = ({
  open,
  shipment,
  onClose,
  canActivate,
  canRecall,
  onActivate,
  onRecall,
  onExportDossier,
  onDeleteDraft,
  onViewTimeline,
}: ShipmentDetailDialogProps) => {
  // Derive action visibility from shipment status & permissions
  const showActivate =
    canActivate && shipment?.status === "CODE_PRINTED";
  const showRecall =
    canRecall && shipment?.status !== "RECALLED";
  const showDeleteDraft =
    shipment?.status === "DRAFT" || shipment?.status === "CODE_PRINTED";

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) {
          onClose();
        }
      }}
    >
      <DialogContent className="flex max-h-[90vh] w-[95vw] max-w-6xl flex-col overflow-hidden">
        <DialogHeader>
          <DialogTitle>Chi tiết lô hàng</DialogTitle>
          <DialogDescription>
            Thông tin chi tiết và mã QR của lô hàng.
          </DialogDescription>
        </DialogHeader>

        {/* No data */}
        {!shipment && open && (
          <div className="py-10 text-center text-muted-foreground">
            Không có dữ liệu lô hàng.
          </div>
        )}

        {shipment && (
          <div className="flex flex-1 flex-col overflow-hidden">
            {/* Header: Name + Status */}
            <div className="flex items-start gap-4 rounded-lg border bg-muted/30 p-4">
              <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Package className="size-6" />
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-lg font-semibold text-foreground">
                    {shipment.name}
                  </h3>

                  <ShipmentStatusBadge status={shipment.status} />
                </div>

                <p className="mt-1 break-all text-sm text-muted-foreground">
                  ID: {maskId(shipment.id)}
                </p>
              </div>
            </div>

            {/* Tabs */}
            <Tabs defaultValue="info" className="mt-4 flex flex-1 flex-col overflow-hidden">
              <TabsList className="w-fit">
                <TabsTrigger value="info">Thông tin</TabsTrigger>
                <TabsTrigger value="qr">
                  Mã QR ({shipment.traceCodes?.length || 0})
                </TabsTrigger>
              </TabsList>

              {/* Tab: Thông tin */}
              <TabsContent
                value="info"
                className="flex-1 overflow-y-auto mt-3 pr-1"
              >
                <div className="space-y-4">
                  {/* Shipment information */}
                  <DetailSection
                    title="Thông tin lô hàng"
                    contentClassName="grid gap-3 sm:grid-cols-2"
                  >
                    <DetailField
                      label="Lô sản xuất"
                      value={shipment.productionLotName || undefined}
                    />
                    <DetailField
                      label="ID lô sản xuất"
                      mono
                      value={maskId(shipment.productionLotId)}
                    />
                    <DetailField
                      label="Số lượng"
                      value={shipment.totalQuantity.toLocaleString("vi-VN")}
                    />
                    <DetailField
                      label="Quy cách đóng gói"
                      value={
                        shipment.packagingInfo ? (
                          <span className="block whitespace-pre-wrap font-normal">
                            {shipment.packagingInfo}
                          </span>
                        ) : undefined
                      }
                    />
                    <DetailField
                      label="Số mã truy xuất"
                      value={shipment.traceCodes.length.toLocaleString("vi-VN")}
                    />
                  </DetailSection>

                  {/* Recording metadata */}
                  <DetailSection title="Thông tin ghi nhận">
                    <div className="grid gap-3 sm:grid-cols-2">
                      <DetailField
                        label="Người tạo"
                        value={shipment.createdByName || undefined}
                      />
                      <DetailField
                        label="Ngày tạo"
                        value={formatDateTime(shipment.createdAt)}
                      />
                    </div>
                  </DetailSection>
                </div>
              </TabsContent>

              {/* Tab: Mã QR */}
              <TabsContent
                value="qr"
                className="flex-1 overflow-y-auto mt-3 pr-1"
              >
                <div className="mb-3 flex items-center justify-between text-sm text-muted-foreground">
                  <span>
                    Tổng số mã: {shipment.traceCodes?.length || 0}
                  </span>
                </div>

                <QrCodeGrid traceCodes={shipment.traceCodes || []} />
              </TabsContent>
            </Tabs>

            {/* Footer: Action buttons */}
            <div className="mt-4 flex flex-wrap items-center gap-2 border-t pt-4">
              {/* Sự kiện — always visible */}
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  onViewTimeline(shipment);
                  onClose();
                }}
              >
                <History className="mr-1.5 size-3.5" />
                Sự kiện
              </Button>

              {/* Xuất hồ sơ — always visible */}
              <Button
                size="sm"
                variant="outline"
                onClick={() => onExportDossier(shipment)}
              >
                <FileText className="mr-1.5 size-3.5" />
                Xuất hồ sơ
              </Button>

              {/* Kích hoạt — conditional */}
              {showActivate && (
                <Button
                  size="sm"
                  variant="edit"
                  onClick={() => {
                    onActivate(shipment);
                    onClose();
                  }}
                >
                  <BadgeCheck className="mr-1.5 size-3.5" />
                  Kích hoạt
                </Button>
              )}

              {/* Spacer to push destructive actions to the right */}
              <div className="flex-1" />

              {/* Hủy nháp — conditional */}
              {showDeleteDraft && (
                <Button
                  size="sm"
                  variant="delete"
                  onClick={() => {
                    onDeleteDraft(shipment);
                    onClose();
                  }}
                >
                  <Trash2 className="mr-1.5 size-3.5" />
                  Hủy nháp
                </Button>
              )}

              {/* Thu hồi — conditional */}
              {showRecall && (
                <Button
                  size="sm"
                  variant="delete"
                  onClick={() => {
                    onRecall(shipment);
                    onClose();
                  }}
                >
                  <Ban className="mr-1.5 size-3.5" />
                  Thu hồi
                </Button>
              )}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};