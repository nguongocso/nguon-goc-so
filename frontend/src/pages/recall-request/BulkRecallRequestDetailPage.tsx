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
import { LoaderCircle, XCircle, AlertTriangle, Package } from 'lucide-react';
import {
  approveBulkRecallRequest,
  getBulkRecallRequest,
  rejectBulkRecallRequest,
} from '@/api/recallApi';
import { useAuth } from '@/hooks/useAuth';
import { StatusBadge } from '@/components/common/StatusBadge';
import type { BulkRecallRequest, BulkRecallShipmentItem } from '@/types/bulkRecall';

const STATUS_MAP: Record<string, { label: string; tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }> = {
  PENDING: { label: 'Chờ duyệt', tone: 'warning' },
  APPROVED: { label: 'Đã duyệt', tone: 'success' },
  REJECTED: { label: 'Đã từ chối', tone: 'danger' },
};

const SHIPMENT_STATUS_MAP: Record<string, { label: string; tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }> = {
  ACTIVATED: { label: 'Đã kích hoạt', tone: 'success' },
  DRAFT: { label: 'Dự thảo', tone: 'neutral' },
  RECALLED: { label: 'Đã thu hồi', tone: 'danger' },
  CODE_PRINTED: { label: 'Đã in mã', tone: 'info' },
};

/**
 * Trang chi tiết đề nghị thu hồi hàng loạt (NCL-08-CN-011).
 * 
 * Hiển thị:
 * - Thông tin đề nghị
 * - Danh sách lô hàng trong phạm vi
 * - Nút phê duyệt/từ chối (nếu có quyền)
 */
export const BulkRecallRequestDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [request, setRequest] = useState<BulkRecallRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
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
      toast.error(err.response?.data?.message || 'Không thể tải đề nghị thu hồi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [id]);

  // Kiểm tra user có phải là người tạo không
  const isOwnRequest = user?.userId === request?.requestedBy?.userId;

  // Kiểm tra có thể approve không
  const canApprove = request?.status === 'PENDING' && !isOwnRequest;

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
        `Đã phê duyệt đề nghị thu hồi. Tổng số lô chuyển sang trạng thái "Đã thu hồi": ${
          result.shipments?.filter(s => s.included).length || 0
        }`,
      );
      setApproveDialogOpen(false);
      load(); // Refresh data
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể phê duyệt đề nghị');
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
      toast.success('Đã từ chối đề nghị thu hồi');
      setRejectDialogOpen(false);
      load(); // Refresh data
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể từ chối đề nghị');
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
        <p>Không tìm thấy đề nghị thu hồi</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate('/recall-requests')}>
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
            Đề nghị thu hồi hàng loạt
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Mã đề nghị: {request.id}
          </p>
        </div>
        <StatusBadge
          label={STATUS_MAP[request.status]?.label || request.status}
          tone={STATUS_MAP[request.status]?.tone || 'neutral'}
        />
      </div>

      {/* Thông tin đề nghị */}
      <Card>
        <CardContent className="p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">Thông tin đề nghị</h2>
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
            <p className="text-sm font-medium text-amber-800">Bạn là người tạo đề nghị này</p>
            <p className="text-sm text-amber-700 mt-1">
              Người tạo đề nghị không được tự phê duyệt (QTN-22). Vui lòng chờ quản lý khác xét duyệt.
            </p>
          </div>
        </div>
      )}

      {/* Danh sách lô hàng */}
      <Card>
        <CardContent className="p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">
            Phạm vi thu hồi ({includedShipments.length} lô)
          </h2>
          
          {includedShipments.length > 0 ? (
            <div className="space-y-2">
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
              Lô bị loại khỏi phạm vi ({excludedShipments.length} lô)
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
              {request.status === 'APPROVED' ? 'Thông tin phê duyệt' : 'Thông tin từ chối'}
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-slate-500">
                  {request.status === 'APPROVED' ? 'Người phê duyệt:' : 'Người từ chối:'}
                </span>
                <p className="font-medium">
                  {request.status === 'APPROVED' 
                    ? request.approvedBy?.fullName 
                    : request.rejectedBy?.fullName}
                </p>
              </div>
              <div>
                <span className="text-slate-500">
                  {request.status === 'APPROVED' ? 'Thời gian phê duyệt:' : 'Thời gian từ chối:'}
                </span>
                <p className="font-medium">
                  {formatDate(request.status === 'APPROVED' ? request.approvedAt : request.rejectedAt)}
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
      {canApprove && (
        <div className="flex gap-3">
          <Button
            variant="outline"
            onClick={() => setRejectDialogOpen(true)}
          >
            Từ chối
          </Button>
          <Button onClick={() => setApproveDialogOpen(true)}>
            Phê duyệt
          </Button>
        </div>
      )}

      {/* Approve Confirmation Dialog */}
      <AlertDialog open={approveDialogOpen} onOpenChange={setApproveDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Phê duyệt đề nghị thu hồi hàng loạt</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-4">
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
              <p className="font-medium mb-1">Cảnh báo quan trọng</p>
              <p>
                Sau khi phê duyệt, {includedShipments.length} lô hàng trong phạm vi sẽ chuyển sang trạng thái{' '}
                <strong>"Đã thu hồi"</strong> và hệ thống sẽ:
              </p>
              <ul className="list-disc list-inside mt-2 space-y-1">
                <li>Kích hoạt cảnh báo công khai cho từng mã tem</li>
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
            <AlertDialogTitle>Từ chối đề nghị thu hồi</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-2">
            <Label htmlFor="rejectionReason">
              Lý do từ chối <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="rejectionReason"
              placeholder="Nhập lý do từ chối đề nghị này..."
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
    </div>
  );
};

/**
 * Component hiển thị thông tin lô hàng trong đề nghị thu hồi.
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
      className={`flex items-center justify-between p-3 rounded-lg border ${
        excluded ? 'bg-slate-50 border-slate-200' : 'bg-white border-slate-200'
      }`}
    >
      <div className="flex items-center gap-3">
        <Package className={`w-5 h-5 ${excluded ? 'text-slate-400' : 'text-blue-500'}`} />
        <div>
          <div className="flex items-center gap-2">
            <span className="font-medium text-sm">{shipment.shipmentName}</span>
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
    </div>
  );
};

export default BulkRecallRequestDetailPage;
