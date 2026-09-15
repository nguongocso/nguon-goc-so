import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  FileText,
  Download,
  Eye,
  Loader2,
  CheckCircle2,
  FileCode,
  FileSpreadsheet,
  Sparkles,
} from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/hooks/useAuth';
import { useProfileTemplates } from '@/hooks/useProfileTemplates';
import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { DossierPreviewDialog } from './DossierPreviewDialog';
import { getLocalDateString } from '@/utils/dateTime';

interface ExportDossierDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  shipmentId: string;
  shipmentName: string;
  shipmentCode?: string;
}

/** Tên hiển thị tiếng Việt của mẫu hồ sơ mặc định do hệ thống cung cấp */
const DEFAULT_TEMPLATE_DISPLAY_NAME = 'Mẫu tiêu chuẩn HTX (Mặc định hệ thống)';

interface TemplateOptionContentProps {
  name: string;
  partnerName?: string | null;
  isDefault?: boolean;
}

/**
 * Hiển thị nội dung của một mẫu hồ sơ gồm tên mẫu, đối tác và nhãn "Mặc định".
 * Dùng chung cho cả danh sách lựa chọn và giá trị đang được chọn
 * để câu chữ hiển thị luôn thống nhất, không hiển thị mã (id) của mẫu.
 */
const TemplateOptionContent: React.FC<TemplateOptionContentProps> = ({
  name,
  partnerName,
  isDefault,
}) => (
  <div className="flex min-w-0 items-center gap-2">
    <span className="truncate">{name}</span>
    {partnerName ? (
      <span className="shrink-0 text-xs text-muted-foreground">({partnerName})</span>
    ) : null}
    {isDefault ? (
      <span className="shrink-0 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-medium text-amber-800 dark:bg-amber-950 dark:text-amber-300">
        Mặc định
      </span>
    ) : null}
  </div>
);

export const ExportDossierDialog: React.FC<ExportDossierDialogProps> = ({
  open,
  onOpenChange,
  shipmentId,
  shipmentName,
  shipmentCode,
}) => {
  const { user } = useAuth();
  const organizationId = user?.organizationId || '';
  const { templates, refresh, loading: loadingTemplates } = useProfileTemplates(organizationId);

  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('default');
  const [selectedFormat, setSelectedFormat] = useState<'pdf' | 'json' | 'csv'>('pdf');
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [showPreview, setShowPreview] = useState<boolean>(false);

  // Tải danh sách mẫu khi mở dialog
  useEffect(() => {
    if (open && organizationId) {
      refresh();
    }
  }, [open, organizationId, refresh]);

  // Đặt lại state khi mở hộp thoại
  useEffect(() => {
    if (open) {
      // Tìm mẫu mặc định nếu có
      const defaultTpl = templates.find((t) => t.isDefault);
      if (defaultTpl) {
        setSelectedTemplateId(defaultTpl.id);
      } else {
        setSelectedTemplateId('default');
      }
      setSelectedFormat('pdf');
      setIsExporting(false);
    }
  }, [open, templates]);

  const activeTemplate = templates.find((t) => t.id === selectedTemplateId);
  const activeTemplateId = selectedTemplateId !== 'default' ? selectedTemplateId : undefined;

  // Giá trị hiển thị trên ô chọn: luôn dùng tên mẫu tiếng Việt thay vì mã (id) của mẫu
  const selectedTemplateContent = activeTemplate ? (
    <TemplateOptionContent
      name={activeTemplate.name}
      partnerName={activeTemplate.partnerName}
      isDefault={activeTemplate.isDefault}
    />
  ) : (
    <span className="truncate font-medium">{DEFAULT_TEMPLATE_DISPLAY_NAME}</span>
  );

  const handleExport = async () => {
    if (!shipmentId) return;

    setIsExporting(true);
    const toastId = toast.loading('Đang khởi tạo tệp hồ sơ truy xuất...');

    try {
      let blob: Blob;
      let extension = selectedFormat;
      let fileName = `Ho_so_truy_xuat_${shipmentName || shipmentCode || shipmentId}_${getLocalDateString()}.${extension}`;

      if (selectedFormat === 'pdf') {
        blob = await exportDossier(shipmentId, activeTemplateId);
      } else {
        blob = await exportShipmentWithTemplate(shipmentId, activeTemplateId, selectedFormat);
        fileName = `dossier_profile_${shipmentCode || shipmentId}_${getLocalDateString()}.${selectedFormat}`;
      }

      toast.dismiss(toastId);

      // Kích hoạt tải tệp
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success('Tải hồ sơ truy xuất thành công!');
      onOpenChange(false);
    } catch (err: unknown) {
      toast.dismiss(toastId);
      const errorMsg =
        (err as { message?: string; response?: { data?: { message?: string } } })?.message ||
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Có lỗi xảy ra khi tạo hồ sơ xuất.';
      toast.error(errorMsg);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader className="space-y-1">
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400">
                <FileText className="size-5" />
              </div>
              <div>
                <DialogTitle className="text-lg font-bold">Xuất hồ sơ truy xuất nguồn gốc</DialogTitle>
              </div>
            </div>
          </DialogHeader>

          <div className="space-y-5 py-3">
            {/* Lựa chọn Mẫu hồ sơ */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="template-select" className="text-sm font-semibold flex items-center gap-1.5">
                  <Sparkles className="size-4 text-emerald-600" />
                  Mẫu hồ sơ áp dụng
                </Label>
                {activeTemplate?.partnerName && (
                  <Badge variant="outline" className="text-xs border-emerald-300 text-emerald-700 bg-emerald-50/60">
                    Đối tác: {activeTemplate.partnerName}
                  </Badge>
                )}
              </div>

              <Select
                value={selectedTemplateId}
                onValueChange={(val) => setSelectedTemplateId(val || 'default')}
                disabled={loadingTemplates || isExporting}
              >
                <SelectTrigger id="template-select" className="w-full">
                  <SelectValue placeholder="Chọn mẫu hồ sơ truy xuất">
                    {selectedTemplateContent}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="default" label={DEFAULT_TEMPLATE_DISPLAY_NAME}>
                    <span className="font-medium">{DEFAULT_TEMPLATE_DISPLAY_NAME}</span>
                  </SelectItem>
                  {templates.map((tpl) => (
                    <SelectItem
                      key={tpl.id}
                      value={tpl.id}
                      label={`${tpl.name}${tpl.partnerName ? ` (${tpl.partnerName})` : ''}${tpl.isDefault ? ' — Mặc định' : ''}`}
                    >
                      <TemplateOptionContent
                        name={tpl.name}
                        partnerName={tpl.partnerName}
                        isDefault={tpl.isDefault}
                      />
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {/* Thông tin giải thích về mẫu được chọn */}
              <div className="p-2.5 rounded-lg bg-slate-50 dark:bg-slate-900/50 border text-xs text-muted-foreground flex items-start gap-2">
                <CheckCircle2 className="size-4 text-emerald-500 shrink-0 mt-0.5" />
                <div>
                  {selectedTemplateId === 'default' ? (
                    <span>
                      Áp dụng biểu mẫu mặc định của HTX gồm đầy đủ các trường bắt buộc và toàn bộ thông tin sản xuất, canh tác, kiểm nghiệm.
                    </span>
                  ) : (
                    <span>
                      Áp dụng mẫu <strong className="text-foreground">{activeTemplate?.name}</strong>
                      {activeTemplate?.partnerName ? ` thiết kế cho đối tác ${activeTemplate.partnerName}` : ''}.
                      Hồ sơ xuất ra sẽ được lọc chính xác theo cấu hình {activeTemplate?.fields?.length || 0} trường đã chọn.
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Lựa chọn Định dạng tệp */}
            <div className="space-y-2">
              <Label className="text-sm font-semibold">Định dạng tệp xuất</Label>
              <div className="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  disabled={isExporting}
                  onClick={() => setSelectedFormat('pdf')}
                  className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between ${selectedFormat === 'pdf'
                    ? 'border-emerald-600 bg-emerald-50/50 dark:bg-emerald-950/20 ring-1 ring-emerald-600'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-800'
                    }`}
                >
                  <FileText className={`size-5 mb-1.5 ${selectedFormat === 'pdf' ? 'text-emerald-600' : 'text-slate-500'}`} />
                  <div>
                    <div className="font-semibold text-xs text-foreground">Hồ sơ PDF</div>
                    <div className="text-[10px] text-muted-foreground">In ấn & nộp đối tác</div>
                  </div>
                </button>

                <button
                  type="button"
                  disabled={isExporting}
                  onClick={() => setSelectedFormat('json')}
                  className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between ${selectedFormat === 'json'
                    ? 'border-emerald-600 bg-emerald-50/50 dark:bg-emerald-950/20 ring-1 ring-emerald-600'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-800'
                    }`}
                >
                  <FileCode className={`size-5 mb-1.5 ${selectedFormat === 'json' ? 'text-emerald-600' : 'text-slate-500'}`} />
                  <div>
                    <div className="font-semibold text-xs text-foreground">Dữ liệu JSON</div>
                    <div className="text-[10px] text-muted-foreground">Tích hợp phần mềm</div>
                  </div>
                </button>

                <button
                  type="button"
                  disabled={isExporting}
                  onClick={() => setSelectedFormat('csv')}
                  className={`p-2.5 rounded-lg border text-left transition-all flex flex-col justify-between ${selectedFormat === 'csv'
                    ? 'border-emerald-600 bg-emerald-50/50 dark:bg-emerald-950/20 ring-1 ring-emerald-600'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-800'
                    }`}
                >
                  <FileSpreadsheet className={`size-5 mb-1.5 ${selectedFormat === 'csv' ? 'text-emerald-600' : 'text-slate-500'}`} />
                  <div>
                    <div className="font-semibold text-xs text-foreground">Bảng tính CSV</div>
                    <div className="text-[10px] text-muted-foreground">Phân tích số liệu</div>
                  </div>
                </button>
              </div>
            </div>
          </div>

          <DialogFooter className="flex flex-col sm:flex-row items-center justify-between gap-2 pt-2 border-t">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setShowPreview(true)}
              disabled={isExporting || !shipmentId}
              className="w-full sm:w-auto gap-1.5 text-xs"
            >
              <Eye className="size-3.5" />
              Xem trước hồ sơ
            </Button>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => onOpenChange(false)}
                disabled={isExporting}
                className="w-full sm:w-auto text-xs"
              >
                Đóng
              </Button>

              <Button
                type="button"
                size="sm"
                onClick={handleExport}
                disabled={isExporting || !shipmentId}
                className="w-full sm:w-auto gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold"
              >
                {isExporting ? (
                  <>
                    <Loader2 className="size-3.5 animate-spin" />
                    Đang xuất...
                  </>
                ) : (
                  <>
                    <Download className="size-3.5" />
                    Tải hồ sơ về máy
                  </>
                )}
              </Button>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Modal xem trước hồ sơ */}
      {showPreview && (
        <DossierPreviewDialog
          open={showPreview}
          onClose={() => setShowPreview(false)}
          shipmentId={shipmentId}
          shipmentName={shipmentName || shipmentCode}
          templateId={activeTemplateId}
          templateName={activeTemplate?.name || 'Mẫu mặc định'}
          activeFormat={selectedFormat}
        />
      )}
    </>
  );
};
