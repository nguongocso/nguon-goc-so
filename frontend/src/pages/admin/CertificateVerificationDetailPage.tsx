import { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  ArrowLeft,
  Award,
  CheckCircle2,
  ExternalLink,
  FileWarning,
  LoaderCircle,
  ShieldCheck,
  XCircle,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import {
  getCertificateDocument,
  getCertificateVerification,
  rejectCertificate,
  verifyCertificate,
} from '@/api/certificateVerificationApi';
import { toApiError } from '@/api/apiError';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type {
  CertificateVerification,
  CertificateVerificationStatus,
} from '@/types/certificateVerification';

const STATUS_LABEL: Record<CertificateVerificationStatus, string> = {
  PENDING: 'Đang chờ xác thực',
  VERIFIED: 'Đã xác thực',
  REJECTED: 'Đã từ chối',
};

const STATUS_TONE = {
  PENDING: 'warning',
  VERIFIED: 'success',
  REJECTED: 'danger',
} as const;

const formatDate = (value?: string | null, includeTime = false): string => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return includeTime ? date.toLocaleString('vi-VN') : date.toLocaleDateString('vi-VN');
};

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

interface DetailRowProps {
  label: string;
  value: string;
}

const DetailRow = ({ label, value }: DetailRowProps) => (
  <div className="grid grid-cols-[9rem_1fr] gap-3 border-b py-2.5 last:border-b-0">
    <dt className="text-muted-foreground">{label}</dt>
    <dd className="min-w-0 font-medium text-foreground">{value}</dd>
  </div>
);

export const CertificateVerificationDetailPage = () => {
  const { certificateId } = useParams<{ certificateId: string }>();
  const navigate = useNavigate();
  const [certificate, setCertificate] = useState<CertificateVerification | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [documentUrl, setDocumentUrl] = useState<string | null>(null);
  const [documentError, setDocumentError] = useState<string | null>(null);
  const [documentLoading, setDocumentLoading] = useState(false);
  const [verifyOpen, setVerifyOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [reviewNote, setReviewNote] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Xác thực chứng nhận', href: '/admin/certifications' },
    { label: 'Chi tiết chứng nhận' },
  ]);

  const loadDetail = useCallback(async () => {
    if (!certificateId) {
      setLoadError('Không xác định được chứng nhận cần xem.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setLoadError(null);
      setCertificate(await getCertificateVerification(certificateId));
    } catch (error: unknown) {
      setLoadError(toApiError(error, 'Không thể tải chi tiết chứng nhận.').message);
      setCertificate(null);
    } finally {
      setLoading(false);
    }
  }, [certificateId]);

  useEffect(() => {
    void loadDetail();
  }, [loadDetail]);

  useEffect(() => {
    let cancelled = false;
    let objectUrl: string | null = null;

    setDocumentUrl(null);
    setDocumentError(null);
    if (!certificateId || !certificate?.document) {
      setDocumentLoading(false);
      if (certificate && !certificate.document) {
        setDocumentError('Chứng nhận chưa có tệp đính kèm để đối chiếu.');
      }
      return undefined;
    }

    const loadDocument = async () => {
      setDocumentLoading(true);
      try {
        const blob = await getCertificateDocument(certificateId);
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setDocumentUrl(objectUrl);
      } catch (error: unknown) {
        if (!cancelled) {
          setDocumentError(toApiError(error, 'Không thể mở tệp chứng nhận.').message);
        }
      } finally {
        if (!cancelled) setDocumentLoading(false);
      }
    };

    void loadDocument();
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [certificateId, certificate?.document]);

  const handleVerify = async () => {
    if (!certificate) return;
    if (reviewNote.trim().length > 1000) {
      toast.error('Ghi chú xác thực không được vượt quá 1000 ký tự.');
      return;
    }
    try {
      setActionLoading(true);
      const result = await verifyCertificate(certificate.id, {
        reviewNote: reviewNote.trim() || undefined,
      });
      setCertificate(result);
      toast.success(`Đã xác thực chứng nhận ${result.code}.`);
      setVerifyOpen(false);
      setReviewNote('');
    } catch (error: unknown) {
      toast.error(toApiError(error, 'Không thể xác thực chứng nhận.').message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!certificate) return;
    const reason = rejectionReason.trim();
    if (reason.length < 10 || reason.length > 1000) {
      toast.error('Lý do từ chối phải từ 10 đến 1000 ký tự.');
      return;
    }
    try {
      setActionLoading(true);
      const result = await rejectCertificate(certificate.id, { rejectionReason: reason });
      setCertificate(result);
      toast.success(`Đã từ chối chứng nhận ${result.code} và gửi thông báo cho tổ chức.`);
      setRejectOpen(false);
      setRejectionReason('');
    } catch (error: unknown) {
      toast.error(toApiError(error, 'Không thể từ chối chứng nhận.').message);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={ShieldCheck}
        title="Chi tiết chứng nhận"
        description={certificate ? `${certificate.standardName} · ${certificate.code}` : 'Đối chiếu thông tin và tài liệu chứng nhận.'}
        actions={
          <Button variant="outline" onClick={() => navigate('/admin/certifications')}>
            <ArrowLeft /> Quay lại danh sách
          </Button>
        }
      />

      {loadError && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Không tải được chi tiết</AlertTitle>
          <AlertDescription>{loadError}</AlertDescription>
        </Alert>
      )}

      {loading ? (
        <Card>
          <CardContent className="flex min-h-80 items-center justify-center gap-2 text-muted-foreground">
            <LoaderCircle className="size-5 animate-spin" /> Đang tải chi tiết...
          </CardContent>
        </Card>
      ) : certificate ? (
        <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1.5fr)_minmax(20rem,0.8fr)]">
          <Card className="min-w-0">
            <CardHeader className="border-b">
              <CardTitle>Tệp chứng nhận</CardTitle>
              <CardDescription>
                {certificate.document
                  ? `${certificate.document.fileName} · ${formatFileSize(certificate.document.fileSize)}`
                  : 'Chưa có tệp đính kèm'}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="mb-3 flex justify-end">
                {documentUrl && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => window.open(documentUrl, '_blank', 'noopener,noreferrer')}
                  >
                    <ExternalLink /> Mở toàn màn hình
                  </Button>
                )}
              </div>
              <div className="flex min-h-[36rem] items-center justify-center overflow-hidden rounded-xl border bg-slate-100 dark:bg-slate-900">
                {documentLoading ? (
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <LoaderCircle className="size-5 animate-spin" /> Đang mở tệp...
                  </div>
                ) : documentError ? (
                  <div className="max-w-sm px-6 text-center">
                    <FileWarning className="mx-auto size-10 text-amber-600" />
                    <p className="mt-3 font-medium">Không thể hiển thị tệp</p>
                    <p className="mt-1 text-sm text-muted-foreground">{documentError}</p>
                  </div>
                ) : documentUrl && certificate.document?.contentType === 'application/pdf' ? (
                  <iframe
                    title={`Tệp chứng nhận ${certificate.code}`}
                    src={documentUrl}
                    className="h-[42rem] w-full bg-white"
                  />
                ) : documentUrl ? (
                  <img
                    src={documentUrl}
                    alt={`Chứng nhận ${certificate.code}`}
                    className="max-h-[42rem] max-w-full object-contain"
                  />
                ) : null}
              </div>
            </CardContent>
          </Card>

          <div className="space-y-5">
            <Card>
              <CardHeader className="border-b">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <CardTitle>{certificate.standardName}</CardTitle>
                    <CardDescription className="mt-1">{certificate.organizationName}</CardDescription>
                  </div>
                  <StatusBadge
                    label={STATUS_LABEL[certificate.verificationStatus]}
                    tone={STATUS_TONE[certificate.verificationStatus]}
                    className={certificate.verificationStatus === 'PENDING' ? 'text-amber-800' : undefined}
                  />
                </div>
              </CardHeader>
              <CardContent className="space-y-5">
                <section aria-labelledby="comparison-heading">
                  <div className="mb-3 flex items-center gap-2">
                    <Award className="size-4 text-emerald-600" />
                    <h2 id="comparison-heading" className="font-semibold">Thông tin cần đối chiếu</h2>
                  </div>
                  <dl className="rounded-xl border bg-muted/20 px-4">
                    <DetailRow label="Số hiệu" value={certificate.code} />
                    <DetailRow label="Cơ quan cấp" value={certificate.issuedBy || 'Chưa khai báo'} />
                    <DetailRow label="Tiêu chuẩn" value={certificate.standardName} />
                    <DetailRow label="Ngày cấp" value={formatDate(certificate.issueDate)} />
                    <DetailRow label="Ngày hết hạn" value={formatDate(certificate.expiryDate)} />
                    <DetailRow label="Tổ chức" value={certificate.organizationName} />
                  </dl>
                </section>

                {certificate.validityStatus === 'EXPIRED' && (
                  <Alert variant="warning">
                    <AlertCircle />
                    <AlertTitle>Chứng nhận đã hết hạn</AlertTitle>
                    <AlertDescription>
                      Có thể xác thực tính chân thực, nhưng chứng nhận sẽ không hiển thị là đang đạt chuẩn.
                    </AlertDescription>
                  </Alert>
                )}

                {certificate.verificationStatus === 'REJECTED' && certificate.rejectionReason && (
                  <Alert variant="destructive">
                    <XCircle />
                    <AlertTitle>Lý do từ chối</AlertTitle>
                    <AlertDescription>{certificate.rejectionReason}</AlertDescription>
                  </Alert>
                )}

                {certificate.verificationStatus === 'VERIFIED' && (
                  <Alert variant="success">
                    <CheckCircle2 />
                    <AlertTitle>Đã xác thực</AlertTitle>
                    <AlertDescription>
                      {certificate.reviewedBy?.fullName || 'Quản trị viên'} · {formatDate(certificate.reviewedAt, true)}
                      {certificate.reviewNote ? ` · ${certificate.reviewNote}` : ''}
                    </AlertDescription>
                  </Alert>
                )}
              </CardContent>
              {certificate.verificationStatus === 'PENDING' && (
                <CardFooter className="justify-end gap-2 border-t">
                  <Button variant="destructive" onClick={() => setRejectOpen(true)}>
                    <XCircle /> Từ chối
                  </Button>
                  <Button
                    className="bg-emerald-700 hover:bg-emerald-800"
                    onClick={() => setVerifyOpen(true)}
                    disabled={!certificate.document}
                  >
                    <CheckCircle2 /> Xác thực
                  </Button>
                </CardFooter>
              )}
            </Card>
          </div>
        </div>
      ) : null}

      <Dialog open={verifyOpen} onOpenChange={setVerifyOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác thực chứng nhận</DialogTitle>
            <DialogDescription>
              Xác nhận số hiệu, cơ quan cấp, tiêu chuẩn, thời hạn và tổ chức trên tệp đều khớp dữ liệu khai báo.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="review-note">Ghi chú xác thực (không bắt buộc)</Label>
            <Textarea
              id="review-note"
              value={reviewNote}
              maxLength={1000}
              onChange={(event) => setReviewNote(event.target.value)}
              placeholder="Ghi lại thông tin đã đối chiếu..."
            />
            <p className="text-right text-xs text-muted-foreground">{reviewNote.length}/1000</p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setVerifyOpen(false)} disabled={actionLoading}>
              Hủy
            </Button>
            <Button
              className="bg-emerald-700 hover:bg-emerald-800"
              onClick={() => void handleVerify()}
              disabled={actionLoading}
            >
              {actionLoading ? <LoaderCircle className="animate-spin" /> : <ShieldCheck />}
              Xác nhận xác thực
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={rejectOpen} onOpenChange={setRejectOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Từ chối chứng nhận</DialogTitle>
            <DialogDescription>
              Lý do sẽ được gửi cho tổ chức để họ chỉnh sửa và nộp lại.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="rejection-reason">Lý do từ chối</Label>
            <Textarea
              id="rejection-reason"
              value={rejectionReason}
              maxLength={1000}
              aria-invalid={rejectionReason.length > 0 && rejectionReason.trim().length < 10}
              onChange={(event) => setRejectionReason(event.target.value)}
              placeholder="Nêu rõ thông tin không khớp hoặc tài liệu cần bổ sung..."
            />
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>Tối thiểu 10 ký tự</span>
              <span>{rejectionReason.length}/1000</span>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectOpen(false)} disabled={actionLoading}>
              Hủy
            </Button>
            <Button variant="destructive" onClick={() => void handleReject()} disabled={actionLoading}>
              {actionLoading ? <LoaderCircle className="animate-spin" /> : <XCircle />}
              Xác nhận từ chối
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default CertificateVerificationDetailPage;
