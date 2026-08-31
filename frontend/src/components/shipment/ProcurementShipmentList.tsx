import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Eye, FileJson, ShoppingCart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { ListCard } from "@/components/common/ListCard";
import { ListToolbar } from "@/components/common/ListToolbar";
import { SearchInput } from "@/components/common/SearchInput";
import { RefreshButton } from "@/components/common/RefreshButton";
import { DataTableShell } from "@/components/common/DataTableShell";
import { Pagination } from "@/components/common/Pagination";
import { ShipmentStatusBadge } from "@/components/shipment/ShipmentStatusBadge";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { usePermission } from "@/hooks/usePermission";
import type { ProcurementShipment } from "@/types/shipment";
import { getEligibleShipments, getShipmentById } from "@/api/shipmentApi";
import { exportGs1Dossier } from "@/api/dossierApi";

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
    <ListCard>
      <ListToolbar
        left={
          <SearchInput
            placeholder="Tìm tên lô hàng, lô sản xuất hoặc loại nông sản..."
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            aria-label="Tìm kiếm lô hàng thu mua"
          />
        }
        right={<RefreshButton onClick={loadShipments} loading={isLoading} />}
      />

      <DataTableShell
        colSpan={8}
        header={
          <>
            <TableHead className="w-12 text-center">STT</TableHead>
            <TableHead>Tên lô hàng</TableHead>
            <TableHead>Lô sản xuất</TableHead>
            <TableHead>Nông sản</TableHead>
            <TableHead>Sản lượng</TableHead>
            <TableHead>Trạng thái</TableHead>
            <TableHead className="text-center">Thao tác</TableHead>
            <TableHead className="text-center">Chi tiết</TableHead>
          </>
        }
        body={paginatedShipments.map((shipment, index) => (
          <TableRow
            key={shipment.id}
            className="hover:bg-muted/40 transition-colors"
          >
            <TableCell className="text-center font-medium text-muted-foreground">
              {safePage * PAGE_SIZE + index + 1}
            </TableCell>
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
              <div className="flex flex-wrap items-center justify-center gap-2">
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
        loading={isLoading}
        empty={!isLoading && filtered.length === 0}
        loadingMessage="Đang tải danh sách lô hàng..."
        emptyMessage={
          search.trim()
            ? "Không tìm thấy lô hàng phù hợp. Hãy thử thay đổi từ khóa tìm kiếm."
            : "Chưa có lô hàng nào sẵn sàng thu mua. Các lô hàng đã kích hoạt tem sẽ xuất hiện tại đây."
        }
      />

      <Pagination
        currentPage={safePage}
        totalPages={totalPages}
        totalElements={filtered.length}
        pageSize={PAGE_SIZE}
        loading={isLoading}
        itemLabel="lô hàng"
        onPageChange={setPage}
      />
    </ListCard>
  );
}
