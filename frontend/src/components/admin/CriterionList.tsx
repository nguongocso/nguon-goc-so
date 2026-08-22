import React, { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
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
import { Plus, Pencil, Trash2 } from "lucide-react";
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

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>
            Tiêu chí kiểm nghiệm
            <Badge variant="outline" className="ml-2 align-middle">
              {criteriaList.length} tiêu chí
            </Badge>
          </CardTitle>
          <Button variant="create" onClick={openCreateForm}>
            <Plus className="h-4 w-4 mr-1" />
            Thêm mới
          </Button>
        </CardHeader>
        <CardContent>
          {criteriaLoading ? (
            <div className="text-center py-8 text-muted-foreground">
              Đang tải tiêu chí...
            </div>
          ) : criteriaList.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              Chưa có tiêu chí nào. Nhấn "Thêm mới" để tạo tiêu chí.
            </div>
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Mã tiêu chí</TableHead>
                    <TableHead>Tên tiêu chí</TableHead>
                    <TableHead>Ghi chú</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {criteriaList.map((criterion) => (
                    <TableRow key={criterion.criteriaId}>
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