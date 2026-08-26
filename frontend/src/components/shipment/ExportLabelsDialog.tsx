import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LoaderCircle, QrCode } from "lucide-react";
import { toast } from "sonner";
import { exportQrLabels, type LabelIncludeFields } from "@/api/shipmentApi";
import type { Shipment } from "@/types/shipment";

/** Các khổ tem được hỗ trợ (mm). */
const LABEL_SIZES = [
  { value: "40x30", label: "40 × 30 mm (45 tem/trang A4)" },
  { value: "50x40", label: "50 × 40 mm (28 tem/trang A4)" },
  { value: "70x50", label: "70 × 50 mm (15 tem/trang A4)" },
] as const;

interface ExportLabelsDialogProps {
  open: boolean;
  shipment: Shipment | null;
  onClose: () => void;
}

/**
 * Dialog xuất tem QR cho lô hàng (NCL-04-CN-005).
 *
 * Cho phép chọn khổ tem, khoảng mã cần xuất và các trường thông tin
 * hiển thị trên tem. Kết quả là file PDF tải về trực tiếp.
 */
export const ExportLabelsDialog = ({
  open,
  shipment,
  onClose,
}: ExportLabelsDialogProps) => {
  const totalCodes = shipment?.traceCodes?.length || 0;

  const [labelSize, setLabelSize] = useState<string>("40x30");
  const [exportAll, setExportAll] = useState(true);
  const [startIndex, setStartIndex] = useState(0);
  const [count, setCount] = useState<number>(totalCodes || 0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [includeFields, setIncludeFields] = useState<LabelIncludeFields>({
    productName: true,
    cooperativeName: true,
    lotCode: true,
    packagingDate: true,
  });

  // Validation theo QTN-23: tổng số tem xuất không vượt số mã đã sinh
  const effectiveCount = exportAll
    ? totalCodes - startIndex
    : count;
  const validationError = (() => {
    if (totalCodes === 0) {
      return "Lô hàng chưa có mã QR nào để xuất.";
    }
    if (!exportAll && effectiveCount < 1) {
      return "Số lượng tem phải ít nhất là 1.";
    }
    if (
      !exportAll &&
      startIndex >= 0 &&
      startIndex + effectiveCount > totalCodes
    ) {
      return `Khoảng mã vượt quá số mã đã sinh (${totalCodes} mã).`;
    }
    return null;
  })();

  const handleDownloadPdf = (blob: Blob, shipmentName: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `Tem_QR_${shipmentName}_${labelSize}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleSubmit = async () => {
    if (!shipment || validationError) return;

    setIsSubmitting(true);
    try {
      const blob = await exportQrLabels(shipment.id, {
        startIndex,
        count: effectiveCount,
        labelSize,
        includeFields,
      });
      handleDownloadPdf(blob, shipment.name.replace(/\s+/g, "_"));
      toast.success(
        `Đã xuất ${effectiveCount} tem QR (khổ ${labelSize}).`,
      );
      onClose();
    } catch (error: any) {
      toast.error(error.message || "Có lỗi xảy ra khi xuất tem.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleField = (key: keyof LabelIncludeFields, checked: boolean) => {
    setIncludeFields((prev) => ({ ...prev, [key]: checked }));
  };

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && onClose()}>
      <DialogContent className="w-full max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <QrCode className="size-5" />
            Xuất tem QR
          </DialogTitle>
          <DialogDescription>
            Xuất file PDF tem QR để in dán lên bao bì sản phẩm.
          </DialogDescription>
        </DialogHeader>

        {shipment && (
          <div className="space-y-4">
            {/* Thông tin lô hàng */}
            <div className="rounded-lg border bg-muted/30 p-3 text-sm">
              <p className="font-medium">{shipment.name}</p>
              <p className="text-muted-foreground">
                Tổng số mã đã sinh:{" "}
                <span className="font-medium text-foreground">
                  {totalCodes}
                </span>
              </p>
            </div>

            {/* Khổ tem */}
            <div className="space-y-1.5">
              <Label>Khổ tem</Label>
              <Select
                value={labelSize}
                onValueChange={(value) => setLabelSize(value ?? "40x30")}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Chọn khổ tem" />
                </SelectTrigger>
                <SelectContent>
                  {LABEL_SIZES.map((size) => (
                    <SelectItem key={size.value} value={size.value}>
                      {size.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Khoảng mã cần xuất */}
            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="export-all"
                  checked={exportAll}
                  onCheckedChange={(checked) => setExportAll(checked === true)}
                />
                <Label htmlFor="export-all" className="cursor-pointer">
                  Xuất tất cả mã ({totalCodes} tem)
                </Label>
              </div>

              {!exportAll && (
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <Label htmlFor="start-index">Vị trí bắt đầu</Label>
                    <Input
                      id="start-index"
                      type="number"
                      min={0}
                      max={totalCodes - 1}
                      value={startIndex}
                      onChange={(e) =>
                        setStartIndex(Math.max(0, Number(e.target.value) || 0))
                      }
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="count">Số lượng tem</Label>
                    <Input
                      id="count"
                      type="number"
                      min={1}
                      max={totalCodes - startIndex}
                      value={count}
                      onChange={(e) =>
                        setCount(Math.max(1, Number(e.target.value) || 1))
                      }
                    />
                  </div>
                </div>
              )}

              {!exportAll && (
                <p className="text-xs text-muted-foreground">
                  Tem từ vị trí {startIndex + 1} đến{" "}
                  {Math.min(startIndex + effectiveCount, totalCodes)} trên tổng
                  số {totalCodes} mã.
                </p>
              )}
            </div>

            {/* Trường hiển thị trên tem */}
            <div className="space-y-2">
              <Label>Thông tin hiển thị trên tem</Label>
              <div className="grid grid-cols-2 gap-2">
                {(
                  [
                    ["productName", "Tên sản phẩm"],
                    ["cooperativeName", "Tên hợp tác xã"],
                    ["lotCode", "Mã lô"],
                    ["packagingDate", "Ngày đóng gói"],
                  ] as const
                ).map(([key, label]) => (
                  <div key={key} className="flex items-center space-x-2">
                    <Checkbox
                      id={`field-${key}`}
                      checked={includeFields[key] !== false}
                      onCheckedChange={(checked) =>
                        toggleField(key, checked === true)
                      }
                    />
                    <Label
                      htmlFor={`field-${key}`}
                      className="cursor-pointer text-sm font-normal"
                    >
                      {label}
                    </Label>
                  </div>
                ))}
              </div>
            </div>

            {/* Lỗi validation */}
            {validationError && (
              <p className="text-sm text-destructive">{validationError}</p>
            )}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting || validationError !== null}
          >
            {isSubmitting && (
              <LoaderCircle className="mr-1.5 size-4 animate-spin" />
            )}
            Xuất PDF
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
