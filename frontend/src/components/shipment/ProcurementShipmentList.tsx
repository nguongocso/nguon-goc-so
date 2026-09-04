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
import type { ProcurementShipment } from "@/types/shipment";
import { getEligibleShipments, getShipmentById } from "@/api/shipmentApi";
import { exportGs1Dossier } from "@/api/dossierApi";
import { ShipmentStatusBadge } from "@/components/shipment/ShipmentStatusBadge";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { usePermission } from "@/hooks/usePermission";
import { useNavigate } from "react-router-dom";
import {
  Eye,
  FileJson,
  LoaderCircle,
  Search,
  ShoppingCart,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { DataTablePagination } from "@/components/common/DataTablePagination";

const PAGE_SIZE = 10;

interface ProcurementShipmentListProps {
  /** Callback khi người dùng bấm "Ghi nhận thu mua" trên một lô hàng */
  onRecordProcurement: (shipmentId: string) => void;
}

export function ProcurementShipmentList({
                                          onRecordProcurement,
                                        }: ProcurementShipmentListProps) {
  const [shipments, setShipments] = useState<ProcurementShipment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const navigate = useNavigate();

  // Điều hướng tới trang chi tiết lô hàng. ProcurementShipment không chứa
  // productionLotId nên cần lấy chi tiết trước để xây dựng route đầy đủ
  // /production-lots/:lotId/shipments/:shipmentId (back button hoạt động).
  const handleViewDetail = async (shipmentId: string) => {
    try {
      const data = await getShipmentById(shipmentId);

      if (!data.productionLotId) {
        toast.error("Không thể xác định lô sản xuất của lô hàng này.");
        return;
      }

      navigate(
        `/production-lots/${data.productionLotId}/shipments/${shipmentId}`,
      );
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
    if (!keyword) return shipments;
    return shipments.filter(
        (shipment) =>
            shipment.name.toLowerCase().includes(keyword) ||
            (shipment.productionLotName ?? "").toLowerCase().includes(keyword) ||
            (shipment.productCategoryName ?? "").toLowerCase().includes(keyword),
    );
  }, [shipments, search]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedShipments = filtered.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

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
          error.message ||
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
                    onChange={(event) => {
                      setSearch(event.target.value);
                      setPage(0);
                    }}
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
                      "Chi tiết",
                    ].map((title) => (
                        <TableHead
                            key={title}
                            className={
                              title === "Thao tác" || title === "Chi tiết"
                                  ? "text-center"
                                  : undefined
                            }
                        >
                          {title}
                        </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {isLoading && (
                      <TableRow>
                        <TableCell
                            colSpan={7}
                            className="py-12 text-center text-muted-foreground"
                        >
                          <LoaderCircle className="mx-auto mb-2 size-5 animate-spin" />
                          Đang tải danh sách lô hàng...
                        </TableCell>
                      </TableRow>
                  )}

                  {!isLoading &&
                      paginatedShipments.map((shipment) => (
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
                              <ShipmentStatusBadge status={shipment.status} />
                            </TableCell>

                            <TableCell>
                              <div className="flex flex-wrap items-center gap-2">
                                <Button
                                    size="sm"
                                    type="button"
                                    variant="default"
                                    title="Ghi nhận thu mua"
                                    onClick={() => onRecordProcurement(shipment.id)}
                                >
                                  <ShoppingCart className="mr-1 size-4" />
                                  Thu mua
                                </Button>

                                {canExportGs1 && (
                                    <Button
                                        size="sm"
                                        type="button"
                                        variant="outline"
                                        title="Xuất hồ sơ GS1"
                                        onClick={() => handleExportGs1(shipment.id)}
                                    >
                                      <FileJson className="mr-1 size-4" />
                                      Xuất GS1
                                    </Button>
                                )}
                              </div>
                            </TableCell>

                            <TableCell className="text-center">
                              <Button
                                  size="sm"
                                  type="button"
                                  variant="outline"
                                  onClick={() => handleViewDetail(shipment.id)}
                              >
                                <Eye className="mr-1 size-4" />
                                Chi tiết
                              </Button>
                            </TableCell>
                          </TableRow>
                      ))}

                  {!isLoading && filtered.length === 0 && (
                      <TableRow>
                        <TableCell
                            colSpan={7}
                            className="py-12 text-center text-muted-foreground"
                        >
                          <p>
                            {search.trim()
                                ? "Không tìm thấy lô hàng phù hợp."
                                : "Chưa có lô hàng nào sẵn sàng thu mua."}
                          </p>
                        </TableCell>
                      </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>
            {!isLoading && filtered.length > 0 && (
              <DataTablePagination
                page={safePage}
                pageSize={PAGE_SIZE}
                totalElements={filtered.length}
                onPageChange={setPage}
                itemLabel="lô hàng"
              />
            )}
          </CardContent>
        </Card>
      </>
  );
}