import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { ProcurementShipment, Shipment } from "@/types/shipment";
import { getEligibleShipments, getShipmentById } from "@/api/shipmentApi";
import { exportGs1Dossier } from "@/api/dossierApi";
import { ShipmentDetailDialog } from "@/components/shipment/ShipmentDetailDialog";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { usePermission } from "@/hooks/usePermission";
import {
  Eye,
  FileJson,
  LoaderCircle,
  Package,
  Search,
  ShoppingCart,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";

interface ProcurementShipmentListProps {
  /** Callback khi người dùng bấm "Ghi nhận thu mua" trên một lô hàng */
  onRecordProcurement: (shipmentId: string) => void;
}

const statusLabels: Record<string, string> = {
  ACTIVATED: "Đã kích hoạt",
  CODE_PRINTED: "Đã in mã",
  DRAFT: "Bản nháp",
  RECALLED: "Đã thu hồi",
};

const statusClasses: Record<string, string> = {
  ACTIVATED: "bg-status-approved/10 text-status-approved",
  CODE_PRINTED: "bg-status-packaged/10 text-status-packaged",
  DRAFT: "bg-status-draft/10 text-status-draft",
  RECALLED: "bg-status-rejected/10 text-status-rejected",
};

export function ProcurementShipmentList({
  onRecordProcurement,
}: ProcurementShipmentListProps) {
  const [shipments, setShipments] = useState<ProcurementShipment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [detailShipment, setDetailShipment] = useState<Shipment | null>(null);

  const handleViewDetail = async (shipmentId: string) => {
    try {
      const data = await getShipmentById(shipmentId);
      setDetailShipment(data);
    } catch {
      toast.error("Không thể tải chi tiết lô hàng.");
    }
  };


  const loadShipments = useCallback(async () => {
    setIsLoading(true);

    try {
      const data = await getEligibleShipments();
      setShipments(data);
    } catch {
      toast.error("Không thể tải danh sách lô hàng thu mua.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadShipments();
  }, [loadShipments]);

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase();

    if (!keyword) {
      return shipments;
    }

    return shipments.filter(
      (shipment) =>
        shipment.name.toLowerCase().includes(keyword) ||
        (shipment.productionLotName ?? "")
          .toLowerCase()
          .includes(keyword) ||
        (shipment.productCategoryName ?? "")
          .toLowerCase()
          .includes(keyword),
    );
  }, [shipments, search]);

  const canExportGs1 = usePermission(ROLE_ACCESS.gs1DossierExport);

  const handleExportGs1 = async (shipmentId: string) => {
    const toastId = toast.loading("Đang tạo hồ sơ GS1...");
    try {
      const { blob, fileName } = await exportGs1Dossier(
        shipmentId,
        "json",
        true,
      );
      toast.dismiss(toastId);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      toast.success("Tải hồ sơ GS1 thành công");
    } catch (error: any) {
      toast.dismiss(toastId);
      const msg =
        error.response?.data?.message ||
        "Có lỗi xảy ra khi xuất hồ sơ GS1.";
      toast.error(msg);
    }
  };

  return (
    <>
      <Card className="overflow-hidden">
        <CardHeader className="border-b">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <div>
              <CardTitle>Danh sách lô hàng sẵn sàng thu mua</CardTitle>

              <p className="mt-1 text-sm text-muted-foreground">
                Chỉ hiển thị các lô hàng đã kích hoạt tem, sẵn sàng ghi nhận thu
                mua.
              </p>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          <div className="border-b bg-table-header p-4">
            <label className="relative">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />

              <Input
                className="bg-white pl-9"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Tìm tên lô hàng, lô sản xuất hoặc loại nông sản..."
                aria-label="Tìm kiếm lô hàng thu mua"
              />
            </label>
          </div>

          <div className="overflow-x-auto">
            <Table className="min-w-[600px] md:min-w-[750px]">
              <TableHeader>
                <TableRow>
                  {[
                    "Tên lô hàng",
                    "Lô sản xuất",
                    "Nông sản",
                    "Sản lượng",
                    "Trạng thái",
                    "Thao tác",
                  ].map((title) => (
                    <TableHead key={title}>{title}</TableHead>
                  ))}
                </TableRow>
              </TableHeader>

              <TableBody>
                {isLoading && (
                  <TableRow>
                    <TableCell
                      colSpan={6}
                      className="py-12 text-center text-muted-foreground"
                    >
                      <LoaderCircle className="mx-auto mb-2 size-5 animate-spin" />
                      Đang tải danh sách lô hàng...
                    </TableCell>
                  </TableRow>
                )}

                {!isLoading &&
                  filtered.map((shipment) => (
                    <TableRow key={shipment.id}>
                      <TableCell className="font-semibold text-foreground">
                        {shipment.name}
                      </TableCell>

                      <TableCell className="text-muted-foreground">
                        {shipment.productionLotName ?? "—"}
                      </TableCell>

                      <TableCell className="text-muted-foreground">
                        {shipment.productCategoryName ?? "—"}
                      </TableCell>

                      <TableCell className="text-muted-foreground">
                        {shipment.totalQuantity != null
                          ? shipment.totalQuantity.toLocaleString("vi-VN")
                          : "—"}
                      </TableCell>

                      <TableCell>
                        <span
                          className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusClasses[shipment.status] ??
                            "bg-status-draft/10 text-status-draft"
                            }`}
                        >
                          {statusLabels[shipment.status] ?? shipment.status}
                        </span>
                      </TableCell>

                      <TableCell>
                        <div className="flex flex-wrap items-center gap-2">
                          <Button
                            size="sm"
                            type="button"
                            variant="outline"
                            onClick={() =>
                              handleViewDetail(shipment.id)
                            }
                          >
                            <Eye className="size-4" />
                            Chi tiết
                          </Button>

                          <Button
                            size="sm"
                            type="button"
                            onClick={() =>
                              onRecordProcurement(shipment.id)
                            }
                          >
                            <ShoppingCart className="size-4" />
                            Ghi nhận thu mua
                          </Button>

                          {canExportGs1 && (
                            <Button
                              size="sm"
                              type="button"
                              variant="outline"
                              onClick={() => handleExportGs1(shipment.id)}
                            >
                              <FileJson className="size-4" />
                              Xuất hồ sơ GS1
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}

                {!isLoading && filtered.length === 0 && (
                  <TableRow>
                    <TableCell
                      colSpan={6}
                      className="py-12 text-center text-muted-foreground"
                    >
                      <Package className="mx-auto mb-3 size-10 text-muted-foreground/40" />

                      <p className="font-semibold">
                        {search.trim()
                          ? "Không tìm thấy lô hàng phù hợp"
                          : "Chưa có lô hàng nào sẵn sàng thu mua"}
                      </p>

                      <p className="mt-1 text-sm text-muted-foreground">
                        {search.trim()
                          ? "Hãy thử thay đổi từ khóa tìm kiếm."
                          : "Các lô hàng đã kích hoạt tem sẽ xuất hiện tại đây."}
                      </p>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      <ShipmentDetailDialog
        open={detailShipment !== null}
        shipment={detailShipment}
        onClose={() => setDetailShipment(null)}
        canActivate={false}
        canRecall={false}
        onActivate={() => { }}
        onRecall={() => { }}
        onExportDossier={() => { }}
        onDeleteDraft={() => { }}
        onViewTimeline={() => { }}
      />
    </>
  );
}
