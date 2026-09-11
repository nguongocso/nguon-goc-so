import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Clock, Info, Loader2, Settings2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  getInspectionExpiryThreshold,
  updateInspectionExpiryThreshold,
} from "@/api/inspectionCriterionApi";
import type { InspectionExpiryThresholdResponse } from "@/types/inspectionCriterion";
import { formatDateTime } from "@/utils/dateTime";

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess?: (thresholdDays: number) => void;
}

/**
 * Modal cấu hình ngưỡng số ngày cảnh báo sắp hết hiệu lực kết quả kiểm nghiệm.
 * Chỉ Quản trị viên hệ thống (VT-01) mới có quyền truy cập và lưu cấu hình này.
 */
export const InspectionExpiryThresholdDialog = ({
  open,
  onClose,
  onSuccess,
}: Props) => {
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [days, setDays] = useState<string>("15");
  const [errorText, setErrorText] = useState<string | null>(null);
  const [configData, setConfigData] =
    useState<InspectionExpiryThresholdResponse | null>(null);

  useEffect(() => {
    if (!open) {
      setErrorText(null);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setErrorText(null);

    getInspectionExpiryThreshold()
      .then((data) => {
        if (!isMounted) return;
        setConfigData(data);
        setDays(String(data.warningThresholdDays ?? 15));
      })
      .catch((err: any) => {
        if (!isMounted) return;
        toast.error(
          err.response?.data?.message ||
            "Không thể tải cấu hình ngưỡng cảnh báo hiện tại"
        );
      })
      .finally(() => {
        if (isMounted) {
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [open]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const val = Number.parseInt(days, 10);

    if (Number.isNaN(val) || val < 1 || val > 365) {
      setErrorText("Ngưỡng cảnh báo phải là số nguyên từ 1 đến 365 ngày.");
      return;
    }

    setSubmitting(true);
    setErrorText(null);

    try {
      const res = await updateInspectionExpiryThreshold({
        warningThresholdDays: val,
      });
      toast.success("Cập nhật ngưỡng cảnh báo hết hiệu lực thành công");
      onSuccess?.(res.warningThresholdDays);
      onClose();
    } catch (err: any) {
      const message =
        err.response?.data?.message || "Không thể lưu cấu hình ngưỡng cảnh báo";
      setErrorText(message);
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2 text-primary font-semibold">
            <Settings2 className="h-5 w-5" />
            <DialogTitle>Cấu hình ngưỡng cảnh báo hết hiệu lực</DialogTitle>
          </div>
          <DialogDescription className="text-sm text-muted-foreground mt-1">
            Thiết lập khoảng thời gian tính theo ngày để hệ thống tự động phát
            hiện và cảnh báo các chỉ tiêu kiểm nghiệm sắp hết hạn hiệu lực.
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="flex flex-col items-center justify-center py-8 gap-2 text-muted-foreground">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
            <span className="text-sm">Đang tải cấu hình hiện tại...</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="thresholdDays" className="font-medium text-foreground">
                Số ngày cảnh báo trước khi hết hạn{" "}
                <span className="text-destructive">*</span>
              </Label>
              <div className="relative">
                <Input
                  id="thresholdDays"
                  type="number"
                  min={1}
                  max={365}
                  value={days}
                  onChange={(e) => {
                    setDays(e.target.value);
                    if (errorText) setErrorText(null);
                  }}
                  placeholder="Nhập số ngày (mặc định 15)"
                  className="pr-16"
                  required
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground pointer-events-none">
                  ngày
                </span>
              </div>
              {errorText && (
                <p className="text-xs text-destructive mt-1 font-medium">
                  {errorText}
                </p>
              )}
            </div>

            <div className="rounded-lg bg-muted/60 p-3 text-xs text-muted-foreground space-y-1.5 border border-border/50">
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                <span>
                  Hệ thống sẽ kích hoạt cảnh báo và thông báo khi thời gian còn
                  lại của kết quả kiểm nghiệm <strong>&le; ngưỡng này</strong>{" "}
                  (ngay khi ghi nhận kiểm nghiệm và quét định kỳ 00:00 mỗi ngày).
                </span>
              </div>
              {configData?.updatedAt && (
                <div className="flex items-center gap-2 pt-1 border-t border-border/40 text-[11px]">
                  <Clock className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                  <span>
                    Cập nhật lần cuối:{" "}
                    <strong>{formatDateTime(configData.updatedAt)}</strong>
                    {configData.updatedByName && (
                      <> bởi <strong>{configData.updatedByName}</strong></>
                    )}
                  </span>
                </div>
              )}
            </div>

            <DialogFooter className="pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={submitting}
              >
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={submitting || loading}
                className="gap-2"
              >
                {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                Lưu cấu hình
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
};
