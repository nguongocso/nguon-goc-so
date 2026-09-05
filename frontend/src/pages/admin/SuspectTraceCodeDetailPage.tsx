import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getSuspectDetail, lockTraceCode, unlockTraceCode } from '@/api/suspectTraceCodeApi';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { SuspectTraceCodeDetailResponse } from '@/types/suspectTraceCode';
import { AlertTriangle, CheckCircle2, Clock, Lock, MapPin, RefreshCw, ScanLine, ShieldAlert, Unlock } from 'lucide-react';
import { toast } from 'sonner';
import { HelpButton } from '@/components/help/HelpButton';
import { useAuth } from '@/hooks/useAuth';

const formatDateTime = (value: string | null) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

export default function SuspectTraceCodeDetailPage() {
  const { traceCodeId } = useParams<{ traceCodeId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [detail, setDetail] = useState<SuspectTraceCodeDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);

  // Trạng thái form khóa nội tuyến
  const [showLockForm, setShowLockForm] = useState(false);
  const [lockReason, setLockReason] = useState('');
  const [lockSubmitting, setLockSubmitting] = useState(false);

  // Trạng thái form mở khóa nội tuyến
  const [showUnlockForm, setShowUnlockForm] = useState(false);
  const [unlockConclusion, setUnlockConclusion] = useState('');
  const [unlockEvidence, setUnlockEvidence] = useState('');
  const [unlockSubmitting, setUnlockSubmitting] = useState(false);

  const fetchDetail = async () => {
    if (!traceCodeId) return;
    try {
      setLoading(true);
      const data = await getSuspectDetail(traceCodeId);
      setDetail(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || 'Không thể tải chi tiết mã tem',
      );
      navigate('/admin/suspect-trace-codes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [traceCodeId]);

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <RefreshCw className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <p className="text-muted-foreground">Không tìm thấy mã tem</p>
      </div>
    );
  }

  const getScoreColor = (score: number) => {
    if (score >= 70) return 'text-red-600';
    if (score >= 50) return 'text-amber-600';
    return 'text-muted-foreground';
  };

  // Kiểm tra xem người dùng hiện tại có phải là người đã khóa mã tem không
  const isSameAdmin = Boolean(
    user?.userId && detail.lockedBy && user.userId === detail.lockedBy
  );
  const minUnlockChars = isSameAdmin ? 20 : 10;
  const trimmedUnlockConclusion = unlockConclusion.trim();
  const isUnlockEmpty = !trimmedUnlockConclusion;
  const isUnlockTooShort = trimmedUnlockConclusion.length > 0 && trimmedUnlockConclusion.length < minUnlockChars;
  const isUnlockTooLong = trimmedUnlockConclusion.length > 500;
  const isEvidenceTooLong = unlockEvidence.trim().length > 500;
  const isUnlockValid = !isUnlockEmpty && !isUnlockTooShort && !isUnlockTooLong && !isEvidenceTooLong;

  // Xác thực form khóa mã tem
  const trimmedLockReason = lockReason.trim();
  const isLockEmpty = !trimmedLockReason;
  const isLockTooShort = trimmedLockReason.length > 0 && trimmedLockReason.length < 10;
  const isLockTooLong = trimmedLockReason.length > 500;
  const isLockValid = !isLockEmpty && !isLockTooShort && !isLockTooLong;

  const handleLockSubmit = async () => {
    if (!detail || !isLockValid) return;
    try {
      setLockSubmitting(true);
      await lockTraceCode(detail.id, { reason: trimmedLockReason });
      toast.success(`Đã khóa mã tem ${detail.codeValue}`);
      setLockReason('');
      setShowLockForm(false);
      fetchDetail();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể khóa mã tem');
    } finally {
      setLockSubmitting(false);
    }
  };

  const handleUnlockSubmit = async () => {
    if (!detail || !isUnlockValid) return;
    try {
      setUnlockSubmitting(true);
      await unlockTraceCode(detail.id, {
        conclusion: trimmedUnlockConclusion,
        evidence: unlockEvidence.trim() ? unlockEvidence.trim() : undefined,
      });
      toast.success(`Đã mở khóa mã tem ${detail.codeValue}`);
      setUnlockConclusion('');
      setUnlockEvidence('');
      setShowUnlockForm(false);
      fetchDetail();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể mở khóa mã tem');
    } finally {
      setUnlockSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-red-100 p-2.5 text-red-700">
            <ShieldAlert className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Chi tiết mã tem nghi vấn</h1>
            <p className="text-sm text-muted-foreground font-mono">
              {detail.codeValue}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="admin-suspect-trace-codes" />
          {detail.status === 'SUSPECT' && (
            <Button variant="destructive" onClick={() => setShowLockForm((prev) => !prev)}>
              <Lock className="mr-2 h-4 w-4" />
              {showLockForm ? 'Đóng biểu mẫu khóa' : 'Khóa mã tem'}
            </Button>
          )}
          {detail.status === 'LOCKED' && (
            <Button
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={() => setShowUnlockForm((prev) => !prev)}
            >
              <Unlock className="mr-2 h-4 w-4" />
              {showUnlockForm ? 'Đóng biểu mẫu mở khóa' : 'Mở khóa mã tem'}
            </Button>
          )}
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-4">
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Điểm nghi vấn</p>
              <p className={`text-2xl font-bold ${getScoreColor(detail.suspicionScore)}`}>
                {detail.suspicionScore}/100
              </p>
            </div>
            <AlertTriangle className="h-8 w-8 text-amber-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Trạng thái</p>
              {detail.status === 'SUSPECT' && (
                <Badge variant="outline" className="border-amber-300 text-amber-700 text-lg">
                  Nghi vấn
                </Badge>
              )}
              {detail.status === 'LOCKED' && (
                <Badge variant="destructive" className="text-lg">
                  <Lock className="mr-1 h-4 w-4" />
                  Đã khóa
                </Badge>
              )}
              {detail.status === 'ACTIVE' && (
                <Badge className="bg-emerald-600 text-white text-lg hover:bg-emerald-700">
                  <CheckCircle2 className="mr-1 h-4 w-4" />
                  Đã xác minh
                </Badge>
              )}
            </div>
            {detail.status === 'ACTIVE' ? (
              <CheckCircle2 className="h-8 w-8 text-emerald-500" />
            ) : detail.status === 'LOCKED' ? (
              <ShieldAlert className="h-8 w-8 text-red-500" />
            ) : (
              <ShieldAlert className="h-8 w-8 text-amber-500" />
            )}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Lượt quét (24h)</p>
              <p className="text-2xl font-bold">{detail.anomalyDetails.totalScans}</p>
            </div>
            <ScanLine className="h-8 w-8 text-blue-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Địa điểm</p>
              <p className="text-2xl font-bold">{detail.anomalyDetails.uniqueLocations}</p>
            </div>
            <MapPin className="h-8 w-8 text-emerald-500" />
          </CardContent>
        </Card>
      </div>

      {/* Form khóa nội tuyến */}
      {showLockForm && detail.status === 'SUSPECT' && (
        <Card className="border-2 border-red-300 bg-red-50/40 shadow-md">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-destructive text-lg">
              <Lock className="h-5 w-5" />
              Khóa mã tem nghi vấn
            </CardTitle>
            <CardDescription>
              Hành động này sẽ chặn mã tem, người tiêu dùng quét mã sẽ thấy cảnh báo thay vì hành trình bình thường.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="lockReasonInput" className="font-medium text-slate-800">
                Lý do khóa *
              </Label>
              <Textarea
                id="lockReasonInput"
                placeholder="Nhập lý do khóa mã tem (tối thiểu 10 ký tự)"
                value={lockReason}
                onChange={(e) => setLockReason(e.target.value)}
                rows={3}
                maxLength={500}
              />
              <div className="flex justify-between text-xs">
                <span>
                  {isLockEmpty && <span className="text-destructive">Vui lòng nhập lý do khóa</span>}
                  {isLockTooShort && (
                    <span className="text-destructive">
                      Tối thiểu 10 ký tự (hiện có {trimmedLockReason.length})
                    </span>
                  )}
                  {isLockTooLong && <span className="text-destructive">Tối đa 500 ký tự</span>}
                  {isLockValid && (
                    <span className="text-emerald-600 flex items-center gap-1">
                      <CheckCircle2 className="h-3.5 w-3.5" /> Hợp lệ
                    </span>
                  )}
                </span>
                <span className="text-muted-foreground">{trimmedLockReason.length}/500</span>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setShowLockForm(false)} disabled={lockSubmitting}>
                Hủy bỏ
              </Button>
              <Button variant="destructive" onClick={handleLockSubmit} disabled={!isLockValid || lockSubmitting}>
                <Lock className="mr-2 h-4 w-4" />
                {lockSubmitting ? 'Đang xử lý...' : 'Xác nhận khóa tem'}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Form mở khóa nội tuyến */}
      {showUnlockForm && detail.status === 'LOCKED' && (
        <Card className="border-2 border-emerald-300 bg-emerald-50/40 shadow-md">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-emerald-700 text-lg">
              <Unlock className="h-5 w-5" />
              Mở khóa mã tem sau khi xác minh
            </CardTitle>
            <CardDescription>
              Khôi phục trạng thái hoạt động của mã tem, gỡ bỏ cảnh báo trên trang tra cứu công khai.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {isSameAdmin && (
              <div className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-2.5 text-xs text-amber-800">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
                <span>
                  Bạn là người đã khóa mã tem này. Vui lòng nhập kết luận xác minh chi tiết (tối thiểu 20 ký tự).
                </span>
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="unlockConclusionInput" className="font-medium text-slate-800">
                Kết luận xác minh *
              </Label>
              <Textarea
                id="unlockConclusionInput"
                placeholder={`Nhập kết luận xác minh (tối thiểu ${minUnlockChars} ký tự)`}
                value={unlockConclusion}
                onChange={(e) => setUnlockConclusion(e.target.value)}
                rows={3}
                maxLength={500}
              />
              <div className="flex justify-between text-xs">
                <span>
                  {isUnlockEmpty && <span className="text-destructive">Vui lòng nhập kết luận xác minh</span>}
                  {isUnlockTooShort && (
                    <span className="text-destructive">
                      Tối thiểu {minUnlockChars} ký tự (hiện có {trimmedUnlockConclusion.length})
                    </span>
                  )}
                  {isUnlockTooLong && <span className="text-destructive">Tối đa 500 ký tự</span>}
                  {isUnlockValid && (
                    <span className="text-emerald-600 flex items-center gap-1">
                      <CheckCircle2 className="h-3.5 w-3.5" /> Hợp lệ
                    </span>
                  )}
                </span>
                <span className="text-muted-foreground">{trimmedUnlockConclusion.length}/500</span>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="unlockEvidenceInput" className="font-medium text-slate-800">
                Bằng chứng xác minh (tùy chọn)
              </Label>
              <Textarea
                id="unlockEvidenceInput"
                placeholder="Nhập thông tin chứng từ, biên bản xác minh, ghi chú..."
                value={unlockEvidence}
                onChange={(e) => setUnlockEvidence(e.target.value)}
                rows={2}
                maxLength={500}
              />
              <div className="flex justify-end text-xs text-muted-foreground">
                {unlockEvidence.trim().length}/500
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setShowUnlockForm(false)} disabled={unlockSubmitting}>
                Hủy bỏ
              </Button>
              <Button
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
                onClick={handleUnlockSubmit}
                disabled={!isUnlockValid || unlockSubmitting}
              >
                <Unlock className="mr-2 h-4 w-4" />
                {unlockSubmitting ? 'Đang xử lý...' : 'Xác nhận mở khóa'}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Suspect Info */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Thông tin nghi vấn</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <p className="text-sm text-muted-foreground">Lô hàng</p>
              <p className="font-medium">{detail.shipmentName}</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Mã tem</p>
              <p className="font-mono text-sm">{detail.codeValue}</p>
            </div>
            {detail.lockedAt && (
              <>
                <div>
                  <p className="text-sm text-muted-foreground">Thời điểm khóa</p>
                  <p className="font-medium">{formatDateTime(detail.lockedAt)}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Người khóa</p>
                  <p className="font-medium">{detail.lockedByName || '—'}</p>
                </div>
              </>
            )}
            {detail.unlockedAt && (
              <>
                <div>
                  <p className="text-sm text-muted-foreground">Thời điểm mở khóa</p>
                  <p className="font-medium">{formatDateTime(detail.unlockedAt)}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Người mở khóa</p>
                  <p className="font-medium">{detail.unlockedByName || '—'}</p>
                </div>
              </>
            )}
          </div>

          {detail.suspicionReason && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3">
              <div className="flex items-start gap-2">
                <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
                <div>
                  <p className="font-medium text-amber-900">Lý do nghi vấn</p>
                  <p className="text-sm text-amber-800">{detail.suspicionReason}</p>
                </div>
              </div>
            </div>
          )}

          {detail.lockReason && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-3">
              <div className="flex items-start gap-2">
                <Lock className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
                <div>
                  <p className="font-medium text-red-900">Lý do khóa</p>
                  <p className="text-sm text-red-800">{detail.lockReason}</p>
                </div>
              </div>
            </div>
          )}

          {detail.unlockConclusion && (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3">
              <div className="flex items-start gap-2">
                <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
                <div className="space-y-1">
                  <p className="font-medium text-emerald-900">Kết luận xác minh mở khóa</p>
                  <p className="text-sm text-emerald-800">{detail.unlockConclusion}</p>
                  {detail.unlockEvidence && (
                    <p className="text-xs text-emerald-700 italic">Bằng chứng: {detail.unlockEvidence}</p>
                  )}
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Score Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Chi tiết điểm nghi vấn</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            <div className="flex items-center justify-between rounded-lg border p-3">
              <div className="flex items-center gap-2">
                <ScanLine className="h-4 w-4 text-blue-500" />
                <span className="text-sm">Tần suất quét cao ({'>'}= 10 lượt/24h)</span>
              </div>
              <Badge variant={detail.anomalyDetails.scoreBreakdown.highFrequency > 0 ? 'default' : 'secondary'}>
                +{detail.anomalyDetails.scoreBreakdown.highFrequency}
              </Badge>
            </div>
            <div className="flex items-center justify-between rounded-lg border p-3">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-red-500" />
                <span className="text-sm">
                  Khoảng cách không hợp lý ({'>'}50km trong {'<'}30 phút)
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-xs text-muted-foreground">
                  {detail.anomalyDetails.impossibleTravelCount} cặp
                </span>
                <Badge variant={detail.anomalyDetails.scoreBreakdown.impossibleTravel > 0 ? 'default' : 'secondary'}>
                  +{detail.anomalyDetails.scoreBreakdown.impossibleTravel}
                </Badge>
              </div>
            </div>
            <div className="flex items-center justify-between rounded-lg border p-3">
              <div className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-emerald-500" />
                <span className="text-sm">Nhiều địa điểm ({'>'}= 5 địa điểm/24h)</span>
              </div>
              <Badge variant={detail.anomalyDetails.scoreBreakdown.multipleLocations > 0 ? 'default' : 'secondary'}>
                +{detail.anomalyDetails.scoreBreakdown.multipleLocations}
              </Badge>
            </div>
            <div className="flex items-center justify-between rounded-lg border-2 border-primary/20 bg-primary/5 p-3">
              <span className="font-semibold">Tổng điểm</span>
              <span className={`text-lg font-bold ${getScoreColor(detail.suspicionScore)}`}>
                {detail.suspicionScore}/100
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Scan Logs */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Lịch sử quét (24 giờ gần nhất)</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {detail.scanLogs.length === 0 ? (
            <div className="flex justify-center py-8 text-sm text-muted-foreground">
              Không có lượt quét nào
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Thời gian</TableHead>
                    <TableHead>Vị trí</TableHead>
                    <TableHead>Tọa độ</TableHead>
                    <TableHead>Thiết bị</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {detail.scanLogs.map((scan, index) => (
                    <TableRow key={index}>
                      <TableCell className="whitespace-nowrap text-sm">
                        {formatDateTime(scan.scannedAt)}
                      </TableCell>
                      <TableCell className="text-sm">
                        {scan.location || 'Không xác định'}
                      </TableCell>
                      <TableCell className="font-mono text-xs text-muted-foreground">
                        {scan.latitude && scan.longitude
                          ? `${scan.latitude.toFixed(4)}, ${scan.longitude.toFixed(4)}`
                          : '—'}
                      </TableCell>
                      <TableCell>
                        <span className="block max-w-48 truncate text-xs text-muted-foreground"
                          title={scan.userAgent}>
                          {scan.userAgent || '—'}
                        </span>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

    </div>
  );
}