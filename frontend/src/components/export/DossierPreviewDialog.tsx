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
import {
  Loader2,
  Copy,
  Check,
  Eye,
  X,
  FileJson,
  FileText,
  Building2,
  Package,
  Calendar,
  ShieldCheck,
  Clock,
  Sparkles,
  Layers,
} from 'lucide-react';
import { toast } from 'sonner';
import { getOpenDataPreview } from '@/api/profileTemplateApi';

interface PreviewOrganization {
  name?: string;
  code?: string;
  address?: string;
  phone?: string;
  email?: string;
}

interface PreviewFarmArea {
  name?: string;
  area?: number;
  areaUnit?: string;
}

interface PreviewProductionLot {
  name?: string;
  productCategory?: string;
  plantingDate?: string;
  harvestDate?: string;
  expectedQuantity?: number;
  actualQuantity?: number;
  status?: string;
}

interface PreviewShipment {
  name?: string;
  totalQuantity?: number;
  packagingInfo?: string;
  status?: string;
}

interface PreviewFarmLog {
  executedDate?: string;
  activityType?: string;
  material?: string;
  quantity?: number;
  notes?: string;
}

interface PreviewInspection {
  sampleSentDate?: string;
  inspectionUnit?: string;
  status?: string;
}

interface PreviewTimelineEvent {
  recordedAt?: string;
  eventType?: string;
  recordedBy?: string;
}

interface PreviewAppliedTemplate {
  templateId?: string;
  templateName?: string;
  isDefault?: boolean;
  totalFields?: number;
}

interface DossierPreviewDialogProps {
  open: boolean;
  onClose: () => void;
  shipmentId?: string;
  templateId?: string;
  templateName?: string;
  activeFormat?: 'pdf' | 'json' | 'csv';
  initialData?: Record<string, unknown> | null;
}

export const DossierPreviewDialog: React.FC<DossierPreviewDialogProps> = ({
  open,
  onClose,
  shipmentId,
  templateId,
  templateName,
  activeFormat = 'pdf',
  initialData,
}) => {
  const [data, setData] = useState<Record<string, unknown> | null>(initialData || null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [activeTab, setActiveTab] = useState<'document' | 'json'>(
    activeFormat === 'json' ? 'json' : 'document'
  );

  useEffect(() => {
    if (activeFormat === 'json') {
      setActiveTab('json');
    } else {
      setActiveTab('document');
    }
  }, [activeFormat, open]);

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

  // Trích xuất các trường dữ liệu từ preview với kiểu rõ ràng
  const org = (data?.organization as PreviewOrganization) || null;
  const farmArea = (data?.farmArea as PreviewFarmArea) || null;
  const lot = (data?.productionLot as PreviewProductionLot) || null;
  const shipment = (data?.shipment as PreviewShipment) || null;
  const farmLogs = Array.isArray(data?.farmLogs) ? (data.farmLogs as PreviewFarmLog[]) : [];
  const inspections = Array.isArray(data?.inspections)
    ? (data.inspections as PreviewInspection[])
    : [];
  const timelineEvents = Array.isArray(data?.timelineEvents)
    ? (data.timelineEvents as PreviewTimelineEvent[])
    : [];
  const appliedTemplate = (data?.appliedTemplate as PreviewAppliedTemplate) || null;

  return (
    <Dialog open={open} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="max-w-4xl max-h-[90vh] flex flex-col p-6">
        <DialogHeader className="space-y-1.5 pb-2 border-b">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pr-6">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400">
                <FileText className="size-5" />
              </div>
              <div>
                <DialogTitle className="text-lg font-bold text-foreground">
                  Xem trước hồ sơ truy xuất nguồn gốc
                </DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground mt-0.5">
                  Nội dung hiển thị được lọc chính xác theo mẫu hồ sơ đã chọn
                </DialogDescription>
              </div>
            </div>

            {/* Toggle tabs */}
            <div className="flex items-center gap-1 bg-muted/60 p-1 rounded-lg border text-xs">
              <button
                type="button"
                onClick={() => setActiveTab('document')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${
                  activeTab === 'document'
                    ? 'bg-background text-foreground shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <FileText className="size-3.5 text-emerald-600" />
                <span>Bản in tài liệu / PDF</span>
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('json')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md font-medium transition-all ${
                  activeTab === 'json'
                    ? 'bg-background text-foreground shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <FileJson className="size-3.5 text-blue-600" />
                <span>Dữ liệu JSON</span>
              </button>
            </div>
          </div>
        </DialogHeader>

        {/* Content Box */}
        <div className="flex-1 min-h-[350px] max-h-[62vh] overflow-y-auto my-2 rounded-xl border border-border bg-slate-50/50 dark:bg-slate-950/30 p-4">
          {loading ? (
            <div className="h-full min-h-[300px] flex flex-col items-center justify-center gap-2 p-8 text-muted-foreground">
              <Loader2 className="size-8 animate-spin text-emerald-600" />
              <p className="text-sm font-medium">Đang khởi tạo bản xem trước hồ sơ...</p>
            </div>
          ) : !data ? (
            <div className="h-full min-h-[300px] flex flex-col items-center justify-center gap-2 p-8 text-muted-foreground">
              <Eye className="size-8 stroke-1 text-muted-foreground/60" />
              <p className="text-sm">Không có dữ liệu xem trước</p>
              <p className="text-xs text-muted-foreground/80">
                Hãy kiểm tra thông tin lô hàng hoặc cấu hình mẫu hồ sơ
              </p>
            </div>
          ) : activeTab === 'json' ? (
            // TAB DỮ LIỆU JSON
            <div className="rounded-lg bg-card p-4 border overflow-auto">
              <pre className="font-mono text-xs leading-relaxed text-foreground whitespace-pre-wrap break-all select-all">
                {jsonString}
              </pre>
            </div>
          ) : (
            // TAB BẢN TÀI LIỆU TRỰC QUAN (DOCUMENT / PDF PREVIEW)
            <div className="max-w-3xl mx-auto bg-card p-6 sm:p-8 rounded-xl border shadow-xs space-y-6 text-foreground">
              {/* Tiêu đề Quốc hiệu & Văn bản */}
              <div className="text-center space-y-1.5 pb-4 border-b">
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
                </p>
                <p className="text-xs italic text-muted-foreground underline decoration-1 underline-offset-4">
                  Độc lập - Tự do - Hạnh phúc
                </p>
                <div className="pt-3">
                  <h2 className="text-base sm:text-lg font-bold text-emerald-800 dark:text-emerald-400 uppercase tracking-wide">
                    HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM
                  </h2>
                  <div className="flex flex-wrap items-center justify-center gap-2 mt-2">
                    {Boolean(appliedTemplate?.templateName) && (
                      <Badge variant="outline" className="text-xs border-emerald-300 text-emerald-700 bg-emerald-50/70">
                        <Sparkles className="size-3 mr-1 text-emerald-600" />
                        Mẫu: {appliedTemplate?.templateName}
                      </Badge>
                    )}
                    {Boolean(templateName) && !appliedTemplate?.templateName && (
                      <Badge variant="outline" className="text-xs border-emerald-300 text-emerald-700 bg-emerald-50/70">
                        Mẫu: {templateName}
                      </Badge>
                    )}
                    {Boolean(shipmentId) && (
                      <span className="text-xs text-muted-foreground">
                        Mã lô hàng: <strong className="text-foreground">{shipmentId}</strong>
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* I. THÔNG TIN ĐƠN VỊ SẢN XUẤT & LÔ SẢN XUẤT */}
              {(org || lot || farmArea) && (
                <div className="space-y-2.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase text-emerald-700 dark:text-emerald-400">
                    <Building2 className="size-4" />
                    <span>I. THÔNG TIN LÔ SẢN XUẤT & ĐƠN VỊ CANH TÁC</span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2 text-xs p-3 rounded-lg bg-slate-50 dark:bg-slate-900/50 border">
                    {Boolean(lot?.name) && (
                      <div>
                        <span className="text-muted-foreground">Tên lô sản xuất:</span>{' '}
                        <strong className="text-foreground">{lot?.name}</strong>
                      </div>
                    )}
                    {Boolean(lot?.productCategory) && (
                      <div>
                        <span className="text-muted-foreground">Danh mục sản phẩm:</span>{' '}
                        <strong className="text-foreground">{lot?.productCategory}</strong>
                      </div>
                    )}
                    {Boolean(org?.name) && (
                      <div>
                        <span className="text-muted-foreground">Đơn vị sản xuất (HTX):</span>{' '}
                        <strong className="text-foreground">{org?.name}</strong>
                      </div>
                    )}
                    {Boolean(org?.code) && (
                      <div>
                        <span className="text-muted-foreground">Mã HTX:</span>{' '}
                        <strong className="text-foreground">{org?.code}</strong>
                      </div>
                    )}
                    {Boolean(org?.address) && (
                      <div className="sm:col-span-2">
                        <span className="text-muted-foreground">Địa chỉ:</span>{' '}
                        <span className="text-foreground">{org?.address}</span>
                      </div>
                    )}
                    {Boolean(farmArea?.name) && (
                      <div>
                        <span className="text-muted-foreground">Vùng trồng:</span>{' '}
                        <span className="text-foreground">
                          {farmArea?.name}
                          {farmArea?.area !== undefined && ` (${farmArea.area} ${farmArea.areaUnit || ''})`}
                        </span>
                      </div>
                    )}
                    {Boolean(lot?.plantingDate) && (
                      <div>
                        <span className="text-muted-foreground">Ngày xuống giống:</span>{' '}
                        <span className="text-foreground">{lot?.plantingDate}</span>
                      </div>
                    )}
                    {Boolean(lot?.harvestDate) && (
                      <div>
                        <span className="text-muted-foreground">Ngày thu hoạch:</span>{' '}
                        <span className="text-foreground">{lot?.harvestDate}</span>
                      </div>
                    )}
                    {lot?.expectedQuantity !== undefined && (
                      <div>
                        <span className="text-muted-foreground">Sản lượng dự kiến:</span>{' '}
                        <span className="text-foreground">{lot.expectedQuantity}</span>
                      </div>
                    )}
                    {lot?.actualQuantity !== undefined && (
                      <div>
                        <span className="text-muted-foreground">Sản lượng thực tế:</span>{' '}
                        <span className="text-foreground">{lot.actualQuantity} kg</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* II. THÔNG TIN LÔ HÀNG VẬN CHUYỂN */}
              {shipment && (
                <div className="space-y-2.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase text-emerald-700 dark:text-emerald-400">
                    <Package className="size-4" />
                    <span>II. THÔNG TIN LÔ HÀNG VẬN CHUYỂN</span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2 text-xs p-3 rounded-lg bg-slate-50 dark:bg-slate-900/50 border">
                    {Boolean(shipment?.name) && (
                      <div>
                        <span className="text-muted-foreground">Tên lô hàng:</span>{' '}
                        <strong className="text-foreground">{shipment?.name}</strong>
                      </div>
                    )}
                    {shipment?.totalQuantity !== undefined && (
                      <div>
                        <span className="text-muted-foreground">Số lượng:</span>{' '}
                        <strong className="text-foreground">{shipment.totalQuantity} sản phẩm</strong>
                      </div>
                    )}
                    {Boolean(shipment?.packagingInfo) && (
                      <div>
                        <span className="text-muted-foreground">Quy cách đóng gói:</span>{' '}
                        <span className="text-foreground">{shipment?.packagingInfo}</span>
                      </div>
                    )}
                    {Boolean(shipment?.status) && (
                      <div>
                        <span className="text-muted-foreground">Trạng thái:</span>{' '}
                        <Badge variant="secondary" className="text-[10px]">
                          {shipment?.status}
                        </Badge>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* III. LỊCH TRÌNH CANH TÁC & CHỨNG TỪ */}
              {farmLogs.length > 0 && (
                <div className="space-y-2.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase text-emerald-700 dark:text-emerald-400">
                    <Calendar className="size-4" />
                    <span>III. LỊCH TRÌNH CANH TÁC & CHỨNG TỪ ({farmLogs.length} ghi chép)</span>
                  </div>
                  <div className="overflow-x-auto border rounded-lg">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-100 dark:bg-slate-900 text-muted-foreground font-semibold border-b">
                        <tr>
                          <th className="p-2 w-10 text-center">STT</th>
                          <th className="p-2">Ngày thực hiện</th>
                          <th className="p-2">Hoạt động</th>
                          <th className="p-2">Vật tư / Số lượng</th>
                          <th className="p-2">Ghi chú</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        {farmLogs.map((log, idx) => (
                          <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/30">
                            <td className="p-2 text-center text-muted-foreground">{idx + 1}</td>
                            <td className="p-2 whitespace-nowrap">{log.executedDate || '—'}</td>
                            <td className="p-2 font-medium">{log.activityType || '—'}</td>
                            <td className="p-2">
                              {log.material
                                ? `${log.material}${log.quantity ? ` (${log.quantity})` : ''}`
                                : '—'}
                            </td>
                            <td className="p-2 text-muted-foreground">{log.notes || '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* IV. LỊCH SỬ KIỂM NGHIỆM */}
              {inspections.length > 0 && (
                <div className="space-y-2.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase text-emerald-700 dark:text-emerald-400">
                    <ShieldCheck className="size-4" />
                    <span>IV. LỊCH SỬ KIỂM NGHIỆM ({inspections.length} hồ sơ)</span>
                  </div>
                  <div className="overflow-x-auto border rounded-lg">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-100 dark:bg-slate-900 text-muted-foreground font-semibold border-b">
                        <tr>
                          <th className="p-2 w-10 text-center">STT</th>
                          <th className="p-2">Ngày gửi mẫu</th>
                          <th className="p-2">Đơn vị kiểm nghiệm</th>
                          <th className="p-2">Kết quả / Trạng thái</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        {inspections.map((insp, idx) => (
                          <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/30">
                            <td className="p-2 text-center text-muted-foreground">{idx + 1}</td>
                            <td className="p-2 whitespace-nowrap">{insp.sampleSentDate || '—'}</td>
                            <td className="p-2 font-medium">{insp.inspectionUnit || '—'}</td>
                            <td className="p-2">
                              <Badge variant="outline" className="text-[10px] text-emerald-600 border-emerald-300">
                                {insp.status || 'Đạt tiêu chuẩn'}
                              </Badge>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* V. DÒNG SỰ KIỆN CHUỖI CUNG ỨNG */}
              {timelineEvents.length > 0 && (
                <div className="space-y-2.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase text-emerald-700 dark:text-emerald-400">
                    <Clock className="size-4" />
                    <span>V. DÒNG SỰ KIỆN CHUỖI CUNG ỨNG ({timelineEvents.length} sự kiện)</span>
                  </div>
                  <div className="overflow-x-auto border rounded-lg">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-100 dark:bg-slate-900 text-muted-foreground font-semibold border-b">
                        <tr>
                          <th className="p-2 w-10 text-center">STT</th>
                          <th className="p-2">Thời điểm ghi nhận</th>
                          <th className="p-2">Loại sự kiện</th>
                          <th className="p-2">Người ghi nhận</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        {timelineEvents.map((ev, idx) => (
                          <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/30">
                            <td className="p-2 text-center text-muted-foreground">{idx + 1}</td>
                            <td className="p-2 whitespace-nowrap">{ev.recordedAt || '—'}</td>
                            <td className="p-2 font-medium">{ev.eventType || '—'}</td>
                            <td className="p-2 text-muted-foreground">{ev.recordedBy || 'Hệ thống'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <DialogFooter className="sm:justify-between items-center gap-2 border-t pt-3">
          <div className="text-xs text-muted-foreground flex items-center gap-2">
            {data && (
              <>
                <span className="flex items-center gap-1">
                  <Layers className="size-3.5 text-muted-foreground" />
                  {activeTab === 'document' ? 'Chế độ mô phỏng bản in PDF' : `Kích thước: ${Math.round((jsonString.length / 1024) * 10) / 10} KB`}
                </span>
              </>
            )}
          </div>
          <div className="flex items-center gap-2">
            {data && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleCopy}
                className="gap-1.5 text-xs"
              >
                {copied ? <Check className="size-3.5 text-emerald-600" /> : <Copy className="size-3.5" />}
                <span>{copied ? 'Đã sao chép' : 'Sao chép JSON'}</span>
              </Button>
            )}
            <Button type="button" variant="outline" size="sm" onClick={onClose} className="gap-1 text-xs">
              <X className="size-3.5" />
              <span>Đóng</span>
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
