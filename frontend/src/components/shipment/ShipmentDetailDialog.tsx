import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { getShipmentById } from "@/api/shipmentApi";
import type { Shipment } from "@/types/shipment";
import { AlertCircle, LoaderCircle, Package } from "lucide-react";

interface ShipmentDetailDialogProps {
  open: boolean;
  shipmentId: string | null;
  onClose: () => void;
}

interface ApiError {
  response?: {
    data?: {
      message?: string;
    };
  };
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
  shipmentId,
  onClose,
}: ShipmentDetailDialogProps) => {
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !shipmentId) {
      setShipment(null);
      setError(null);
      setIsLoading(false);
      return;
    }

    let isMounted = true;

    const loadShipmentDetail = async () => {
      setIsLoading(true);
      setError(null);
      setShipment(null);

      try {
        const data = await getShipmentById(shipmentId);

        if (isMounted) {
          setShipment(data);
        }
      } catch (requestError: unknown) {
        if (!isMounted) {
          return;
        }

        const apiError = requestError as ApiError;

        setError(
          apiError.response?.data?.message ||
            "Không thể tải thông tin chi tiết lô hàng.",
        );
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    void loadShipmentDetail();

    return () => {
      isMounted = false;
    };
  }, [open, shipmentId]);

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) {
          onClose();
        }
      }}
    >
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Chi tiết lô hàng</DialogTitle>
          <DialogDescription>
            Thông tin chi tiết của lô hàng được lấy trực tiếp từ hệ thống.
          </DialogDescription>
        </DialogHeader>

        {isLoading && (
          <div className="flex min-h-48 flex-col items-center justify-center gap-3 text-muted-foreground">
            <LoaderCircle className="size-8 animate-spin" />
            <p>Đang tải thông tin lô hàng...</p>
          </div>
        )}

        {!isLoading && error && (
          <div className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
            <AlertCircle className="mt-0.5 size-5 shrink-0" />

            <div>
              <p className="font-semibold">Không thể tải dữ liệu</p>
              <p className="mt-1 text-sm">{error}</p>
            </div>
          </div>
        )}

        {!isLoading && !error && shipment && (
          <div className="space-y-5">
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

            <dl className="divide-y rounded-lg border">
              <div className="grid gap-1 px-4 py-3 sm:grid-cols-[180px_1fr] sm:gap-4">
                <dt className="text-sm text-muted-foreground">Lô sản xuất</dt>
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
                <dt className="text-sm text-muted-foreground">Người tạo</dt>
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
          </div>
        )}

        {!isLoading && !error && !shipment && open && (
          <div className="py-10 text-center text-muted-foreground">
            Không có dữ liệu lô hàng.
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};