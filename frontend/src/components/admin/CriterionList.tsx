import React, { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { DataTablePagination } from "@/components/common/DataTablePagination";
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
  AlertDialogPopup,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/components/ui/alert-dialog";
import { Plus, Pencil, Trash2, Search } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import {
  getStandards,
  getStandardCriteria,
  createStandardCriterion,
  updateStandardCriterion,
  deleteStandardCriterion,
} from "@/api/standardApi";
import type { InspectionCriterionFormValues } from "@/utils/validators";
import type { InspectionCriterion } from "@/types/standard";
import { CriterionForm } from "./CriterionForm";

const PAGE_SIZE = 10;

interface CriterionListProps {
  standardId?: string;
}

export const CriterionList: React.FC<CriterionListProps> = ({ standardId }) => {
  const [criteriaList, setCriteriaList] = useState<InspectionCriterion[]>([]);
  const [criteriaLoading, setCriteriaLoading] = useState(false);
  const [criteriaSubmitting, setCriteriaSubmitting] = useState(false);
  const [standardName, setStandardName] = useState<string | null>(null);

  const [formOpen, setFormOpen] = useState(false);
  const [editingCriterion, setEditingCriterion] =
    useState<InspectionCriterion | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<InspectionCriterion | null>(
    null,
  );
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const filteredCriteria = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return criteriaList;
    return criteriaList.filter(
      (criterion) =>
        criterion.code.toLowerCase().includes(keyword) ||
        criterion.name.toLowerCase().includes(keyword),
    );
  }, [criteriaList, search]);

  const totalPages = Math.max(1, Math.ceil(filteredCriteria.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedCriteria = filteredCriteria.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const fetchStandardName = async () => {
    if (!standardId) return;
    try {
      const data = await getStandards({ page: 0, size: 100 });
      const found = data.items.find((s) => s.id === standardId);
      if (found) setStandardName(found.name);
    } catch {
      setStandardName(null);
    }
  };

  const fetchCriteria = async () => {
    if (!standardId) return;
    setCriteriaLoading(true);
    try {
      const data = await getStandardCriteria(standardId);
      setCriteriaList(data);
      if (data.length > 0) {
        setStandardName(data[0].standardName);
      } else {
        setStandardName(null);
        await fetchStandardName();
      }
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể tải danh sách tiêu chí",
      );
    } finally {
      setCriteriaLoading(false);
    }
  };

  useEffect(() => {
    void fetchCriteria();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [standardId]);

  const openCreateForm = () => {
    setEditingCriterion(null);
    setFormOpen(true);
  };

  const openEditForm = (criterion: InspectionCriterion) => {
    setEditingCriterion(criterion);
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditingCriterion(null);
  };

  const handleFormSubmit = async (values: InspectionCriterionFormValues) => {
    if (!standardId) return;

    const payload = {
      standardId,
      criterionCode: values.criterionCode.trim(),
      criterionName: values.criterionName.trim(),
      note: values.note?.trim() || undefined,
    };

    setCriteriaSubmitting(true);
    try {
      if (editingCriterion) {
        const updated = await updateStandardCriterion(
          standardId,
          editingCriterion.criteriaId,
          payload,
        );
        toast.success("Cập nhật tiêu chí thành công");
        setCriteriaList((prev) =>
          prev.map((item) =>
            item.criteriaId === updated.criteriaId ? updated : item,
          ),
        );
      } else {
        const created = await createStandardCriterion(standardId, payload);
        toast.success("Thêm tiêu chí thành công");
        setCriteriaList((prev) => [...prev, created]);
      }
      closeForm();
    } catch (error: any) {
      const msg = error.response?.data?.message || "Lưu tiêu chí thất bại";
      toast.error(msg);
    } finally {
      setCriteriaSubmitting(false);
    }
  };

  const handleDeleteCriterion = async () => {
    if (!standardId || !deleteTarget) return;

    setDeleteSubmitting(true);
    try {
      await deleteStandardCriterion(standardId, deleteTarget.criteriaId);
      toast.success("Xóa tiêu chí thành công");
      setCriteriaList((prev) =>
        prev.filter((item) => item.criteriaId !== deleteTarget.criteriaId),
      );
      if (editingCriterion?.criteriaId === deleteTarget.criteriaId) {
        closeForm();
      }
      setDeleteTarget(null);
    } catch (error: any) {
      const msg = error.response?.data?.message || "Xóa tiêu chí thất bại";
      toast.error(msg);
    } finally {
      setDeleteSubmitting(false);
    }
  };

  return (
    <>
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Quản lý tiêu chí kiểm nghiệm</h1>
          <p className="text-sm text-muted-foreground mt-1">
            {criteriaLoading
              ? "Đang tải thông tin tiêu chuẩn..."
              : standardName
                ? `Tiêu chuẩn: ${standardName}`
                : "Không tìm thấy tiêu chuẩn"}
          </p>
        </div>
        <HelpButton screenKey="admin-standard-criteria" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <CardTitle className="text-xl font-bold text-slate-900">
            Tiêu chí kiểm nghiệm
            <Badge variant="outline" className="ml-2 align-middle">
              {filteredCriteria.length} tiêu chí
            </Badge>
          </CardTitle>
          <div className="flex items-center gap-2">
            <div className="relative w-full max-w-[240px]">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="pl-9"
                placeholder="Tìm theo mã hoặc tên tiêu chí..."
                aria-label="Tìm kiếm tiêu chí"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
            </div>
            <Button variant="create" size="sm" onClick={openCreateForm}>
              <Plus className="h-4 w-4 mr-1" />
              Thêm mới
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {criteriaLoading ? (
            <div className="text-center py-12 text-muted-foreground">
              Đang tải tiêu chí...
            </div>
          ) : filteredCriteria.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              {search.trim()
                ? "Không tìm thấy tiêu chí phù hợp."
                : "Chưa có tiêu chí nào. Nhấn \"Thêm mới\" để tạo tiêu chí."}
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50/80">
                    <TableHead className="font-semibold text-slate-700">Mã tiêu chí</TableHead>
                    <TableHead className="font-semibold text-slate-700">Tên tiêu chí</TableHead>
                    <TableHead className="font-semibold text-slate-700">Ghi chú</TableHead>
                    <TableHead className="text-right font-semibold text-slate-700">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedCriteria.map((criterion) => (
                    <TableRow key={criterion.criteriaId} className="hover:bg-slate-50/60">
                      <TableCell className="font-medium">
                        {criterion.code}
                      </TableCell>
                      <TableCell>{criterion.name}</TableCell>
                      <TableCell className="max-w-xs truncate text-muted-foreground">
                        {criterion.note || "—"}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => openEditForm(criterion)}
                            title="Sửa tiêu chí"
                            className="hover:bg-muted"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => setDeleteTarget(criterion)}
                            title="Xóa tiêu chí"
                            className="text-destructive hover:text-destructive hover:bg-muted"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              </div>
              <DataTablePagination
                page={safePage}
                pageSize={PAGE_SIZE}
                totalElements={filteredCriteria.length}
                onPageChange={setPage}
                itemLabel="tiêu chí"
              />
            </>
          )}
        </CardContent>
      </Card>

      <CriterionForm
        open={formOpen}
        onClose={closeForm}
        onSubmit={handleFormSubmit}
        initialData={editingCriterion}
        isLoading={criteriaSubmitting}
      />

      <AlertDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa tiêu chí</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc muốn xóa tiêu chí{" "}
              <strong>{deleteTarget?.name}</strong> không? Hành động này không
              thể hoàn tác.
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
              onClick={handleDeleteCriterion}
              disabled={deleteSubmitting}
              className="bg-red-600 hover:bg-red-700"
            >
              {deleteSubmitting ? "Đang xóa..." : "Xóa"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>
    </>
  );
};