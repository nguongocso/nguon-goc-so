import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Loader2, Copy, Check, Eye, X, FileJson } from 'lucide-react';
import { toast } from 'sonner';
import { getOpenDataPreview } from '@/api/profileTemplateApi';

interface DossierPreviewDialogProps {
  open: boolean;
  onClose: () => void;
  shipmentId?: string;
  templateId?: string;
  templateName?: string;
  initialData?: Record<string, unknown> | null;
}

export const DossierPreviewDialog: React.FC<DossierPreviewDialogProps> = ({
  open,
  onClose,
  shipmentId,
  templateId,
  templateName,
  initialData,
}) => {
  const [data, setData] = useState<Record<string, unknown> | null>(initialData || null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (initialData) {
      setData(initialData);
      return;
    }

    if (open && shipmentId) {
      const fetchPreview = async () => {
        setLoading(true);
        try {
          const res = await getOpenDataPreview(shipmentId, templateId);
          setData(res);
        } catch (err: unknown) {
          const msg =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            'Không thể xem trước dữ liệu hồ sơ';
          toast.error(msg);
          setData(null);
        } finally {
          setLoading(false);
        }
      };
      fetchPreview();
    } else if (!open) {
      setData(null);
      setCopied(false);
    }
  }, [open, shipmentId, templateId, initialData]);

  const handleCopy = () => {
    if (!data) return;
    navigator.clipboard.writeText(JSON.stringify(data, null, 2));
    setCopied(true);
    toast.success('Đã sao chép cấu trúc JSON vào bộ nhớ tạm');
    setTimeout(() => setCopied(false), 2000);
  };

  const jsonString = data ? JSON.stringify(data, null, 2) : '';

  return (
    <Dialog open={open} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="max-w-3xl max-h-[85vh] flex flex-col p-6">
        <DialogHeader className="space-y-1.5">
          <div className="flex items-center justify-between pr-6">
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-lg bg-primary/10 text-primary">
                <FileJson className="size-5" />
              </div>
              <div>
                <DialogTitle className="text-lg font-bold text-foreground">
                  Xem trước hồ sơ truy xuất
                </DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground mt-0.5">
                  Dữ liệu xuất thực tế chỉ bao gồm các trường được cấu hình trong mẫu
                </DialogDescription>
              </div>
            </div>
            {templateName && (
              <Badge variant="outline" className="text-xs border-primary/30 text-primary">
                Mẫu: {templateName}
              </Badge>
            )}
          </div>
        </DialogHeader>

        {/* Content Box */}
        <div className="flex-1 min-h-[300px] max-h-[55vh] overflow-hidden my-2 rounded-xl border border-border bg-muted/40 flex flex-col">
          {loading ? (
            <div className="flex-1 flex flex-col items-center justify-center gap-2 p-8 text-muted-foreground">
              <Loader2 className="size-8 animate-spin text-primary" />
              <p className="text-sm">Đang tạo bản xem trước hồ sơ...</p>
            </div>
          ) : data ? (
            <div className="flex-1 overflow-auto p-4">
              <pre className="font-mono text-xs leading-relaxed text-foreground whitespace-pre-wrap break-all select-all">
                {jsonString}
              </pre>
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center gap-2 p-8 text-muted-foreground">
              <Eye className="size-8 stroke-1 text-muted-foreground/60" />
              <p className="text-sm">Không có dữ liệu xem trước</p>
              <p className="text-xs text-muted-foreground/80">
                Hãy chọn lô hàng hoặc lưu mẫu hồ sơ để tạo bản kết xuất
              </p>
            </div>
          )}
        </div>

        {/* Footer */}
        <DialogFooter className="sm:justify-between items-center gap-2 border-t pt-3">
          <div className="text-xs text-muted-foreground">
            {data && (
              <span>
                Kích thước: {Math.round(jsonString.length / 1024 * 10) / 10} KB | Định dạng: JSON
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            {data && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleCopy}
                className="gap-1.5"
              >
                {copied ? <Check className="size-3.5 text-primary" /> : <Copy className="size-3.5" />}
                <span>{copied ? 'Đã sao chép' : 'Sao chép JSON'}</span>
              </Button>
            )}
            <Button type="button" variant="outline" size="sm" onClick={onClose} className="gap-1">
              <X className="size-3.5" />
              <span>Đóng</span>
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
