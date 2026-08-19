import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Plus, Pencil, Trash2, Save, ChevronLeft, ListChecks } from "lucide-react";
import {
  getStandardById,
  getStandardCriteria,
  createStandardCriterion,
  updateStandardCriterion,
  deleteStandardCriterion,
} from "@/api/standardApi";
import type { InspectionCriterionFormValues } from "@/utils/validators";
import type { Standard, InspectionCriterion } from "@/types/standard";

const CriteriaManagementPage: React.FC = () => {
  const navigate = useNavigate();
  const { standardId } = useParams<{ standardId: string }>();

  const [standard, setStandard] = useState<Standard | null>(null);
  const [standardLoading, setStandardLoading] = useState(true);
  const [criteriaList, setCriteriaList] = useState<InspectionCriterion[]>([]);
  const [criteriaLoading, setCriteriaLoading] = useState(false);
  const [criteriaSubmitting, setCriteriaSubmitting] = useState(false);
  const [editingCriterion, setEditingCriterion] = useState<InspectionCriterion | null>(null);
  const [criteriaForm, setCriteriaForm] = useState<InspectionCriterionFormValues>({
    criterionCode: "",
    criterionName: "",
    note: "",
  });

  const fetchStandard = async () => {
    if (!standardId) return;
    setStandardLoading(true);
    try {
      const data = await getStandardById(standardId);
      setStandard(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể tải thông tin tiêu chuẩn",
      );
    } finally {
      setStandardLoading(false);
    }
  };

  const fetchCriteria = async () => {
    if (!standardId) return;
    setCriteriaLoading(true);
    try {
      const data = await getStandardCriteria(standardId);
      setCriteriaList(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể tải danh sách tiêu chí",
      );
    } finally {
      setCriteriaLoading(false);
    }
  };

  useEffect(() => {
    void fetchStandard();
    void fetchCriteria();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [standardId]);

  const resetCriteriaForm = () => {
    setEditingCriterion(null);
    setCriteriaForm({ criterionCode: "", criterionName: "", note: "" });
  };

  const handleCriteriaFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!standardId) return;

    if (!criteriaForm.criterionCode.trim() || !criteriaForm.criterionName.trim()) {
      toast.error("Mã tiêu chí và tên tiêu chí không được để trống");
      return;
    }

    setCriteriaSubmitting(true);
    try {
      if (editingCriterion) {
        await updateStandardCriterion(standardId, editingCriterion.criteriaId, {
          standardId,
          criterionCode: criteriaForm.criterionCode.trim(),
          criterionName: criteriaForm.criterionName.trim(),
          note: criteriaForm.note?.trim() || undefined,
        });
        toast.success("Cập nhật tiêu chí thành công");
      } else {
        await createStandardCriterion(standardId, {
          standardId,
          criterionCode: criteriaForm.criterionCode.trim(),
          criterionName: criteriaForm.criterionName.trim(),
          note: criteriaForm.note?.trim() || undefined,
        });
        toast.success("Thêm tiêu chí thành công");
      }

      resetCriteriaForm();
      const data = await getStandardCriteria(standardId);
      setCriteriaList(data);
    } catch (error: any) {
      const msg = error.response?.data?.message || "Lưu tiêu chí thất bại";
      toast.error(msg);
    } finally {
      setCriteriaSubmitting(false);
    }
  };

  const handleEditCriterion = (criterion: InspectionCriterion) => {
    setEditingCriterion(criterion);
    setCriteriaForm({
      criterionCode: criterion.code,
      criterionName: criterion.name,
      note: criterion.note || "",
    });
  };

  const handleDeleteCriterion = async (criterion: InspectionCriterion) => {
    if (!standardId) return;

    const confirmed = window.confirm(
      `Bạn có chắc muốn xóa tiêu chí "${criterion.name}" không?`,
    );
    if (!confirmed) return;

    try {
      await deleteStandardCriterion(standardId, criterion.criteriaId);
      toast.success("Xóa tiêu chí thành công");
      setCriteriaList((prev) =>
        prev.filter((item) => item.criteriaId !== criterion.criteriaId),
      );
      if (editingCriterion?.criteriaId === criterion.criteriaId) {
        resetCriteriaForm();
      }
    } catch (error: any) {
      const msg = error.response?.data?.message || "Xóa tiêu chí thất bại";
      toast.error(msg);
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
            {standardLoading
              ? "Đang tải thông tin tiêu chuẩn..."
              : standard
                ? `Tiêu chuẩn: ${standard.name}`
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
                        <div className="font-medium text-sm">{criterion.code}</div>
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
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDeleteCriterion(criterion)}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <form onSubmit={handleCriteriaFormSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="criterionCode">Mã tiêu chí</Label>
                <Input
                  id="criterionCode"
                  value={criteriaForm.criterionCode}
                  onChange={(e) =>
                    setCriteriaForm((prev) => ({
                      ...prev,
                      criterionCode: e.target.value,
                    }))
                  }
                  placeholder="VD: HEAVY_METAL"
                  disabled={criteriaSubmitting}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="criterionName">Tên tiêu chí</Label>
                <Input
                  id="criterionName"
                  value={criteriaForm.criterionName}
                  onChange={(e) =>
                    setCriteriaForm((prev) => ({
                      ...prev,
                      criterionName: e.target.value,
                    }))
                  }
                  placeholder="VD: Kim loại nặng"
                  disabled={criteriaSubmitting}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="criterionNote">Ghi chú</Label>
                <textarea
                  id="criterionNote"
                  value={criteriaForm.note || ""}
                  onChange={(e) =>
                    setCriteriaForm((prev) => ({
                      ...prev,
                      note: e.target.value,
                    }))
                  }
                  placeholder="Nhập ghi chú cho tiêu chí..."
                  disabled={criteriaSubmitting}
                  className="flex min-h-[90px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                />
              </div>

              <div className="flex gap-2 pt-2">
                <Button type="submit" disabled={criteriaSubmitting} className="flex-1">
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
    </div>
  );
};

export default CriteriaManagementPage;