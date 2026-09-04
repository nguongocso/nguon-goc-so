import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import type {
  CancelProductionLotRequest,
  ProductionLot,
} from "@/types/productionLot";
import {
  Ban,
  ClipboardCheck,
  FileUp,
  LoaderCircle,
  NotebookPen,
  Pencil,
  Plus,
  Send,
  ShoppingCart,
  Sprout,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTableShell } from "@/components/common/DataTableShell";
import { FilterSelect } from "@/components/common/FilterSelect";
import { ListCard } from "@/components/common/ListCard";
import { ListToolbar } from "@/components/common/ListToolbar";
import { Pagination } from "@/components/common/Pagination";
import { RefreshButton } from "@/components/common/RefreshButton";
import { SearchInput } from "@/components/common/SearchInput";
import { StatusBadge, type StatusTone } from "@/components/common/StatusBadge";
import { useAuth } from "@/hooks/useAuth";
import { ApproveProductionLotDialog } from "./Approveproductionlotdialog";
import {
  CANCELLABLE_PRODUCTION_LOT_STATUSES,
  CancelProductionLotDialog,
} from "./CancelProductionLotDialog";

interface ProductionLotListProps {
  lots: ProductionLot[];
  isLoading: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canSubmitForApproval: boolean;
  canApprove: boolean;
  canRecordFarmLog: boolean;
  /** NCL-02-CN-006: cho phép hủy lô (VT-02). */
  canCancel: boolean;
  onCreate: () => void;
  onEdit: (id: string) => void;
  onSubmitForApproval: (id: string) => Promise<void>;
  onDecideApproval: (
    id: string,
    approved: boolean,
    reason?: string,
  ) => Promise<void>;
  onRecordFarmLog: (id: string) => void;
  /** NCL-02-CN-006: gọi API hủy lô kèm lý do + diễn giải. */
  onCancel: (id: string, payload: CancelProductionLotRequest) => Promise<void>;
  onRecordProcurement?: (lotId: string) => void;
  /** Ẩn khối tiêu đề trong card (dùng khi trang đã có ListPageHeader riêng). */
  hideCardHeader?: boolean;
  /** Callback làm mới dữ liệu — có thì hiển thị nút "Làm mới". */
  onRefresh?: () => void;
  /** Đang làm mới (spinner trên nút Làm mới). */
  isRefreshing?: boolean;
}

const PAGE_SIZE = 10;

const STATUS_LABELS: Record<ProductionLot["status"], string> = {
  DRAFT: "Bản nháp",
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Bị từ chối",
  HARVESTED: "Đã thu hoạch",
  PREPROCESSED: "Đã sơ chế",
  PACKAGED: "Đã đóng gói",
  CLOSED: "Đã kết thúc",
  RECALLED: "Đã thu hồi",
  CANCELLED: "Đã hủy",
};

const STATUS_TONES: Record<ProductionLot["status"], StatusTone> = {
  DRAFT: "neutral",
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "danger",
  HARVESTED: "success",
  PREPROCESSED: "info",
  PACKAGED: "info",
  CLOSED: "neutral",
  RECALLED: "danger",
  CANCELLED: "danger",
};

// NCL-02-CN-006: mặc định "Đang canh tác" = mọi trạng thái trừ "Đã hủy",
// để lô đã hủy không nằm mãi trong danh sách đang canh tác.
const STATUS_FILTER_OPTIONS = [
  { value: "ACTIVE", label: "Đang canh tác" },
  { value: "ALL", label: "Tất cả trạng thái" },
  ...Object.entries(STATUS_LABELS).map(([value, label]) => ({
    value,
    label,
  })),
];

export const ProductionLotList = ({
  lots,
  isLoading,
  canCreate,
  canEdit,
  canSubmitForApproval,
  canApprove,
  canRecordFarmLog,
  canCancel,
  onCreate,
  onEdit,
  onSubmitForApproval,
  onDecideApproval,
  onRecordFarmLog,
  onCancel,
  onRecordProcurement,
  hideCardHeader = false,
  onRefresh,
  isRefreshing = false,
}: ProductionLotListProps) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [search, setSearch] = useState("");
  // NCL-02-CN-006: mặc định chỉ hiển thị các lô đang canh tác (ẩn lô đã hủy)
  const [statusFilter, setStatusFilter] = useState("ACTIVE");
  const [page, setPage] = useState(0);
  const [confirmingLot, setConfirmingLot] =
    useState<ProductionLot | null>(null);
  const [approvingLot, setApprovingLot] =
    useState<ProductionLot | null>(null);
  const [cancellingLot, setCancellingLot] =
    useState<ProductionLot | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const canImport = user?.roleCode === "VT-02"; // quyền nhập lô hàng loạt

  const handleConfirmSubmit = async () => {
    if (!confirmingLot) return;
    setIsSubmitting(true);
    try {
      await onSubmitForApproval(confirmingLot.id);
      setConfirmingLot(null);
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredLots = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return lots.filter((lot) => {
      const matchesSearch =
        !keyword ||
        [
          lot.name,
          lot.farmAreaName ?? "",
          lot.productCategoryName ?? "",
        ].some((value) => value.toLowerCase().includes(keyword));
      // NCL-02-CN-006: "Đang canh tác" loại bỏ các lô đã hủy.
      const matchesStatus =
        statusFilter === "ALL" ||
        (statusFilter === "ACTIVE"
          ? lot.status !== "CANCELLED"
          : lot.status === statusFilter);
      return matchesSearch && matchesStatus;
    });
  }, [lots, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredLots.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedLots = filteredLots.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const hasActiveFilter =
    search.trim() !== "" ||
    (statusFilter !== "ALL" && statusFilter !== "ACTIVE");

  const renderStatus = (status: ProductionLot["status"]) => {
    const label = STATUS_LABELS[status];
    if (!label) {
      return <StatusBadge label={status || "Không xác định"} tone="neutral" />;
    }
    return <StatusBadge label={label} tone={STATUS_TONES[status]} />;
  };

  return (
    <>
      <ListCard>
        {/* Tiêu đề card — ẩn trên trang /production-lots vì trang đã có ListPageHeader */}
        {!hideCardHeader && (
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                <Sprout className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-xl font-bold tracking-tight text-foreground">
                  Danh sách lô sản xuất
                </h2>
                <p className="text-sm text-muted-foreground">
                  Theo dõi lô theo vùng trồng, nông sản và trạng thái
                </p>
              </div>
            </div>
            <div className="flex gap-2">
              {canCreate && (
                <Button type="button" variant="create" onClick={onCreate}>
                  <Plus className="size-4" />
                  Tạo lô sản xuất
                </Button>
              )}
              {canImport && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate("/production-lots/import")}
                >
                  <FileUp className="size-4" />
                  Nhập lô hàng loạt
                </Button>
              )}
            </div>
          </div>
        )}

        {/* Bộ lọc */}
        <ListToolbar
          left={
            <>
              <SearchInput
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                placeholder="Tìm tên lô, vùng trồng hoặc loại nông sản..."
                aria-label="Tìm kiếm lô sản xuất"
              />
              <FilterSelect
                value={statusFilter}
                onValueChange={(value) => {
                  setStatusFilter(value ?? "ACTIVE");
                  setPage(0);
                }}
                options={STATUS_FILTER_OPTIONS}
              />
            </>
          }
          right={
            onRefresh ? (
              <RefreshButton onClick={onRefresh} loading={isRefreshing} />
            ) : undefined
          }
        />

        {/* Bảng */}
        <DataTableShell
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Tên lô</TableHead>
              <TableHead>Vùng trồng</TableHead>
              <TableHead>Nông sản</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
              <TableHead className="text-center">Chi tiết</TableHead>
            </>
          }
          body={paginatedLots.map((lot, index) => {
            const showEdit = canEdit && lot.status === "DRAFT";
            const showSubmit = canSubmitForApproval && lot.status === "DRAFT";
            const showApprove = canApprove && lot.status === "PENDING";
            const showRecordFarmLog =
              canRecordFarmLog &&
              (lot.status === "APPROVED" || lot.status === "HARVESTED");
            const showRecordProcurement =
              !!onRecordProcurement && lot.status === "PACKAGED";
            const showCancel =
              canCancel &&
              CANCELLABLE_PRODUCTION_LOT_STATUSES.includes(lot.status);
            const hasAction =
              showEdit ||
              showSubmit ||
              showApprove ||
              showRecordFarmLog ||
              showRecordProcurement ||
              showCancel;

            return (
              <TableRow
                key={lot.id}
                className="hover:bg-muted/40 transition-colors"
              >
                <TableCell className="text-center font-medium text-muted-foreground">
                  {safePage * PAGE_SIZE + index + 1}
                </TableCell>
                <TableCell
                  className="max-w-[240px] truncate font-semibold text-slate-900"
                  title={lot.name}
                >
                  {lot.name}
                </TableCell>
                <TableCell
                  className="max-w-[200px] truncate text-muted-foreground"
                  title={lot.farmAreaName ?? undefined}
                >
                  {lot.farmAreaName ?? "—"}
                </TableCell>
                <TableCell
                  className="max-w-[200px] truncate text-muted-foreground"
                  title={lot.productCategoryName ?? undefined}
                >
                  {lot.productCategoryName ?? "—"}
                </TableCell>
                <TableCell>{renderStatus(lot.status)}</TableCell>
                <TableCell className="text-center">
                  <div className="flex justify-center gap-1">
                    {showEdit && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        title="Chỉnh sửa"
                        onClick={() => onEdit(lot.id)}
                      >
                        <Pencil className="size-4" />
                      </Button>
                    )}
                    {showSubmit && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        title="Gửi duyệt"
                        onClick={() => setConfirmingLot(lot)}
                      >
                        <Send className="size-4" />
                      </Button>
                    )}
                    {showApprove && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        title="Duyệt lô"
                        onClick={() => setApprovingLot(lot)}
                      >
                        <ClipboardCheck className="size-4 text-amber-600" />
                      </Button>
                    )}
                    {showRecordFarmLog && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        title="Ghi nhật ký canh tác"
                        onClick={() => onRecordFarmLog(lot.id)}
                      >
                        <NotebookPen className="size-4 text-emerald-600" />
                      </Button>
                    )}
                    {showRecordProcurement && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        title="Ghi nhận thu mua"
                        onClick={() => onRecordProcurement?.(lot.id)}
                      >
                        <ShoppingCart className="size-4 text-blue-600" />
                      </Button>
                    )}
                    {showCancel && (
                      <Button
                        size="icon-sm"
                        variant="ghost"
                        title="Hủy lô"
                        onClick={() => setCancellingLot(lot)}
                      >
                        <Ban className="size-4 text-red-600" />
                      </Button>
                    )}
                    {!hasAction && (
                      <span className="text-xs text-muted-foreground">—</span>
                    )}
                  </div>
                </TableCell>
                <TableCell className="text-center">
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-8 text-xs"
                    onClick={() => navigate(`/production-lots/${lot.id}`)}
                  >
                    Chi tiết
                  </Button>
                </TableCell>
              </TableRow>
            );
          })}
          loading={isLoading}
          empty={!isLoading && filteredLots.length === 0}
          colSpan={7}
          loadingMessage="Đang tải danh sách lô sản xuất..."
          emptyMessage={
            hasActiveFilter
              ? "Không tìm thấy lô sản xuất phù hợp với bộ lọc."
              : "Chưa có lô sản xuất nào."
          }
        />

        {/* Phân trang */}
        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filteredLots.length}
          pageSize={PAGE_SIZE}
          loading={isLoading}
          itemLabel="lô sản xuất"
          onPageChange={setPage}
        />
      </ListCard>

      {/* Dialog xác nhận gửi duyệt */}
      <AlertDialog
        open={confirmingLot !== null}
        onOpenChange={(open) => {
          if (!open && !isSubmitting) setConfirmingLot(null);
        }}
      >
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle>Gửi duyệt lô sản xuất</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn sắp gửi duyệt lô{" "}
              <span className="font-semibold text-foreground">
                {confirmingLot?.name}
              </span>
              . Tiếp tục?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isSubmitting}>Hủy</AlertDialogCancel>
            <Button
              type="button"
              disabled={isSubmitting}
              onClick={() => void handleConfirmSubmit()}
            >
              {isSubmitting && <LoaderCircle className="size-4 animate-spin" />}
              Xác nhận
            </Button>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>

      <ApproveProductionLotDialog
        open={approvingLot !== null}
        lot={approvingLot}
        onClose={() => setApprovingLot(null)}
        onDecide={onDecideApproval}
      />

      <CancelProductionLotDialog
        open={cancellingLot !== null}
        lot={cancellingLot}
        onClose={() => setCancellingLot(null)}
        onCancel={onCancel}
      />
    </>
  );
};