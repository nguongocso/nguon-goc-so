import React, { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { FileText, Download, Eye, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/hooks/useAuth';
import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { toApiError } from '@/api/apiError';
import { DossierPreviewDialog } from './DossierPreviewDialog';
import { DossierFormatSelector, type DossierExportFormat } from './DossierFormatSelector';
import { getLocalDateString } from '@/utils/dateTime';
import { cn } from '@/lib/utils';
import { ProfileTemplateSelector } from './ProfileTemplateSelector';
import type { ProfileTemplate } from '@/types/profileTemplate';

/** Props truyền vào component ExportDossierDialog */
export interface ExportDossierDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  shipmentId: string;
  shipmentName: string;
  shipmentCode?: string;
  cooperativeOrganizationId?: string;
}

/** Xuất hồ sơ PDF, JSON hoặc CSV theo mẫu của tổ chức hay mẫu mặc định. */
export const ExportDossierDialog: React.FC<ExportDossierDialogProps> = ({
  open,
  onOpenChange,
  shipmentId,
  shipmentName,
  shipmentCode,
  cooperativeOrganizationId,
}) => {
  const { user } = useAuth();
  const organizationId = user?.organizationId || '';
  const templateOrgId = cooperativeOrganizationId ?? organizationId;

  const [activeTemplateId, setActiveTemplateId] = useState<string | undefined>(undefined);
  const [activeTemplate, setActiveTemplate] = useState<ProfileTemplate | null>(null);
  const [selectedFormat, setSelectedFormat] = useState<DossierExportFormat>('pdf');
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [showPreview, setShowPreview] = useState<boolean>(false);

  useEffect(() => {
    if (open) {
      setSelectedFormat('pdf');
      setIsExporting(false);
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
      const shipmentIdentifier = shipmentName || shipmentCode || shipmentId;
      let fileName = `Ho_so_truy_xuat_${shipmentIdentifier}_${getLocalDateString()}.${selectedFormat}`;

      if (selectedFormat === 'pdf') {
        blob = await exportDossier(shipmentId, activeTemplateId);
      } else {
        blob = await exportShipmentWithTemplate(shipmentId, activeTemplateId, selectedFormat);
        fileName = `dossier_profile_${shipmentCode || shipmentId}_${getLocalDateString()}.${selectedFormat}`;
      }
      toast.dismiss(toastId);
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
      const errorMsg = toApiError(err, 'Có lỗi xảy ra khi tạo hồ sơ xuất.').message;
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
              <div
                className={cn(
                  'p-2 rounded-lg bg-emerald-50 text-emerald-600',
                  'dark:bg-emerald-950/40 dark:text-emerald-400',
                )}
              >
                <FileText className="size-5" />
              </div>
              <div>
                <DialogTitle className="text-lg font-bold">
                  Xuất hồ sơ truy xuất nguồn gốc
                </DialogTitle>
              </div>
            </div>
          </DialogHeader>

          <div className="space-y-5 py-3">
            {templateOrgId && (
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
            )}

            <DossierFormatSelector
              selectedFormat={selectedFormat}
              onSelectFormat={setSelectedFormat}
              disabled={isExporting}
            />
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
                className={cn(
                  'w-full sm:w-auto gap-1.5 bg-emerald-600 hover:bg-emerald-700',
                  'text-white text-xs font-semibold',
                )}
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
