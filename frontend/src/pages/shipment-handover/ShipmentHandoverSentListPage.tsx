import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Truck, Eye, XCircle } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import { Button } from "@/components/ui/button";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { ListPageHeader } from "@/components/common/ListPageHeader";
import { ListCard } from "@/components/common/ListCard";
import { ListToolbar } from "@/components/common/ListToolbar";
import { SearchInput } from "@/components/common/SearchInput";
import { FilterSelect } from "@/components/common/FilterSelect";
import { RefreshButton } from "@/components/common/RefreshButton";
import { DataTableShell } from "@/components/common/DataTableShell";
import { Pagination } from "@/components/common/Pagination";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import {
  HANDOVER_STATUS_LABELS,
  HandoverStatusBadge,
} from "@/components/shipment/HandoverStatusBadge";
import type { HandoverDetailResponse } from "@/types/shipmentHandover";
import { getSentHandovers, cancelHandover } from "@/api/handoverApi";

const PAGE_SIZE = 10;

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: "ALL", label: "Tất cả trạng thái" },
  ...Object.entries(HANDOVER_STATUS_LABELS).map(([value, label]) => ({
    value,
    label,
  })),
];

/** Định dạng ngày giờ theo locale Việt Nam. */
const formatDateTime = (iso?: string | null): string => {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
};

/**
 * Danh sách phiếu bàn giao lô hàng do tổ chức hiện tại TẠO RA (gửi đi).
 *
 * Dành cho Hợp tác xã / Quản lý (VT-02) để theo dõi các phiếu đã gửi
 * tới doanh nghiệp thu mua. Hỗ trợ tìm kiếm, lọc trạng thái, xem chi tiết
 * và hủy phiếu đang chờ xác nhận.
 */
export function ShipmentHandoverSentListPage() {
  const navigate = useNavigate();
  const [handovers, setHandovers] = useState<HandoverDetailResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);

  // State cho dialog hủy phiếu
  const [cancelTarget, setCancelTarget] = useState<HandoverDetailResponse | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Phiếu bàn giao đã gửi" },
  ]);

  const load = async () => {
    setIsLoading(true);
    try {
      const data = await getSentHandovers();
      setHandovers(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể tải danh sách phiếu bàn giao đã gửi.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Lọc client-side theo từ khóa và trạng thái. */
  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return handovers.filter(
      (handover) =>
        (statusFilter === "ALL" || handover.status === statusFilter) &&
        (!keyword ||
          handover.shipmentName.toLowerCase().includes(keyword) ||
          (handover.toOrganizationName ?? "").toLowerCase().includes(keyword)),
    );
  }, [handovers, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = filtered.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  /** Xử lý hủy phiếu bàn giao đang chờ xác nhận. */
  const handleCancel = async () => {
    if (!cancelTarget) return;
    setIsCancelling(true);
    try {
      await cancelHandover(cancelTarget.id, "Hủy từ danh sách phiếu đã gửi");
      toast.success("Đã hủy phiếu bàn giao thành công.");
      setCancelTarget(null);
      void load();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể hủy phiếu bàn giao.",
      );
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Truck}
        iconBoxClassName="bg-blue-500/10"
        title="Phiếu bàn giao đã gửi"
        description="Xem các phiếu bàn giao lô hàng do tổ chức bạn tạo ra và gửi tới doanh nghiệp thu mua."
        actions={<HelpButton screenKey="dashboard" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên lô hàng hoặc doanh nghiệp nhận..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                aria-label="Tìm kiếm phiếu bàn giao đã gửi"
              />
              <FilterSelect
                value={statusFilter}
                onValueChange={(value) => {
                  setStatusFilter(value ?? "ALL");
                  setPage(0);
                }}
                options={STATUS_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={() => void load()} loading={isLoading} />}
        />

        <DataTableShell
          colSpan={8}
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Tên lô hàng</TableHead>
              <TableHead>Doanh nghiệp nhận</TableHead>
              <TableHead className="text-right">Số lượng</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead>Hạn xác nhận</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={paginated.map((handover, index) => (
            <TableRow
              key={handover.id}
              className="hover:bg-muted/40 transition-colors"
            >
              <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell>
                <div className="font-semibold text-foreground">
                  {handover.shipmentName}
                </div>
              </TableCell>
              <TableCell className="text-muted-foreground">
                {handover.toOrganizationName ?? "—"}
              </TableCell>
              <TableCell className="text-right text-muted-foreground">
                {handover.quantity.toLocaleString("vi-VN")} kg
              </TableCell>
              <TableCell>
                <HandoverStatusBadge status={handover.status} />
              </TableCell>
              <TableCell className="text-muted-foreground">
                {formatDateTime(handover.createdAt)}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {formatDateTime(handover.expiresAt)}
              </TableCell>
              <TableCell className="text-center">
                <div className="flex items-center justify-center gap-1.5">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      navigate(`/shipment-handovers/${handover.id}`)
                    }
                  >
                    <Eye className="mr-1.5 h-4 w-4" />
                    Xem chi tiết
                  </Button>
                  {/* Chỉ hiện nút Hủy khi phiếu đang chờ xác nhận */}
                  {handover.status === "PENDING_CONFIRMATION" && (
                    <Button
                      type="button"
                      variant="destructive"
                      size="sm"
                      onClick={() => setCancelTarget(handover)}
                    >
                      <XCircle className="mr-1.5 h-4 w-4" />
                      Hủy
                    </Button>
                  )}
                </div>
              </TableCell>
            </TableRow>
          ))}
          loading={isLoading}
          empty={!isLoading && filtered.length === 0}
          loadingMessage="Đang tải danh sách phiếu bàn giao..."
          emptyMessage={
            search.trim() || statusFilter !== "ALL"
              ? "Không tìm thấy phiếu bàn giao phù hợp. Hãy thử thay đổi từ khóa tìm kiếm."
              : "Chưa có phiếu bàn giao nào được tạo bởi tổ chức của bạn."
          }
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={isLoading}
          itemLabel="phiếu bàn giao"
          onPageChange={setPage}
        />
      </ListCard>

      {/* Dialog xác nhận hủy phiếu */}
      <ConfirmDialog
        open={cancelTarget !== null}
        onOpenChange={(isOpen) => !isOpen && setCancelTarget(null)}
        title="Hủy phiếu bàn giao"
        description={`Bạn có chắc chắn muốn hủy phiếu bàn giao lô hàng "${cancelTarget?.shipmentName}" gửi tới ${cancelTarget?.toOrganizationName}? Hành động này không thể hoàn tác.`}
        confirmLabel="Hủy phiếu"
        cancelLabel="Quay lại"
        variant="destructive"
        onConfirm={() => void handleCancel()}
        loading={isCancelling}
      />
    </div>
  );
}