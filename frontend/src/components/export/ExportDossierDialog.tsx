import React, { useState, useEffect } from 'react';
import { FileText, Download, Eye, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/hooks/useAuth';
import { DossierPreviewDialog } from './DossierPreviewDialog';
import { DossierFormatSelector, type DossierExportFormat } from './DossierFormatSelector';
import { ProfileTemplateSelector } from './ProfileTemplateSelector';
import {
  TemplatePreviewSummary,
  type TemplatePreviewInfo,
} from './TemplatePreviewSummary';

import { exportDossier } from '@/api/dossierApi';
import { exportShipmentWithTemplate } from '@/api/exportApi';
import { previewTemplatePdf } from '@/api/profileTemplateApi';
import { toApiError } from '@/api/apiError';
import { convertDossierDataToCsv } from '@/utils/dossierCsvConverter';
import { getLocalDateString } from '@/utils/dateTime';
import { cn } from '@/lib/utils';
import type { ProfileTemplate } from '@/types/profileTemplate';

/** Thuộc tính truyền vào thành phần ExportDossierDialog. */
export interface ExportDossierDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  shipmentId?: string;
  shipmentName?: string;
  shipmentCode?: string;
  cooperativeOrganizationId?: string;
  templatePreviewMode?: boolean;
  templateInfo?: TemplatePreviewInfo;
}

/** Kích hoạt tải Blob xuống thiết bị với tên tệp được chỉ định. */
function downloadBlob(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/** Xuất hồ sơ PDF, JSON hoặc CSV theo mẫu của tổ chức hay mẫu mặc định. */
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
  const templateOrgId: string = cooperativeOrganizationId ?? organizationId;

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
    if (templatePreviewMode && templateInfo?.mockData) {
      setIsExporting(true);
      const rawName = (templateInfo.name || 'mau_ho_so').replace(/\s+/g, '_');

      try {
        let blob: Blob;
        if (selectedFormat === 'pdf') {
          const activeOrgId: string = templateOrgId ?? organizationId;
          blob = await previewTemplatePdf(activeOrgId, {
            name: templateInfo.name || 'Mẫu hồ sơ mới',
            partnerName: templateInfo.partnerName,
            selectedFieldKeys: templateInfo.selectedFieldKeys,
          });
        } else if (selectedFormat === 'csv') {
          const csv = convertDossierDataToCsv(templateInfo.mockData);
          blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        } else {
          const json = JSON.stringify(templateInfo.mockData, null, 2);
          blob = new Blob([json], { type: 'application/json;charset=utf-8;' });
        }

        const prefix = selectedFormat === 'pdf' ? 'Ho_so_truy_xuat' : 'dossier_profile';
        downloadBlob(
          blob,
          `${prefix}_${rawName}_${getLocalDateString()}.${selectedFormat}`,
        );
        toast.success(`Tải hồ sơ ${selectedFormat.toUpperCase()} mẫu thành công!`);
        onOpenChange(false);
      } catch (err: unknown) {
        toast.error(toApiError(err, 'Có lỗi xảy ra khi tạo tệp mẫu xuất.').message);
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
      const shipmentIdentifier = shipmentName || shipmentCode || shipmentId;
      let fileName = `Ho_so_truy_xuat_${shipmentIdentifier}_${getLocalDateString()}.${selectedFormat}`;

      if (selectedFormat === 'pdf') {
        blob = await exportDossier(shipmentId, activeTemplateId);
      } else {
        blob = await exportShipmentWithTemplate(shipmentId, activeTemplateId, selectedFormat);
        fileName = `dossier_profile_${shipmentCode || shipmentId}_${getLocalDateString()}.${selectedFormat}`;
      }
      toast.dismiss(toastId);
      downloadBlob(blob, fileName);
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
            {templatePreviewMode && templateInfo ? (
              <TemplatePreviewSummary info={templateInfo} />
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
          shipmentName={
            shipmentName || shipmentCode || (templatePreviewMode ? 'SHIP-MOCK-2026-DEMO' : undefined)
          }
          templateId={activeTemplateId}
          organizationId={templateOrgId}
          templateName={
            templatePreviewMode
              ? templateInfo?.name || 'Mẫu đang tạo'
              : activeTemplate?.name || 'Mẫu mặc định'
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
