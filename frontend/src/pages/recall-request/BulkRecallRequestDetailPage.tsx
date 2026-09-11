import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { LoaderCircle, XCircle, AlertTriangle, Package, ClipboardCheck, ExternalLink, FileText } from 'lucide-react';
import {
  approveBulkRecallRequest,
  getBulkRecallRequest,
  getEvidenceDownloadUrl,
  rejectBulkRecallRequest,
} from '@/api/recallApi';
import { useAuth } from '@/hooks/useAuth';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { CloseBulkRecallDialog, RESOLUTION_LABEL } from './CloseBulkRecallDialog';
import type { BulkRecallRequest, BulkRecallShipmentItem } from '@/types/bulkRecall';

const STATUS_MAP: Record<string, { label: string; tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }> = {
  PENDING: { label: 'Chờ duyệt', tone: 'warning' },
  APPROVED: { label: 'Đã duyệt', tone: 'info' },
  COMPLETED: { label: 'Đã xử lý', tone: 'success' },
  REJECTED: { label: 'Đã từ chối', tone: 'danger' },
};

const SHIPMENT_STATUS_MAP: Record<string, { label: string; tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }> = {
  ACTIVATED: { label: 'Đã kích hoạt', tone: 'success' },
  DRAFT: { label: 'Dự thảo', tone: 'neutral' },
  RECALLING: { label: 'Đang thu hồi', tone: 'warning' },
  RECALLED: { label: 'Đã thu hồi', tone: 'danger' },
  CODE_PRINTED: { label: 'Đã in mã', tone: 'info' },
};

/**
 * Trang chi tiết yêu cầu thu hồi hàng loạt (NCL-08-CN-011, NCL-08-CN-012).
 * 
 * Hiển thị:
 * - Thông tin yêu cầu
 * - Danh sách lô hàng trong phạm vi
 * - Nút phê duyệt/từ chối (nếu có quyền)
 * - Nút kết thúc vụ việc (nếu đã duyệt)
 * - Thông tin kết quả xử lý và biện pháp khắc phục (khi đã hoàn thành)
 */
export const BulkRecallRequestDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Breadcrumb quay đến danh sách thu hồi theo phạm vi (NCL-08-CN-011).
  // Nhãn "Yêu cầu thu hồi theo phạm vi" trỏ về danh sách yêu cầu thu hồi theo phạm vi.
  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Yêu cầu thu hồi theo phạm vi', href: '/recall-requests/bulk' },
    { label: 'Chi tiết yêu cầu thu hồi' },
  ]);

  const [request, setRequest] = useState<BulkRecallRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [closeDialogOpen, setCloseDialogOpen] = useState(false);
  const [remarks, setRemarks] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const load = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await getBulkRecallRequest(id);
      setRequest(data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tải yêu cầu thu hồi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [id]);

  // Kiểm tra user có phải là người tạo không
  const isOwnRequest = user?.userId === request?.requestedBy?.userId;

  // Quyền quản lý hợp tác xã (VT-02) hoặc Admin (VT-01)
  const isManager = user?.roleCode === 'VT-02' || user?.roleCode === 'VT-01';

  // Kiểm tra có thể approve không
  const canApprove = request?.status === 'PENDING' && !isOwnRequest && isManager;

  // Kiểm tra có thể kết thúc vụ việc thu hồi không (NCL-08-CN-012)
  const canClose = request?.status === 'APPROVED' && isManager;

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  const handleApprove = async () => {
    if (!id) return;
    try {
      setActionLoading(true);
      const result = await approveBulkRecallRequest(id, {
        remarks: remarks.trim() || undefined,
      });
      toast.success(
        `Đã phê duyệt yêu cầu thu hồi. Tổng số lô chuyển sang trạng thái "Đang thu hồi": ${result.shipments?.filter(s => s.included).length || 0
        }`,
      );
      setApproveDialogOpen(false);
      load(); // Refresh data
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể phê duyệt yêu cầu');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!id) return;
    if (!rejectionReason.trim()) {
      toast.error('Lý do từ chối không được để trống');
      return;
    }
    try {
      setActionLoading(true);
      await rejectBulkRecallRequest(id, { reason: rejectionReason.trim() });
      toast.success('Đã từ chối yêu cầu thu hồi');
      setRejectDialogOpen(false);
      load(); // Refresh data
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể từ chối yêu cầu');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <LoaderCircle className="w-8 h-8 animate-spin text-slate-400" />
      </div>
    );
  }

  if (!request) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-slate-500">
        <XCircle className="w-12 h-12 mb-4" />
        <p>Không tìm thấy yêu cầu thu hồi</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate('/recall-requests/bulk')}>
          Quay lại danh sách
        </Button>
      </div>
    );
  }

  const includedShipments = request.shipments?.filter(s => s.included) || [];
  const excludedShipments = request.shipments?.filter(s => !s.included) || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Yêu cầu thu hồi hàng loạt
          </h1>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge
            label={STATUS_MAP[request.status]?.label || request.status}
            tone={STATUS_MAP[request.status]?.tone || 'neutral'}
          />
        </div>
      </div>

      {/* Thông tin yêu cầu */}
      <Card>
        <CardContent className="p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Thông tin yêu cầu</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-slate-500">Lô sản xuất nguồn:</span>
              <p className="font-medium">{request.productionLotName}</p>
            </div>
            <div>
              <span className="text-slate-500">Người tạo:</span>
              <p className="font-medium">{request.requestedBy?.fullName || '—'}</p>
            </div>
            <div>
              <span className="text-slate-500">Thời gian tạo:</span>
              <p className="font-medium">{formatDate(request.requestedAt)}</p>
            </div>
            <div>
              <span className="text-slate-500">Lý do:</span>
              <p className="font-medium">{request.reason}</p>
            </div>
            {request.evidence && (
              <div className="md:col-span-2">
                <span className="text-slate-500">Bằng chứng:</span>
                <p className="font-medium whitespace-pre-line">{request.evidence}</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Thông báo nếu là người tạo */}
      {isOwnRequest && request.status === 'PENDING' && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-amber-800">Bạn là người tạo yêu cầu này</p>
            <p className="text-sm text-amber-700 mt-1">
              Người tạo yêu cầu không được tự phê duyệt. Vui lòng chờ quản lý khác xét duyệt.
            </p>
          </div>
        </div>
      )}

      {/* Thông tin kết thúc vụ việc khi đã hoàn thành (COMPLETED) */}
      {request.status === 'COMPLETED' && (
        <Card className="border-emerald-200 bg-emerald-50/20">
          <CardContent className="p-6">
            <h2 className="text-lg font-semibold text-emerald-900 mb-4 flex items-center gap-2">
              <ClipboardCheck className="w-5 h-5 text-emerald-600" />
              Kết quả xử lý & Biện pháp khắc phục phòng ngừa
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-slate-500">Người kết thúc:</span>
                <p className="font-medium text-slate-800">{request.closedBy?.fullName || '—'}</p>
              </div>
              <div>
                <span className="text-slate-500">Thời gian kết thúc:</span>
                <p className="font-medium text-slate-800">{formatDate(request.closedAt)}</p>
              </div>
              {((request.evidenceFiles && request.evidenceFiles.length > 0) ||
                (request.evidenceFileIds && request.evidenceFileIds.length > 0)) && (
                  <div className="md:col-span-2 space-y-1.5">
                    <span className="text-slate-500 font-medium text-xs">
                      Tệp biên bản / bằng chứng thu hồi:
                    </span>
                    {request.evidenceFiles && request.evidenceFiles.length > 0 ? (
                      <div className="flex flex-wrap gap-2 pt-1">
                        {request.evidenceFiles.map((file) => (
                          <a
                            key={file.id}
                            href={getEvidenceDownloadUrl(file.id)}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-md border border-emerald-300 bg-white hover:bg-emerald-50 text-emerald-800 text-xs transition-colors shadow-xs group"
                            title="Bấm để mở xem tệp trong tab mới của trình duyệt"
                          >
                            <FileText className="h-3.5 w-3.5 text-emerald-600 group-hover:text-emerald-700" />
                            <span className="font-medium max-w-[200px] truncate group-hover:underline">{file.fileName}</span>
                            <span className="text-slate-400 text-[11px]">
                              ({Math.round(file.fileSize / 1024)} KB)
                            </span>
                            <ExternalLink className="h-3 w-3 text-slate-400 group-hover:text-emerald-700 ml-0.5" />
                          </a>
                        ))}
                      </div>
                    ) : (
                      <p className="font-mono text-xs text-slate-700 mt-0.5">
                        {request.evidenceFileIds?.join(', ')}
                      </p>
                    )}
                  </div>
                )}
              {request.remediationMeasures && (
                <div className="md:col-span-2 bg-white p-3 rounded-lg border border-emerald-200">
                  <span className="text-slate-700 font-medium">Biện pháp khắc phục phòng ngừa chung:</span>
                  <p className="mt-1 text-slate-800 whitespace-pre-line text-sm">{request.remediationMeasures}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Danh sách lô hàng trong phạm vi */}
      <Card>
        <CardContent className="p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">
            Phạm vi thu hồi ({includedShipments.length} lô)
          </h2>

          {includedShipments.length > 0 ? (
            <div className="space-y-3">
              {includedShipments.map((shipment) => (
                <ShipmentItem key={shipment.id} shipment={shipment} />
              ))}
            </div>
          ) : (
            <p className="text-sm text-slate-500 italic">Không có lô hàng nào trong phạm vi</p>
          )}
        </CardContent>
      </Card>

      {/* Danh sách lô bị loại */}
      {excludedShipments.length > 0 && (
        <Card>
          <CardContent className="p-6">
            <h2 className="text-lg font-semibold text-slate-900 mb-4">
              Lô bị loại khỏi phạm vi thu hồi ({excludedShipments.length} lô)
            </h2>
            <div className="space-y-2">
              {excludedShipments.map((shipment) => (
                <ShipmentItem key={shipment.id} shipment={shipment} excluded />
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Thông tin phê duyệt/từ chối */}
      {request.status !== 'PENDING' && (
        <Card>
          <CardContent className="p-6">
            <h2 className="text-lg font-semibold text-slate-900 mb-4">
              {request.status === 'REJECTED' ? 'Thông tin từ chối' : 'Thông tin phê duyệt'}
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-slate-500">
                  {request.status === 'REJECTED' ? 'Người từ chối:' : 'Người phê duyệt:'}
                </span>
                <p className="font-medium">
                  {request.status === 'REJECTED'
                    ? request.rejectedBy?.fullName
                    : request.approvedBy?.fullName}
                </p>
              </div>
              <div>
                <span className="text-slate-500">
                  {request.status === 'REJECTED' ? 'Thời gian từ chối:' : 'Thời gian phê duyệt:'}
                </span>
                <p className="font-medium">
                  {formatDate(request.status === 'REJECTED' ? request.rejectedAt : request.approvedAt)}
                </p>
              </div>
              {request.approvalRemarks && (
                <div className="md:col-span-2">
                  <span className="text-slate-500">Ghi chú phê duyệt:</span>
                  <p className="font-medium whitespace-pre-line">{request.approvalRemarks}</p>
                </div>
              )}
              {request.rejectionReason && (
                <div className="md:col-span-2">
                  <span className="text-slate-500">Lý do từ chối:</span>
                  <p className="font-medium whitespace-pre-line">{request.rejectionReason}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Action buttons */}
      <div className="flex gap-3">
        {canApprove && (
          <>
            <Button
              variant="outline"
              onClick={() => setRejectDialogOpen(true)}
            >
              Từ chối
            </Button>
            <Button onClick={() => setApproveDialogOpen(true)}>
              Phê duyệt
            </Button>
          </>
        )}
        {canClose && (
          <Button
            className="bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm"
            onClick={() => setCloseDialogOpen(true)}
          >
            Kết thúc vụ việc
          </Button>
        )}
      </div>

      {/* Approve Confirmation Dialog */}
      <AlertDialog open={approveDialogOpen} onOpenChange={setApproveDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Phê duyệt yêu cầu thu hồi hàng loạt</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-4">
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
              <p className="font-medium mb-1">Cảnh báo quan trọng</p>
              <p>
                Sau khi phê duyệt, {includedShipments.length} lô hàng trong phạm vi sẽ chuyển sang trạng thái{' '}
                <strong>"Đang thu hồi"</strong> và hệ thống sẽ:
              </p>
              <ul className="list-disc list-inside mt-2 space-y-1">
                <li>Bật cảnh báo thu hồi công khai khi tra cứu tem cho các lô hàng này</li>
                <li>Gửi thông báo đến các doanh nghiệp thu mua liên quan</li>
              </ul>
            </div>
            <div className="space-y-2">
              <Label htmlFor="remarks">Ghi chú phê duyệt (tùy chọn)</Label>
              <Textarea
                id="remarks"
                placeholder="Nhập ghi chú khi phê duyệt..."
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
                className="min-h-[80px]"
              />
            </div>
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Hủy</AlertDialogCancel>
            <Button onClick={handleApprove} disabled={actionLoading}>
              {actionLoading ? 'Đang xử lý...' : 'Phê duyệt'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Reject Dialog */}
      <AlertDialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Từ chối yêu cầu thu hồi</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-2">
            <Label htmlFor="rejectionReason">
              Lý do từ chối <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="rejectionReason"
              placeholder="Nhập lý do từ chối yêu cầu này..."
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              className="min-h-[80px]"
            />
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Hủy</AlertDialogCancel>
            <Button variant="destructive" onClick={handleReject} disabled={actionLoading}>
              {actionLoading ? 'Đang xử lý...' : 'Từ chối'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Close Bulk Recall Dialog (NCL-08-CN-012) */}
      <CloseBulkRecallDialog
        open={closeDialogOpen}
        bulkRequest={request}
        onClose={() => setCloseDialogOpen(false)}
        onSuccess={load}
      />
    </div>
  );
};

/**
 * Component hiển thị thông tin lô hàng trong yêu cầu thu hồi.
 */
const ShipmentItem: React.FC<{ shipment: BulkRecallShipmentItem; excluded?: boolean }> = ({
  shipment,
  excluded = false,
}) => {
  const statusInfo = SHIPMENT_STATUS_MAP[shipment.shipmentStatus] || {
    label: shipment.shipmentStatus,
    tone: 'neutral' as const,
  };

  return (
    <div
      className={`p-3.5 rounded-lg border ${excluded ? 'bg-slate-50 border-slate-200' : 'bg-white border-slate-200'
        }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Package className={`w-5 h-5 ${excluded ? 'text-slate-400' : 'text-blue-500'}`} />
          <div>
            <div className="flex items-center gap-2">
              <span className="font-medium text-sm text-slate-900">{shipment.shipmentName}</span>
              <StatusBadge label={statusInfo.label} tone={statusInfo.tone} />
              {excluded && (
                <span className="text-xs bg-slate-200 text-slate-600 px-2 py-0.5 rounded-full">
                  Bị loại
                </span>
              )}
            </div>
            {excluded && shipment.exclusionReason && (
              <p className="text-xs text-slate-500 mt-1">
                Lý do loại: {shipment.exclusionReason}
              </p>
            )}
          </div>
        </div>

        {shipment.totalQuantity != null && (
          <div className="text-xs text-muted-foreground">
            Tổng: <span className="font-medium text-slate-700">{shipment.totalQuantity}</span>{' '}
            {shipment.unit || ''}
          </div>
        )}
      </div>

      {/* Kết quả xử lý lô khi vụ việc đã kết thúc */}
      {shipment.resolution && (
        <div className="mt-3 pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-xs bg-emerald-50/40 p-2.5 rounded-md">
          <div className="flex items-center gap-2">
            <span className="text-slate-600">Kết quả xử lý:</span>
            <span className="font-semibold text-emerald-800">
              {RESOLUTION_LABEL[shipment.resolution] || shipment.resolution}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-slate-600">Thu hồi được:</span>
            <span className="font-semibold text-emerald-800">
              {shipment.recoveredQuantity ?? 0} {shipment.unit || ''}
            </span>
          </div>
          {shipment.notes && (
            <div className="w-full text-slate-700 mt-1 bg-white/70 p-2 rounded border border-emerald-100">
              <span className="font-medium text-slate-800">Biện pháp khắc phục: </span>
              {shipment.notes}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default BulkRecallRequestDetailPage;
