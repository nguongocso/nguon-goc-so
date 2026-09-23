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
import { Badge } from '@/components/ui/badge';
import {
  FileText,
  Download,
  Eye,
  Loader2,
  FileCode,
  FileSpreadsheet,
  CheckCircle2,
  ChevronDown,
} from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/hooks/useAuth';
import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { previewTemplatePdf } from '@/api/profileTemplateApi';
import { convertDossierDataToCsv } from '@/utils/dossierCsvConverter';
import { DossierPreviewDialog } from './DossierPreviewDialog';
import { getLocalDateString } from '@/utils/dateTime';
import { ProfileTemplateSelector } from './ProfileTemplateSelector';
import type { ProfileTemplate } from '@/types/profileTemplate';

export interface TemplatePreviewInfo {
  name: string;
  partnerName?: string;
  isDefault?: boolean;
  selectedFieldsCount: number;
  selectedFieldKeys?: string[];
  mockData?: Record<string, unknown>;
}

export interface ExportDossierDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  shipmentId?: string;
  shipmentName?: string;
  shipmentCode?: string;
  /**
   * UUID của tổ chức HTX sở hữu lô hàng.
   * Truyền vào khi VT-04 mở dialog — ProfileTemplateSelector sẽ dùng
   * orgId này thay vì orgId của người dùng để lấy mẫu của HTX.
   */
  cooperativeOrganizationId?: string;
  /**
   * Cờ bật chế độ xem trước mẫu hồ sơ trên trang cấu hình mẫu
   */
  templatePreviewMode?: boolean;
  /**
   * Thông tin mẫu hồ sơ đang được cấu hình
   */
  templateInfo?: TemplatePreviewInfo;
}

export const ExportDossierDialog: React.FC<ExportDossierDialogProps> = ({
  open,
  onOpenChange,
  shipmentId,
  shipmentName,
  shipmentCode,
  cooperativeOrganizationId,
  templatePreviewMode = false,
  templateInfo,
}) => {
  const { user } = useAuth();
  const organizationId = user?.organizationId || '';

  /**
   * orgId dùng để lấy mẫu hồ sơ:
   * - VT-02: dùng org của chính mình
   * - VT-04: dùng cooperativeOrganizationId (org của HTX sở hữu lô)
   */
  const templateOrgId = cooperativeOrganizationId ?? organizationId;

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
    if (templatePreviewMode && templateInfo?.mockData) {
      setIsExporting(true);
      const rawName = (templateInfo.name || 'mau_ho_so').replace(/\s+/g, '_');
      try {
        if (selectedFormat === 'json') {
          const jsonStr = JSON.stringify(templateInfo.mockData, null, 2);
          const blob = new Blob([jsonStr], { type: 'application/json;charset=utf-8;' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `dossier_profile_${rawName}_${getLocalDateString()}.json`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
          toast.success('Tải hồ sơ JSON mẫu thành công!');
        } else if (selectedFormat === 'csv') {
          const csvStr = convertDossierDataToCsv(templateInfo.mockData);
          const blob = new Blob([csvStr], { type: 'text/csv;charset=utf-8;' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `dossier_profile_${rawName}_${getLocalDateString()}.csv`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
          toast.success('Tải hồ sơ CSV mẫu thành công!');
        } else {
          // PDF: Tải tệp PDF thật kết xuất theo các trường đã cấu hình
          const blob = await previewTemplatePdf(organizationId, {
            name: templateInfo.name || 'Mẫu hồ sơ mới',
            partnerName: templateInfo.partnerName,
            selectedFieldKeys: templateInfo.selectedFieldKeys,
          });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `Ho_so_truy_xuat_${rawName}_${getLocalDateString()}.pdf`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
          toast.success('Tải hồ sơ PDF mẫu thành công!');
        }
        onOpenChange(false);
      } catch {
        toast.error('Có lỗi xảy ra khi tạo tệp mẫu xuất.');
      } finally {
        setIsExporting(false);
      }
      return;
    }

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
      const errObj = err as any;
      let errorMsg =
        errObj?.response?.data?.message ||
        (err instanceof Error && err.message && !err.message.includes('status code')
          ? err.message
          : null) ||
        errObj?.message ||
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
            {/* Lựa chọn Mẫu hồ sơ: Nếu ở templatePreviewMode thì hiển thị mẫu đang cấu hình */}
            {templatePreviewMode && templateInfo ? (
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="preview-template-name" className="text-sm font-semibold">
                    Mẫu hồ sơ áp dụng
                  </Label>
                  {templateInfo.partnerName && (
                    <Badge
                      variant="outline"
                      className="border-emerald-500/40 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300 text-[11px] font-semibold"
                    >
                      Đối tác: {templateInfo.partnerName}
                    </Badge>
                  )}
                </div>

                <div
                  id="preview-template-name"
                  aria-label="Mẫu hồ sơ áp dụng"
                  className="flex items-center justify-between w-full h-10 px-3 py-2 text-sm bg-background border rounded-md shadow-xs text-foreground"
                >
                  <div className="flex items-center gap-1.5 truncate">
                    <span className="font-semibold text-foreground">
                      {templateInfo.name || 'Mẫu hồ sơ mới'}
                    </span>
                    {templateInfo.partnerName && (
                      <span className="text-muted-foreground font-normal">
                        ({templateInfo.partnerName})
                      </span>
                    )}
                    {templateInfo.isDefault && (
                      <span className="text-[10px] bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300 px-1.5 py-0.5 rounded font-medium ml-1">
                        Mặc định
                      </span>
                    )}
                  </div>
                  <ChevronDown className="size-4 text-muted-foreground opacity-60" />
                </div>

                {/* Thông báo giải thích về mẫu đang cấu hình */}
                <div className="p-2.5 rounded-lg bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800/40 text-xs text-muted-foreground flex items-start gap-2">
                  <CheckCircle2 className="size-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                  <div>
                    <span>
                      Áp dụng mẫu <strong className="text-foreground">{templateInfo.name || 'Mẫu đang tạo'}</strong>
                      {templateInfo.partnerName ? ` thiết kế cho đối tác ${templateInfo.partnerName}` : ''}.
                      {' '}Hồ sơ xuất ra sẽ được lọc chính xác theo cấu hình{' '}
                      <strong className="text-emerald-700 dark:text-emerald-300 font-semibold">{templateInfo.selectedFieldsCount} trường đã chọn</strong>.
                    </span>
                  </div>
                </div>
              </div>
            ) : templateOrgId ? (
              <ProfileTemplateSelector
                organizationId={templateOrgId}
                open={open}
                onTemplateChange={(templateId, template) => {
                  setActiveTemplateId(templateId === 'default' ? undefined : templateId);
                  setActiveTemplate(template);
                }}
                disabled={isExporting}
                showInfoText
              />
            ) : null}

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
                disabled={isExporting || (!shipmentId && !templatePreviewMode)}
                className="w-full sm:w-auto gap-1.5 text-xs"
              >
                <Eye className="size-3.5" />
                Xem trước hồ sơ
              </Button>

              <Button
                type="button"
                size="sm"
                onClick={handleExport}
                disabled={isExporting || (!shipmentId && !templatePreviewMode)}
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
          shipmentName={
            shipmentName || shipmentCode || (templatePreviewMode ? 'SHIP-MOCK-2026-DEMO' : undefined)
          }
          templateId={activeTemplateId}
          templateName={
            templatePreviewMode
              ? (templateInfo?.name || 'Mẫu đang tạo')
              : (activeTemplate?.name || 'Mẫu mặc định')
          }
          activeFormat={selectedFormat}
          initialData={templatePreviewMode ? templateInfo?.mockData : undefined}
          selectedFieldKeys={templatePreviewMode ? templateInfo?.selectedFieldKeys : undefined}
          partnerName={templatePreviewMode ? templateInfo?.partnerName : activeTemplate?.partnerName}
        />
      )}
    </>
  );
};
