import { useEffect, useState } from "react";
import { toast } from "sonner";
import {
  Eye,
  EyeOff,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { HelpButton } from "@/components/help/HelpButton";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import {
  deleteInspectionCriterion,
  disableInspectionCriterion,
  enableInspectionCriterion,
  getInspectionCriteria,
} from "@/api/inspectionCriterionApi";
import type {
  InspectionCriterion,
  InspectionCriterionStatus,
} from "@/types/inspectionCriterion";
import { InspectionCriterionForm } from "@/components/admin/inspection-criterion/InspectionCriterionForm";

const PAGE_SIZE = 10;

const statusFilterOptions = [
  { value: "all", label: "Tất cả" },
  { value: "ACTIVE", label: "Đang hoạt động" },
  { value: "INACTIVE", label: "Ngừng sử dụng" },
];

export default function InspectionCriteriaManagementPage() {
  const canManage = usePermission(ROLE_ACCESS.inspectionCriteriaManagement);
  const [criteria, setCriteria] = useState<InspectionCriterion[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<
    InspectionCriterionStatus | undefined
  >(undefined);
  const [searchInput, setSearchInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);

  const [formOpen, setFormOpen] = useState(false);
  const [editingCriterion, setEditingCriterion] =
    useState<InspectionCriterion | null>(null);
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<InspectionCriterion | null>(
    null
  );
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  const fetchCriteria = async () => {
    setLoading(true);
    try {
      const data = await getInspectionCriteria({
        keyword: keyword || undefined,
        status: statusFilter,
        page: currentPage,
        size: PAGE_SIZE,
      });
      setCriteria(data.items);
      setTotalElements(data.totalElements);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể tải danh sách chỉ tiêu kiểm nghiệm"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCriteria();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage, statusFilter, keyword]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = searchInput.trim();
    if (next === keyword) return;
    setKeyword(next);
    setCurrentPage(0);
  };

  const openCreateDialog = () => {
    setEditingCriterion(null);
    setFormOpen(true);
  };

  const openEditDialog = (criterion: InspectionCriterion) => {
    setEditingCriterion(criterion);
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditingCriterion(null);
  };

  const handleToggleStatus = async (criterion: InspectionCriterion) => {
    if (togglingId !== null) return;
    setTogglingId(criterion.id);
    try {
      if (criterion.status === "ACTIVE") {
        await disableInspectionCriterion(criterion.id);
        toast.success(`Đã ngừng sử dụng chỉ tiêu "${criterion.name}"`);
      } else {
        await enableInspectionCriterion(criterion.id);
        toast.success(`Đã kích hoạt lại chỉ tiêu "${criterion.name}"`);
      }
      fetchCriteria();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể cập nhật trạng thái chỉ tiêu"
      );
    } finally {
      setTogglingId(null);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget || deleteSubmitting) return;
    setDeleteSubmitting(true);
    try {
      await deleteInspectionCriterion(deleteTarget.id);
      toast.success("Xóa chỉ tiêu kiểm nghiệm thành công");
      setDeleteTarget(null);
      // Trang hiện tại chỉ còn mục vừa xóa thì lùi lại trang trước
      if (criteria.length === 1 && currentPage > 0) {
        setCurrentPage(currentPage - 1);
      } else {
        fetchCriteria();
      }
    } catch (error: any) {
      // 409: chỉ tiêu đang được yêu cầu kiểm nghiệm tham chiếu (TC-05)
      toast.error(error.response?.data?.message || "Không thể xóa chỉ tiêu");
    } finally {
      setDeleteSubmitting(false);
    }
  };

  const totalPages = Math.ceil(totalElements / PAGE_SIZE);

  const getStatusFilterLabel = (value: string) => {
    const option = statusFilterOptions.find((opt) => opt.value === value);
    return option ? option.label : "Trạng thái";
  };

  const currentStatusValue = statusFilter ?? "all";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Quản lý danh mục chỉ tiêu kiểm nghiệm
          </h1>
          <p className="text-sm text-muted-foreground">
            Thêm, sửa, ngừng sử dụng các chỉ tiêu kiểm nghiệm dùng chung
          </p>
        </div>
        <div className="flex items-center gap-3">
          <HelpButton screenKey="admin-inspection-criteria" />
          {canManage && (
            <Button onClick={openCreateDialog} variant="create">
              <Plus className="h-4 w-4 mr-1" /> Thêm chỉ tiêu
            </Button>
          )}
        </div>
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <CardTitle className="text-xl font-bold text-slate-900">
              Danh sách chỉ tiêu
            </CardTitle>
            <div className="flex flex-wrap items-center gap-2">
              <form onSubmit={handleSearch} className="relative w-full sm:w-64">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Tìm theo tên hoặc tiêu chuẩn..."
                  className="h-9 pl-9"
                />
              </form>
              <Select
                value={currentStatusValue}
                onValueChange={(val) => {
                  setStatusFilter(
                    val === "all"
                      ? undefined
                      : (val as InspectionCriterionStatus)
                  );
                  setCurrentPage(0);
                }}
              >
                <SelectTrigger size="sm" className="w-[180px]">
                  <SelectValue placeholder="Trạng thái">
                    {getStatusFilterLabel(currentStatusValue)}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {statusFilterOptions.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                variant="outline"
                size="sm"
                onClick={fetchCriteria}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
                />
                Làm mới
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="text-center py-12 text-muted-foreground">
              Đang tải...
            </div>
          ) : criteria.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              Chưa có chỉ tiêu kiểm nghiệm nào trong danh mục.
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-slate-50/80">
                      <TableHead className="font-semibold text-slate-700">Tên chỉ tiêu</TableHead>
                      <TableHead className="font-semibold text-slate-700">Đơn vị</TableHead>
                      <TableHead className="font-semibold text-slate-700">Ngưỡng tối đa</TableHead>
                      <TableHead className="font-semibold text-slate-700">Tiêu chuẩn tham chiếu</TableHead>
                      <TableHead className="font-semibold text-slate-700">Trạng thái</TableHead>
                      {canManage && (
                        <TableHead className="text-right font-semibold text-slate-700">Thao tác</TableHead>
                      )}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {criteria.map((criterion) => {
                      const isActive = criterion.status === "ACTIVE";
                      return (
                        <TableRow
                          key={criterion.id}
                          className={`hover:bg-slate-50/60${
                            isActive ? "" : " opacity-60"
                          }`}
                        >
                          <TableCell className="font-medium">{criterion.name}</TableCell>
                          <TableCell>{criterion.unit}</TableCell>
                          <TableCell>{Number(criterion.maxThreshold)}</TableCell>
                          <TableCell className="max-w-xs truncate">
                            {criterion.referenceStandard || "—"}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={isActive ? "default" : "outline"}
                              className={
                                !isActive
                                  ? "text-muted-foreground bg-muted/50 border-muted-foreground/20"
                                  : ""
                              }
                            >
                              {isActive ? "Đang hoạt động" : "Ngừng sử dụng"}
                            </Badge>
                          </TableCell>
                          {canManage && (
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-1">
                                <Button
                                  variant="ghost"
                                  size="icon-sm"
                                  onClick={() => openEditDialog(criterion)}
                                  title="Sửa chỉ tiêu"
                                  className="hover:bg-muted"
                                >
                                  <Pencil className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon-sm"
                                  onClick={() => handleToggleStatus(criterion)}
                                  disabled={togglingId === criterion.id}
                                  title={isActive ? "Ngừng sử dụng" : "Kích hoạt lại"}
                                  className="hover:bg-muted"
                                >
                                  {isActive ? (
                                    <EyeOff className="h-4 w-4" />
                                  ) : (
                                    <Eye className="h-4 w-4" />
                                  )}
                                </Button>
                                {/* Chỉ tiêu đã tham chiếu chỉ được ngừng sử dụng (BR-5/BR-6) */}
                                {!criterion.referenced && (
                                  <Button
                                    variant="ghost"
                                    size="icon-sm"
                                    onClick={() => setDeleteTarget(criterion)}
                                    title="Xóa chỉ tiêu"
                                    className="text-destructive hover:text-destructive hover:bg-muted"
                                  >
                                    <Trash2 className="h-4 w-4" />
                                  </Button>
                                )}
                              </div>
                            </TableCell>
                          )}
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>

              {totalPages > 1 && (
                <div className="flex items-center justify-between mt-4 px-5 pb-5">
                  <div className="text-sm text-muted-foreground">
                    Hiển thị {currentPage * PAGE_SIZE + 1} -{" "}
                    {Math.min((currentPage + 1) * PAGE_SIZE, totalElements)}{" "}
                    trong tổng số {totalElements} chỉ tiêu
                  </div>
                  <div className="flex gap-1">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={currentPage === 0}
                      onClick={() => setCurrentPage((p) => p - 1)}
                    >
                      Trước
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={currentPage >= totalPages - 1}
                      onClick={() => setCurrentPage((p) => p + 1)}
                    >
                      Sau
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <InspectionCriterionForm
        open={formOpen}
        onClose={closeForm}
        onSuccess={fetchCriteria}
        criterion={editingCriterion}
      />

      <AlertDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa chỉ tiêu kiểm nghiệm</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc muốn xóa chỉ tiêu <strong>{deleteTarget?.name}</strong>{" "}
              không? Hành động này không thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setDeleteTarget(null)}
              disabled={deleteSubmitting}
            >
              Hủy
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleteSubmitting}
              className="bg-red-600 hover:bg-red-700"
            >
              {deleteSubmitting ? "Đang xóa..." : "Xóa"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>
    </div>
  );
}
