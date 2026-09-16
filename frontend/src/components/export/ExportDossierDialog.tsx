import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  FileText,
  Download,
  Eye,
  Loader2,
  FileCode,
  FileSpreadsheet,
} from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/hooks/useAuth';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';
import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { DossierPreviewDialog } from './DossierPreviewDialog';
import { getLocalDateString } from '@/utils/dateTime';
import { ProfileTemplateSelector } from './ProfileTemplateSelector';
import type { ProfileTemplate } from '@/types/profileTemplate';

interface ExportDossierDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  shipmentId: string;
  shipmentName: string;
  shipmentCode?: string;
}

export const ExportDossierDialog: React.FC<ExportDossierDialogProps> = ({
  open,
  onOpenChange,
  shipmentId,
  shipmentName,
  shipmentCode,
}) => {
  const { user } = useAuth();
  const organizationId = user?.organizationId || '';

  /**
   * Chỉ vai trò có quyền quản lý mẫu hồ sơ (VT-02) mới được chọn mẫu khi xuất.
   * VT-04 luôn dùng mẫu mặc định hệ thống (activeTemplateId = undefined).
   */
  const canSelectTemplate = usePermission(ROLE_ACCESS.profileTemplateManage);

  const [activeTemplateId, setActiveTemplateId] = useState<string | undefined>(undefined);
  const [activeTemplate, setActiveTemplate] = useState<ProfileTemplate | null>(null);
  const [selectedFormat, setSelectedFormat] = useState<'pdf' | 'json' | 'csv'>('pdf');
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [showPreview, setShowPreview] = useState<boolean>(false);

  // Đặt lại state khi mở hộp thoại
  useEffect(() => {
    if (open) {
      setSelectedFormat('pdf');
      setIsExporting(false);
      // Reset lựa chọn mẫu mỗi lần mở để tránh giữ state cũ giữa các lô
      setActiveTemplateId(undefined);
      setActiveTemplate(null);
    }
  }, [open]);

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
                        {/* Lựa chọn Mẫu hồ sơ — chỉ hiển thị cho VT-02 (có quyền profileTemplateManage) */}
            {canSelectTemplate && (
              <ProfileTemplateSelector
                organizationId={organizationId}
                open={open}
                onTemplateChange={(templateId, template) => {
                  setActiveTemplateId(templateId === 'default' ? undefined : templateId);
                  setActiveTemplate(template);
                }}
                disabled={isExporting}
                showInfoText
              />
            )}

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
              variant="ghost"
              size="sm"
              onClick={() => onOpenChange(false)}
              disabled={isExporting}
              className="w-full sm:w-auto text-xs"
            >
              Đóng
            </Button>

            <div className="flex items-center gap-2 w-full sm:w-auto">
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
