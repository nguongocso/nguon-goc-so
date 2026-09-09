import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Handshake, Eye } from "lucide-react";
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
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import {
  HANDOVER_STATUS_LABELS,
  HandoverStatusBadge,
} from "@/components/shipment/HandoverStatusBadge";
import type { HandoverDetailResponse } from "@/types/shipmentHandover";
import { getReceivedHandovers } from "@/api/handoverApi";

const PAGE_SIZE = 10;

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: "ALL", label: "Tất cả trạng thái" },
  ...Object.entries(HANDOVER_STATUS_LABELS).map(([value, label]) => ({
    value,
    label,
  })),
];

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
 * Danh sách phiếu bàn giao lô hàng gửi tới tổ chức hiện tại (NCL-05-CN-008/CN-009).
 *
 * Đây là lối vào chủ động cho tổ chức nhận (Doanh nghiệp thu mua VT-04): hiển
 * thị mọi phiếu nhận được bất kể lô hàng tương ứng nằm ở trang nào của danh
 * sách thu mua. Mỗi phiếu có nút "Xem chi tiết" dẫn tới HandoverDetailPage.
 */
export function ShipmentHandoverReceivedListPage() {
  const navigate = useNavigate();
  const [handovers, setHandovers] = useState<HandoverDetailResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Phiếu bàn giao nhận" },
  ]);

  const load = async () => {
    setIsLoading(true);
    try {
      const data = await getReceivedHandovers();
      setHandovers(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể tải danh sách phiếu bàn giao nhận.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // Hàm load ổn định theo module; không cần thêm dependency nào khác.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return handovers.filter(
      (handover) =>
        (statusFilter === "ALL" || handover.status === statusFilter) &&
        (!keyword ||
          handover.shipmentName.toLowerCase().includes(keyword) ||
          (handover.fromOrganizationName ?? "").toLowerCase().includes(keyword)),
    );
  }, [handovers, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = filtered.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Handshake}
        iconBoxClassName="bg-indigo-500/10"
        title="Phiếu bàn giao nhận"
        description="Xem các phiếu bàn giao lô hàng của tổ chức bạn là bên nhận; xác nhận hoặc từ chối tại trang chi tiết."
        actions={<HelpButton screenKey="dashboard" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên lô hàng hoặc tổ chức giao..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                aria-label="Tìm kiếm phiếu bàn giao nhận"
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
          colSpan={7}
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Tên lô hàng</TableHead>
              <TableHead>Tổ chức giao</TableHead>
              <TableHead className="text-right">Số lượng</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Ngày tạo</TableHead>
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
                <div className="text-xs text-muted-foreground">
                  {handover.shipmentId}
                </div>
              </TableCell>
              <TableCell className="text-muted-foreground">
                {handover.fromOrganizationName ?? "—"}
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
              <TableCell className="text-center">
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
              </TableCell>
            </TableRow>
          ))}
          loading={isLoading}
          empty={!isLoading && filtered.length === 0}
          loadingMessage="Đang tải danh sách phiếu bàn giao..."
          emptyMessage={
            search.trim() || statusFilter !== "ALL"
              ? "Không tìm thấy phiếu bàn giao phù hợp. Hãy thử thay đổi từ khóa tìm kiếm."
              : "Chưa có phiếu bàn giao nào gửi tới tổ chức của bạn."
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
    </div>
  );
}