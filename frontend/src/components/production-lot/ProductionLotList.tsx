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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import type { ProductionLot } from "@/types/productionLot";
import {
  ClipboardCheck,
  FileUp,
  LoaderCircle,
  NotebookPen,
  PackageOpen,
  Pencil,
  Plus,
  Search,
  Send,
  ShoppingCart,
  Sprout,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { DataTablePagination } from "@/components/common/DataTablePagination";
import { ApproveProductionLotDialog } from "./Approveproductionlotdialog";
import { useAuth } from "@/hooks/useAuth";

interface ProductionLotListProps {
  lots: ProductionLot[];
  isLoading: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canSubmitForApproval: boolean;
  canApprove: boolean;
  canRecordFarmLog: boolean;
  onCreate: () => void;
  onEdit: (id: string) => void;
  onSubmitForApproval: (id: string) => Promise<void>;
  onDecideApproval: (
    id: string,
    approved: boolean,
    reason?: string,
  ) => Promise<void>;
  onRecordFarmLog: (id: string) => void;
  onRecordProcurement?: (lotId: string) => void;
}

const PAGE_SIZE = 10;

const statusConfig: Record<
  ProductionLot["status"],
  { label: string; className: string }
> = {
  DRAFT: {
    label: "Bản nháp",
    className: "bg-gray-100 text-gray-700 border-gray-300",
  },
  PENDING: {
    label: "Chờ duyệt",
    className: "bg-yellow-100 text-yellow-800 border-yellow-300",
  },
  APPROVED: {
    label: "Đã duyệt",
    className: "bg-emerald-100 text-emerald-800 border-emerald-300",
  },
  REJECTED: {
    label: "Bị từ chối",
    className: "bg-red-100 text-red-800 border-red-300",
  },
  HARVESTED: {
    label: "Đã thu hoạch",
    className: "bg-lime-100 text-lime-800 border-lime-300",
  },
  PREPROCESSED: {
    label: "Đã sơ chế",
    className: "bg-teal-100 text-teal-800 border-teal-300",
  },
  PACKAGED: {
    label: "Đã đóng gói",
    className: "bg-sky-100 text-sky-800 border-sky-300",
  },
  CLOSED: {
    label: "Đã kết thúc",
    className: "bg-purple-100 text-purple-800 border-purple-300",
  },
  RECALLED: {
    label: "Đã thu hồi",
    className: "bg-red-100 text-red-800 border-red-300",
  },
};

export const ProductionLotList = ({
  lots,
  isLoading,
  canCreate,
  canEdit,
  canSubmitForApproval,
  canApprove,
  canRecordFarmLog,
  onCreate,
  onEdit,
  onSubmitForApproval,
  onDecideApproval,
  onRecordFarmLog,
  onRecordProcurement,
}: ProductionLotListProps) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const [confirmingLot, setConfirmingLot] =
    useState<ProductionLot | null>(null);
  const [approvingLot, setApprovingLot] =
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
      const matchesStatus =
        statusFilter === "ALL" || lot.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [lots, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredLots.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedLots = filteredLots.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const getStatusBadge = (status: ProductionLot["status"]) => {
    const config = statusConfig[status] || {
      label: status || "Không xác định",
      className: "bg-gray-100 text-gray-800 border-gray-300",
    };
    return (
      <Badge
        variant="outline"
        className={`${config.className} border text-xs font-semibold px-2.5 py-0.5`}
      >
        {config.label}
      </Badge>
    );
  };

  return (
    <>
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100">
                <Sprout className="h-5 w-5 text-emerald-700" />
              </div>
              <div>
                <CardTitle className="text-xl font-bold text-slate-900">
                  Danh sách lô sản xuất
                </CardTitle>
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
        </CardHeader>

        <CardContent className="p-4">
          {/* Bộ lọc */}
          <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_220px]">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="pl-9"
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                placeholder="Tìm tên lô, vùng trồng hoặc loại nông sản..."
                aria-label="Tìm kiếm lô sản xuất"
              />
            </div>

            <Select
              value={statusFilter}
              onValueChange={(value) => {
                setStatusFilter(value || "");
                setPage(0);
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Tất cả trạng thái">
                  {statusFilter === "ALL"
                    ? "Tất cả trạng thái"
                    : statusConfig[statusFilter as ProductionLot["status"]]?.label || statusFilter}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
                {Object.entries(statusConfig).map(([value, config]) => (
                  <SelectItem key={value} value={value}>
                    {config.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Bảng */}
          <div className="overflow-x-auto rounded-lg border border-slate-200">
            <Table>
              <TableHeader>
                <TableRow className="bg-slate-50/80">
                  {[
                    "Tên lô",
                    "Vùng trồng",
                    "Nông sản",
                    "Trạng thái",
                    "Thao tác",
                    "Chi tiết",
                  ].map((title) => (
                    <TableHead
                      key={title}
                      className={`font-semibold text-slate-700${
                        title === "Thao tác" || title === "Chi tiết"
                          ? " text-center"
                          : ""
                      }`}
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
                      colSpan={6}
                      className="py-12 text-center text-muted-foreground"
                    >
                      <Sprout className="mx-auto mb-2 h-6 w-6 animate-spin text-emerald-500" />
                      Đang tải danh sách lô sản xuất...
                    </TableCell>
                  </TableRow>
                )}

                {!isLoading &&
                  paginatedLots.map((lot) => {
                    const showEdit =
                      canEdit && lot.status === "DRAFT";
                    const showSubmit =
                      canSubmitForApproval && lot.status === "DRAFT";
                    const showApprove =
                      canApprove && lot.status === "PENDING";
                    const showRecordFarmLog =
                      canRecordFarmLog &&
                      (lot.status === "APPROVED" ||
                        lot.status === "HARVESTED");
                    const showRecordProcurement =
                      !!onRecordProcurement &&
                      lot.status === "PACKAGED";
                    const hasAction =
                      showEdit ||
                      showSubmit ||
                      showApprove ||
                      showRecordFarmLog ||
                      showRecordProcurement;

                    return (
                      <TableRow key={lot.id} className="hover:bg-slate-50/60">
                        <TableCell className="font-semibold text-slate-900">
                          {lot.name}
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {lot.farmAreaName ?? "—"}
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {lot.productCategoryName ?? "—"}
                        </TableCell>

                        <TableCell>
                          {getStatusBadge(lot.status)}
                        </TableCell>

                        <TableCell>
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
                                onClick={() =>
                                  onRecordProcurement?.(lot.id)
                                }
                              >
                                <ShoppingCart className="size-4 text-blue-600" />
                              </Button>
                            )}
                            {!hasAction && (
                              <span className="text-xs text-muted-foreground">
                                —
                              </span>
                            )}
                          </div>
                        </TableCell>

                        <TableCell className="text-center">
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-8 text-xs"
                            onClick={() =>
                              navigate(`/production-lots/${lot.id}`)
                            }
                          >
                            Chi tiết
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
              </TableBody>
            </Table>

            {!isLoading && !filteredLots.length && (
              <div className="grid place-items-center px-4 py-16 text-center">
                <PackageOpen className="mb-3 size-10 text-slate-300" />
                <p className="font-semibold text-slate-900">
                  Không tìm thấy lô sản xuất
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Hãy thử thay đổi từ khóa hoặc bộ lọc trạng thái.
                </p>
              </div>
            )}
          </div>

          {!isLoading && filteredLots.length > 0 && (
            <DataTablePagination
              page={safePage}
              pageSize={PAGE_SIZE}
              totalElements={filteredLots.length}
              onPageChange={setPage}
              itemLabel="lô sản xuất"
            />
          )}
        </CardContent>
      </Card>

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
              {isSubmitting && (
                <LoaderCircle className="size-4 animate-spin" />
              )}
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
    </>
  );
};
