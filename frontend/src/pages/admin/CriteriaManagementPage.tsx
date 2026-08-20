import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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
import {
  Plus,
  Pencil,
  Trash2,
  Save,
  ChevronLeft,
  ListChecks,
} from "lucide-react";
import {
  getStandards,
  getStandardCriteria,
  createStandardCriterion,
  updateStandardCriterion,
  deleteStandardCriterion,
} from "@/api/standardApi";
import {
  inspectionCriterionSchema,
  type InspectionCriterionFormValues,
} from "@/utils/validators";
import type { InspectionCriterion } from "@/types/standard";

const CriteriaManagementPage: React.FC = () => {
  const navigate = useNavigate();
  const { standardId } = useParams<{ standardId: string }>();

  const [criteriaList, setCriteriaList] = useState<InspectionCriterion[]>([]);
  const [criteriaLoading, setCriteriaLoading] = useState(false);
  const [criteriaSubmitting, setCriteriaSubmitting] = useState(false);
  const [standardName, setStandardName] = useState<string | null>(null);
  const [editingCriterion, setEditingCriterion] =
    useState<InspectionCriterion | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<InspectionCriterion | null>(
    null,
  );
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InspectionCriterionFormValues>({
    resolver: zodResolver(inspectionCriterionSchema),
    defaultValues: {
      criterionCode: "",
      criterionName: "",
      note: "",
    },
  });

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

  const resetCriteriaForm = () => {
    setEditingCriterion(null);
    reset({ criterionCode: "", criterionName: "", note: "" });
  };

  const handleCriteriaFormSubmit = handleSubmit(async (values) => {
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
      resetCriteriaForm();
    } catch (error: any) {
      const msg = error.response?.data?.message || "Lưu tiêu chí thất bại";
      toast.error(msg);
    } finally {
      setCriteriaSubmitting(false);
    }
  });

  const handleEditCriterion = (criterion: InspectionCriterion) => {
    setEditingCriterion(criterion);
    reset({
      criterionCode: criterion.code,
      criterionName: criterion.name,
      note: criterion.note || "",
    });
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
        resetCriteriaForm();
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
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate("/admin/standards")}
            className="mb-2 -ml-2"
          >
            <ChevronLeft className="h-4 w-4 mr-1" />
            Quay lại
          </Button>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <ListChecks className="h-6 w-6 text-muted-foreground" />
            Quản lý tiêu chí kiểm nghiệm
          </h1>
          <p className="text-sm text-muted-foreground">
            {criteriaLoading
              ? "Đang tải thông tin tiêu chuẩn..."
              : standardName
                ? `Tiêu chuẩn: ${standardName}`
                : "Không tìm thấy tiêu chuẩn"}
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Tiêu chí kiểm nghiệm</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6 md:grid-cols-[1.2fr_1fr]">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-medium">Danh sách tiêu chí</h3>
                <Badge variant="outline">{criteriaList.length} tiêu chí</Badge>
              </div>

              {criteriaLoading ? (
                <div className="text-sm text-muted-foreground py-6 text-center">
                  Đang tải tiêu chí...
                </div>
              ) : criteriaList.length === 0 ? (
                <div className="text-sm text-muted-foreground py-6 text-center border rounded-md">
                  Chưa có tiêu chí nào cho tiêu chuẩn này.
                </div>
              ) : (
                <div className="space-y-2 max-h-[340px] overflow-y-auto pr-1">
                  {criteriaList.map((criterion) => (
                    <div
                      key={criterion.criteriaId}
                      className="flex items-center justify-between rounded-md border p-3 gap-3"
                    >
                      <div className="min-w-0">
                        <div className="font-medium text-sm">
                          {criterion.code}
                        </div>
                        <div className="text-sm text-muted-foreground truncate">
                          {criterion.name}
                        </div>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleEditCriterion(criterion)}
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="delete"
                          size="sm"
                          onClick={() => setDeleteTarget(criterion)}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <form
              onSubmit={handleCriteriaFormSubmit}
              className="space-y-4"
              noValidate
            >
              <div className="space-y-2">
                <Label htmlFor="criterionCode">Mã tiêu chí</Label>
                <Input
                  id="criterionCode"
                  {...register("criterionCode")}
                  placeholder="VD: HEAVY_METAL"
                  disabled={criteriaSubmitting}
                  aria-invalid={!!errors.criterionCode}
                />
                {errors.criterionCode && (
                  <p className="text-sm text-red-500">
                    {errors.criterionCode.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="criterionName">Tên tiêu chí</Label>
                <Input
                  id="criterionName"
                  {...register("criterionName")}
                  placeholder="VD: Kim loại nặng"
                  disabled={criteriaSubmitting}
                  aria-invalid={!!errors.criterionName}
                />
                {errors.criterionName && (
                  <p className="text-sm text-red-500">
                    {errors.criterionName.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="criterionNote">Ghi chú</Label>
                <Textarea
                  id="criterionNote"
                  {...register("note")}
                  placeholder="Nhập ghi chú cho tiêu chí..."
                  disabled={criteriaSubmitting}
                />
                {errors.note && (
                  <p className="text-sm text-red-500">{errors.note.message}</p>
                )}
              </div>

              <div className="flex gap-2 pt-2">
                <Button
                  type="submit"
                  disabled={criteriaSubmitting}
                  className="flex-1"
                >
                  {criteriaSubmitting ? (
                    "Đang lưu..."
                  ) : editingCriterion ? (
                    <>
                      <Save className="h-4 w-4 mr-1" />
                      Cập nhật
                    </>
                  ) : (
                    <>
                      <Plus className="h-4 w-4 mr-1" />
                      Thêm mới
                    </>
                  )}
                </Button>
                {editingCriterion && (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={resetCriteriaForm}
                    disabled={criteriaSubmitting}
                  >
                    Hủy
                  </Button>
                )}
              </div>
            </form>
          </div>
        </CardContent>
      </Card>

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
    </div>
  );
};

export default CriteriaManagementPage;
