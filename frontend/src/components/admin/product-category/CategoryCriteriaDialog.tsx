import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  assignProductCategoryCriteria,
  getInspectionCriteria,
  getProductCategoryCriteria,
} from "@/api/inspectionCriterionApi";
import type { InspectionCriterion } from "@/types/inspectionCriterion";
import type { ProductCategory } from "@/types/productCategory";

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  category: ProductCategory | null;
}

/**
 * Dialog gán bộ chỉ tiêu kiểm nghiệm mặc định cho loại nông sản.
 * PUT /api/v1/product-categories/{id}/criteria — semantic REPLACE toàn bộ.
 */
export const CategoryCriteriaDialog = ({
  open,
  onClose,
  onSuccess,
  category,
}: Props) => {
  const [criteriaOptions, setCriteriaOptions] = useState<
    InspectionCriterion[]
  >([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open || !category) return;
    const loadAssignment = async () => {
      setLoading(true);
      try {
        // Chỉ tiêu ACTIVE mới được gán (backend từ chối chỉ tiêu INACTIVE)
        const [catalogPage, assigned] = await Promise.all([
          getInspectionCriteria({ status: "ACTIVE", page: 0, size: 200 }),
          getProductCategoryCriteria(category.id, true),
        ]);
        setCriteriaOptions(catalogPage.items);
        setSelectedIds(assigned.map((c) => c.id));
      } catch (error: any) {
        toast.error(
          error.response?.data?.message ||
            "Không thể tải bộ chỉ tiêu của loại nông sản"
        );
      } finally {
        setLoading(false);
      }
    };
    void loadAssignment();
  }, [open, category]);

  const toggleSelected = (id: number, checked: boolean) => {
    setSelectedIds((prev) =>
      checked ? [...prev, id] : prev.filter((v) => v !== id)
    );
  };

  const handleSubmit = async () => {
    if (!category || saving) return;
    setSaving(true);
    try {
      await assignProductCategoryCriteria(category.id, selectedIds);
      toast.success(`Đã cập nhật bộ chỉ tiêu cho "${category.name}"`);
      onSuccess();
      onClose();
    } catch (error: any) {
      // BR-3: xóa hết chỉ tiêu khi đang bật bắt buộc kiểm nghiệm sẽ bị backend từ chối
      toast.error(
        error.response?.data?.message || "Không thể cập nhật bộ chỉ tiêu"
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Gán bộ chỉ tiêu — {category?.name}</DialogTitle>
        </DialogHeader>

        {loading ? (
          <div className="text-center py-8 text-muted-foreground">
            Đang tải...
          </div>
        ) : criteriaOptions.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            Danh mục chưa có chỉ tiêu đang hoạt động nào.
          </div>
        ) : (
          <div className="space-y-3 max-h-[320px] overflow-y-auto rounded-md border p-3">
            {criteriaOptions.map((criterion) => (
              <div key={criterion.id} className="flex items-start gap-2">
                <Checkbox
                  id={`criterion-${criterion.id}`}
                  checked={selectedIds.includes(criterion.id)}
                  onCheckedChange={(checked) =>
                    toggleSelected(criterion.id, checked === true)
                  }
                />
                <Label
                  htmlFor={`criterion-${criterion.id}`}
                  className="cursor-pointer text-sm font-normal leading-snug"
                >
                  {criterion.name}
                  <span className="block text-xs text-muted-foreground">
                    ≤ {Number(criterion.maxThreshold)} {criterion.unit}
                    {criterion.referenceStandard
                      ? ` · ${criterion.referenceStandard}`
                      : ""}
                  </span>
                </Label>
              </div>
            ))}
          </div>
        )}

        <p className="text-xs text-muted-foreground">
          Lưu bộ chỉ tiêu sẽ thay thế toàn bộ gán hiện có của loại nông sản này.
        </p>

        {/* Nút hành động */}
        <div className="flex justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={saving}
          >
            Hủy
          </Button>
          <Button onClick={handleSubmit} disabled={loading || saving}>
            {saving ? "Đang lưu..." : "Lưu bộ chỉ tiêu"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
