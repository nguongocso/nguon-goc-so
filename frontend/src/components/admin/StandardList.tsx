import React, { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Plus, Pencil, RefreshCw, Trash2, ListChecks, Save } from "lucide-react";
import {
  getStandards,
  createStandard,
  updateStandard,
  getStandardCriteria,
  createStandardCriterion,
  updateStandardCriterion,
  deleteStandardCriterion,
} from "@/api/standardApi";
import { StandardForm } from "./StandardForm";
import type { StandardFormValues, InspectionCriterionFormValues } from "@/utils/validators";
import type { Standard, InspectionCriterion } from "@/types/standard";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";

const PAGE_SIZE = 10;

const statusFilterOptions = [
  { value: "all", label: "Tất cả" },
  { value: "true", label: "Đang hoạt động" },
  { value: "false", label: "Không hoạt động" },
];

export const StandardList: React.FC = () => {
  const canManage = usePermission(ROLE_ACCESS.standardManagement);
  const [standards, setStandards] = useState<Standard[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [isActiveFilter, setIsActiveFilter] = useState<boolean | undefined>(
    undefined,
  );
  const [loading, setLoading] = useState(true);

  const [formDialogOpen, setFormDialogOpen] = useState(false);
  const [editingStandard, setEditingStandard] = useState<Standard | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [criteriaDialogOpen, setCriteriaDialogOpen] = useState(false);
  const [selectedStandard, setSelectedStandard] = useState<Standard | null>(null);
  const [criteriaList, setCriteriaList] = useState<InspectionCriterion[]>([]);
  const [criteriaLoading, setCriteriaLoading] = useState(false);
  const [criteriaSubmitting, setCriteriaSubmitting] = useState(false);
  const [editingCriterion, setEditingCriterion] = useState<InspectionCriterion | null>(null);
  const [criteriaForm, setCriteriaForm] = useState<InspectionCriterionFormValues>({
    criterionCode: "",
    criterionName: "",
    note: "",
  });

  const fetchStandards = async () => {
    setLoading(true);
    try {
      const data = await getStandards({
        isActive: isActiveFilter,
        page: currentPage,
        size: PAGE_SIZE,
      });
      setStandards(data.items);
      setTotalElements(data.totalElements);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể tải danh sách tiêu chuẩn",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStandards();
  }, [currentPage, isActiveFilter]);

  const handleCreate = async (data: StandardFormValues) => {
    setSubmitting(true);
    try {
      await createStandard({
        name: data.name,
        description: data.description || undefined,
        issuingBody: data.issuingBody || undefined,
      });
      toast.success("Thêm tiêu chuẩn thành công");
      setFormDialogOpen(false);
      fetchStandards();
    } catch (error: any) {
      const msg = error.response?.data?.message || "Thêm tiêu chuẩn thất bại";
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async (data: StandardFormValues) => {
    if (!editingStandard) return;
    setSubmitting(true);
    try {
      await updateStandard(editingStandard.id, {
        name: data.name,
        description: data.description || undefined,
        issuingBody: data.issuingBody || undefined,
        isActive: data.isActive ?? true,
      });
      toast.success("Cập nhật tiêu chuẩn thành công");
      setFormDialogOpen(false);
      setEditingStandard(null);
      fetchStandards();
    } catch (error: any) {
      const msg =
        error.response?.data?.message || "Cập nhật tiêu chuẩn thất bại";
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const openCreateDialog = () => {
    setEditingStandard(null);
    setFormDialogOpen(true);
  };

  const openEditDialog = (standard: Standard) => {
    setEditingStandard(standard);
    setFormDialogOpen(true);
  };

  const closeDialog = () => {
    setFormDialogOpen(false);
    setEditingStandard(null);
  };

  const resetCriteriaForm = () => {
    setEditingCriterion(null);
    setCriteriaForm({ criterionCode: "", criterionName: "", note: "" });
  };

  const closeCriteriaManager = () => {
    setCriteriaDialogOpen(false);
    setSelectedStandard(null);
    setCriteriaList([]);
    resetCriteriaForm();
  };

  const openCriteriaManager = async (standard: Standard) => {
    resetCriteriaForm();
    setCriteriaList([]);
    setSelectedStandard(standard);
    setCriteriaDialogOpen(true);
    setCriteriaLoading(true);
    try {
      const data = await getStandardCriteria(standard.id);
      setCriteriaList(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể tải danh sách tiêu chí",
      );
    } finally {
      setCriteriaLoading(false);
    }
  };

  const handleCriteriaFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStandard) return;

    if (!criteriaForm.criterionCode.trim() || !criteriaForm.criterionName.trim()) {
      toast.error("Mã tiêu chí và tên tiêu chí không được để trống");
      return;
    }

    setCriteriaSubmitting(true);
    try {
      if (editingCriterion) {
        await updateStandardCriterion(selectedStandard.id, editingCriterion.criteriaId, {
          standardId: selectedStandard.id,
          criterionCode: criteriaForm.criterionCode.trim(),
          criterionName: criteriaForm.criterionName.trim(),
          note: criteriaForm.note?.trim() || undefined,
        });
        toast.success("Cập nhật tiêu chí thành công");
      } else {
        await createStandardCriterion(selectedStandard.id, {
          standardId: selectedStandard.id,
          criterionCode: criteriaForm.criterionCode.trim(),
          criterionName: criteriaForm.criterionName.trim(),
          note: criteriaForm.note?.trim() || undefined,
        });
        toast.success("Thêm tiêu chí thành công");
      }

      resetCriteriaForm();
      const data = await getStandardCriteria(selectedStandard.id);
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
    if (!selectedStandard) return;

    const confirmed = window.confirm(
      `Bạn có chắc muốn xóa tiêu chí "${criterion.name}" không?`,
    );
    if (!confirmed) return;

    try {
      await deleteStandardCriterion(selectedStandard.id, criterion.criteriaId);
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

  const totalPages = Math.ceil(totalElements / PAGE_SIZE);

  const getStatusFilterLabel = (value: string) => {
    const option = statusFilterOptions.find((opt) => opt.value === value);
    return option ? option.label : "Trạng thái";
  };

  const currentFilterValue =
    isActiveFilter === undefined ? "all" : String(isActiveFilter);

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <CardTitle>Danh mục tiêu chuẩn chất lượng</CardTitle>
            <div className="flex flex-wrap items-center gap-2">
              <Select
                value={currentFilterValue}
                onValueChange={(val) => {
                  if (val === "all") setIsActiveFilter(undefined);
                  else setIsActiveFilter(val === "true");
                }}
              >
                <SelectTrigger size="sm" className="w-[180px]">
                  <SelectValue placeholder="Trạng thái">
                    {getStatusFilterLabel(currentFilterValue)}
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
                onClick={fetchStandards}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
                />
                Làm mới
              </Button>
              {canManage && (
                <Button onClick={openCreateDialog}>
                  <Plus className="h-4 w-4 mr-1" />
                  Thêm tiêu chuẩn
                </Button>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-center py-8">Đang tải...</div>
          ) : standards.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              Chưa có tiêu chuẩn nào trong danh mục.
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Tên tiêu chuẩn</TableHead>
                      <TableHead>Cơ quan ban hành</TableHead>
                      <TableHead>Mô tả</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Ngày tạo</TableHead>
                      {canManage && (
                        <TableHead className="text-right">Thao tác</TableHead>
                      )}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {standards.map((std) => {
                      const isActive = std.isActive;
                      return (
                        <TableRow
                          key={std.id}
                          className={!isActive ? "opacity-60" : ""}
                        >
                          <TableCell className="font-medium">
                            {std.name}
                          </TableCell>
                          <TableCell>{std.issuingBody || "---"}</TableCell>
                          <TableCell className="max-w-xs truncate">
                            {std.description || "---"}
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
                              {isActive ? "Hoạt động" : "Không hoạt động"}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            {new Date(std.createdAt).toLocaleDateString("vi-VN")}
                          </TableCell>
                          {canManage && (
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-1">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => openCriteriaManager(std)}
                                  title="Quản lý tiêu chí"
                                >
                                  <ListChecks className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => openEditDialog(std)}
                                  disabled={!isActive}
                                  className={!isActive ? "text-muted-foreground" : ""}
                                  title="Sửa tiêu chuẩn"
                                >
                                  <Pencil className="h-4 w-4" />
                                </Button>
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
                <div className="flex items-center justify-between mt-4">
                  <div className="text-sm text-muted-foreground">
                    Hiển thị {currentPage * PAGE_SIZE + 1} -{" "}
                    {Math.min((currentPage + 1) * PAGE_SIZE, totalElements)}{" "}
                    trong tổng số {totalElements} tiêu chuẩn
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

      <StandardForm
        open={formDialogOpen}
        onClose={closeDialog}
        onSubmit={editingStandard ? handleUpdate : handleCreate}
        initialData={editingStandard}
        isLoading={submitting}
      />

      <Dialog open={criteriaDialogOpen} onOpenChange={(open) => {
        if (!open) {
          closeCriteriaManager();
        }
      }}>
        <DialogContent className="max-w-3xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              Quản lý tiêu chí kiểm nghiệm - {selectedStandard?.name || ""}
            </DialogTitle>
          </DialogHeader>

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

          <DialogFooter>
            <Button variant="outline" onClick={closeCriteriaManager}>
              Đóng
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};