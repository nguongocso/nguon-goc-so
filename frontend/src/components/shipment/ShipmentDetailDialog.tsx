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
import { QrCodeGrid } from "./QrCodeGrid";

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

const statusLabelMap: Record<Shipment["status"], string> = {
  DRAFT: "Nháp",
  CODE_PRINTED: "Đã in mã",
  ACTIVATED: "Đã kích hoạt",
  RECALLED: "Đã thu hồi",
};

const statusClassMap: Record<Shipment["status"], string> = {
  DRAFT: "bg-status-draft/10 text-status-draft",
  CODE_PRINTED: "bg-status-packaged/10 text-status-packaged",
  ACTIVATED: "bg-status-approved/10 text-status-approved",
  RECALLED: "bg-status-rejected/10 text-status-rejected",
};

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

                  <span
                    className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                      statusClassMap[shipment.status]
                    }`}
                  >
                    {statusLabelMap[shipment.status]}
                  </span>
                </div>

                <p className="mt-1 break-all text-sm text-muted-foreground">
                  ID: {shipment.id}
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
                <dl className="divide-y rounded-lg border">
                  <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                    <dt className="text-sm text-muted-foreground">
                      Lô sản xuất
                    </dt>
                    <dd className="text-sm font-medium">
                      {shipment.productionLotName || "—"}
                    </dd>
                  </div>

                  <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                    <dt className="text-sm text-muted-foreground">
                      ID lô sản xuất
                    </dt>
                    <dd className="break-all text-sm font-medium">
                      {shipment.productionLotId}
                    </dd>
                  </div>

                  <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                    <dt className="text-sm text-muted-foreground">Số lượng</dt>
                    <dd className="text-sm font-medium">
                      {shipment.totalQuantity.toLocaleString("vi-VN")}
                    </dd>
                  </div>

                  <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                    <dt className="text-sm text-muted-foreground">
                      Quy cách đóng gói
                    </dt>
                    <dd className="whitespace-pre-wrap text-sm font-medium">
                      {shipment.packagingInfo || "—"}
                    </dd>
                  </div>

                  <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                    <dt className="text-sm text-muted-foreground">
                      Số mã truy xuất
                    </dt>
                    <dd className="text-sm font-medium">
                      {shipment.traceCodes.length.toLocaleString("vi-VN")}
                    </dd>
                  </div>

                  <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                    <dt className="text-sm text-muted-foreground">
                      Người tạo
                    </dt>
                    <dd className="text-sm font-medium">
                      {shipment.createdByName || "—"}
                    </dd>
                  </div>

                  <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                    <dt className="text-sm text-muted-foreground">Ngày tạo</dt>
                    <dd className="text-sm font-medium">
                      {formatDateTime(shipment.createdAt)}
                    </dd>
                  </div>
                </dl>
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