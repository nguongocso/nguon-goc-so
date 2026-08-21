import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
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
import { Plus, Pencil, RefreshCw, ListChecks } from "lucide-react";
import {
  getStandards,
  createStandard,
  updateStandard,
} from "@/api/standardApi";
import { StandardForm } from "./StandardForm";
import type { StandardFormValues } from "@/utils/validators";
import type { Standard } from "@/types/standard";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";

const PAGE_SIZE = 10;

const statusFilterOptions = [
  { value: "all", label: "Tất cả" },
  { value: "true", label: "Đang hoạt động" },
  { value: "false", label: "Không hoạt động" },
];

export const StandardList: React.FC = () => {
  const navigate = useNavigate();
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
                          <TableCell>{std.issuingBody || "—"}</TableCell>
                          <TableCell className="max-w-xs truncate">
                            {std.description || "—"}
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
                                  variant="ghost"
                                  size="icon-sm"
                                  onClick={() =>
                                    navigate(`/admin/standards/${std.id}/criteria`)
                                  }
                                  title="Quản lý tiêu chí"
                                  className="hover:bg-muted"
                                >
                                  <ListChecks className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon-sm"
                                  onClick={() => openEditDialog(std)}
                                  disabled={!isActive}
                                  className={`hover:bg-muted${!isActive ? " text-muted-foreground" : ""}`}
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
    </>
  );
};