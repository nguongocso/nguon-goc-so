import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  Award,
  Building2,
  CalendarDays,
  CheckCircle2,
  ExternalLink,
  FileSearch,
  FileWarning,
  LoaderCircle,
  ShieldCheck,
  XCircle,
} from 'lucide-react';
import { toast } from 'sonner';
import {
  getCertificateDocument,
  getCertificateVerification,
  getCertificateVerifications,
  rejectCertificate,
  verifyCertificate,
} from '@/api/certificateVerificationApi';
import { toApiError } from '@/api/apiError';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { FilterSelect } from '@/components/common/FilterSelect';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListToolbar } from '@/components/common/ListToolbar';
import { Pagination } from '@/components/common/Pagination';
import { RefreshButton } from '@/components/common/RefreshButton';
import { SearchInput } from '@/components/common/SearchInput';
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
import { cn } from '@/lib/utils';
import type {
  CertificateVerification,
  CertificateVerificationStatus,
} from '@/types/certificateVerification';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Đang chờ xác thực' },
  { value: 'VERIFIED', label: 'Đã xác thực' },
  { value: 'REJECTED', label: 'Đã từ chối' },
];

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

export const CertificateVerificationPage = () => {
  const [items, setItems] = useState<CertificateVerification[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<CertificateVerification | null>(null);
  const [status, setStatus] = useState<CertificateVerificationStatus>('PENDING');
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [listLoading, setListLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
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
    { label: 'Xác thực chứng nhận' },
  ]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setKeyword(keywordInput.trim());
      setPage(0);
    }, 350);
    return () => window.clearTimeout(timer);
  }, [keywordInput]);

  const loadList = useCallback(async () => {
    try {
      setListLoading(true);
      setListError(null);
      const result = await getCertificateVerifications({
        verificationStatus: status,
        keyword: keyword || undefined,
        page,
        size: PAGE_SIZE,
        sortBy: 'createdAt',
        sortDir: 'desc',
      });
      setItems(result.items);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
      setSelectedId((current) => {
        if (current && result.items.some((item) => item.id === current)) return current;
        return result.items[0]?.id ?? null;
      });
    } catch (error: unknown) {
      const message = toApiError(error, 'Không thể tải danh sách chứng nhận.').message;
      setListError(message);
      setItems([]);
      setSelectedId(null);
    } finally {
      setListLoading(false);
    }
  }, [keyword, page, status]);

  useEffect(() => {
    void loadList();
  }, [loadList]);

  useEffect(() => {
    let cancelled = false;
    let nextDocumentUrl: string | null = null;

    if (!selectedId) {
      setSelected(null);
      setDocumentUrl(null);
      return undefined;
    }

    const loadDetail = async () => {
      setDetailLoading(true);
      setDocumentLoading(true);
      setDocumentError(null);
      setDocumentUrl(null);
      try {
        const detail = await getCertificateVerification(selectedId);
        if (cancelled) return;
        setSelected(detail);
        if (!detail.document) {
          setDocumentError('Chứng nhận chưa có tệp đính kèm để đối chiếu.');
          setDocumentLoading(false);
          return;
        }
        try {
          const blob = await getCertificateDocument(selectedId);
          if (cancelled) return;
          nextDocumentUrl = URL.createObjectURL(blob);
          setDocumentUrl(nextDocumentUrl);
        } catch (error: unknown) {
          if (!cancelled) {
            setDocumentError(toApiError(error, 'Không thể mở tệp chứng nhận.').message);
          }
        } finally {
          if (!cancelled) setDocumentLoading(false);
        }
      } catch (error: unknown) {
        if (!cancelled) {
          toast.error(toApiError(error, 'Không thể tải chi tiết chứng nhận.').message);
          setSelected(null);
          setDocumentLoading(false);
        }
      } finally {
        if (!cancelled) setDetailLoading(false);
      }
    };

    void loadDetail();
    return () => {
      cancelled = true;
      if (nextDocumentUrl) URL.revokeObjectURL(nextDocumentUrl);
    };
  }, [selectedId]);

  const pendingDescription = useMemo(
    () => status === 'PENDING'
      ? `${totalElements} chứng nhận đang chờ kiểm tra.`
      : 'Kiểm tra thông tin khai báo và tài liệu do tổ chức cung cấp.',
    [status, totalElements],
  );

  const refreshAfterDecision = async (result: CertificateVerification) => {
    setSelected(result);
    await loadList();
  };

  const handleVerify = async () => {
    if (!selected) return;
    if (reviewNote.trim().length > 1000) {
      toast.error('Ghi chú xác thực không được vượt quá 1000 ký tự.');
      return;
    }
    try {
      setActionLoading(true);
      const result = await verifyCertificate(selected.id, {
        reviewNote: reviewNote.trim() || undefined,
      });
      toast.success(`Đã xác thực chứng nhận ${result.code}.`);
      setVerifyOpen(false);
      setReviewNote('');
      await refreshAfterDecision(result);
    } catch (error: unknown) {
      toast.error(toApiError(error, 'Không thể xác thực chứng nhận.').message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!selected) return;
    const reason = rejectionReason.trim();
    if (reason.length < 10 || reason.length > 1000) {
      toast.error('Lý do từ chối phải từ 10 đến 1000 ký tự.');
      return;
    }
    try {
      setActionLoading(true);
      const result = await rejectCertificate(selected.id, { rejectionReason: reason });
      toast.success(`Đã từ chối chứng nhận ${result.code} và gửi thông báo cho tổ chức.`);
      setRejectOpen(false);
      setRejectionReason('');
      await refreshAfterDecision(result);
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
        title="Xác thực chứng nhận"
        description={pendingDescription}
      />

      <ListToolbar
        left={
          <>
            <SearchInput
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              placeholder="Tìm số hiệu, cơ quan cấp, tiêu chuẩn hoặc tổ chức..."
            />
            <FilterSelect
              value={status}
              onValueChange={(value) => {
                setStatus((value || 'PENDING') as CertificateVerificationStatus);
                setPage(0);
              }}
              options={STATUS_OPTIONS}
            />
          </>
        }
        right={<RefreshButton onClick={() => void loadList()} loading={listLoading} />}
      />

      {listError && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Không tải được danh sách</AlertTitle>
          <AlertDescription>{listError}</AlertDescription>
        </Alert>
      )}

      <div className="grid items-start gap-5 xl:grid-cols-[minmax(22rem,0.9fr)_minmax(34rem,1.5fr)]">
        <Card className="min-w-0">
          <CardHeader className="border-b">
            <CardTitle>Danh sách chứng nhận</CardTitle>
            <CardDescription>Chọn một chứng nhận để xem và đối chiếu.</CardDescription>
          </CardHeader>
          <CardContent className="p-0">
            {listLoading ? (
              <div className="flex min-h-64 items-center justify-center gap-2 text-muted-foreground">
                <LoaderCircle className="size-5 animate-spin" /> Đang tải chứng nhận...
              </div>
            ) : items.length === 0 ? (
              <div className="flex min-h-64 flex-col items-center justify-center gap-3 px-6 text-center">
                <CheckCircle2 className="size-10 text-emerald-600" />
                <div>
                  <p className="font-medium">Không có chứng nhận phù hợp</p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Thử đổi trạng thái hoặc từ khóa tìm kiếm.
                  </p>
                </div>
              </div>
            ) : (
              <div className="divide-y">
                {items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    aria-pressed={selectedId === item.id}
                    onClick={() => setSelectedId(item.id)}
                    className={cn(
                      'w-full border-l-4 border-transparent px-4 py-4 text-left transition-colors hover:bg-muted/60',
                      selectedId === item.id && 'border-l-emerald-600 bg-emerald-50/70 dark:bg-emerald-950/20',
                    )}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate font-semibold">{item.standardName}</p>
                        <p className="mt-0.5 truncate text-sm text-muted-foreground">{item.code}</p>
                      </div>
                      <StatusBadge
                        label={STATUS_LABEL[item.verificationStatus]}
                        tone={STATUS_TONE[item.verificationStatus]}
                      />
                    </div>
                    <div className="mt-3 space-y-1 text-sm text-muted-foreground">
                      <p className="flex items-center gap-2 truncate">
                        <Building2 className="size-3.5" /> {item.organizationName}
                      </p>
                      <p className="flex items-center gap-2">
                        <CalendarDays className="size-3.5" /> Nộp {formatDate(item.createdAt, true)}
                      </p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </CardContent>
          <CardFooter>
            <div className="w-full">
              <Pagination
                currentPage={page}
                totalPages={totalPages}
                totalElements={totalElements}
                pageSize={PAGE_SIZE}
                itemLabel="chứng nhận"
                loading={listLoading}
                onPageChange={setPage}
              />
            </div>
          </CardFooter>
        </Card>

        <Card className="min-w-0 xl:sticky xl:top-4">
          {!selectedId ? (
            <CardContent className="flex min-h-[36rem] flex-col items-center justify-center gap-3 text-center">
              <FileSearch className="size-12 text-muted-foreground/60" />
              <div>
                <p className="font-medium">Chưa chọn chứng nhận</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Chọn một bản ghi trong danh sách để bắt đầu đối chiếu.
                </p>
              </div>
            </CardContent>
          ) : detailLoading || !selected ? (
            <CardContent className="flex min-h-[36rem] items-center justify-center gap-2 text-muted-foreground">
              <LoaderCircle className="size-5 animate-spin" /> Đang tải chi tiết...
            </CardContent>
          ) : (
            <>
              <CardHeader className="border-b">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <CardTitle>{selected.standardName}</CardTitle>
                    <CardDescription className="mt-1">
                      {selected.organizationName} · {selected.code}
                    </CardDescription>
                  </div>
                  <StatusBadge
                    label={STATUS_LABEL[selected.verificationStatus]}
                    tone={STATUS_TONE[selected.verificationStatus]}
                  />
                </div>
              </CardHeader>
              <CardContent className="space-y-5">
                <section aria-labelledby="document-heading">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <div>
                      <h2 id="document-heading" className="font-semibold">Tệp chứng nhận</h2>
                      {selected.document && (
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          {selected.document.fileName} · {formatFileSize(selected.document.fileSize)}
                        </p>
                      )}
                    </div>
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
                  <div className="flex min-h-[26rem] items-center justify-center overflow-hidden rounded-xl border bg-slate-100 dark:bg-slate-900">
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
                    ) : documentUrl && selected.document?.contentType === 'application/pdf' ? (
                      <iframe
                        title={`Tệp chứng nhận ${selected.code}`}
                        src={documentUrl}
                        className="h-[32rem] w-full bg-white"
                      />
                    ) : documentUrl ? (
                      <img
                        src={documentUrl}
                        alt={`Chứng nhận ${selected.code}`}
                        className="max-h-[32rem] max-w-full object-contain"
                      />
                    ) : null}
                  </div>
                </section>

                <section aria-labelledby="comparison-heading">
                  <div className="mb-3 flex items-center gap-2">
                    <Award className="size-4 text-emerald-600" />
                    <h2 id="comparison-heading" className="font-semibold">Thông tin cần đối chiếu</h2>
                  </div>
                  <dl className="rounded-xl border bg-muted/20 px-4">
                    <DetailRow label="Số hiệu" value={selected.code} />
                    <DetailRow label="Cơ quan cấp" value={selected.issuedBy || 'Chưa khai báo'} />
                    <DetailRow label="Tiêu chuẩn" value={selected.standardName} />
                    <DetailRow label="Ngày cấp" value={formatDate(selected.issueDate)} />
                    <DetailRow label="Ngày hết hạn" value={formatDate(selected.expiryDate)} />
                    <DetailRow label="Tổ chức" value={selected.organizationName} />
                  </dl>
                </section>

                {selected.validityStatus === 'EXPIRED' && (
                  <Alert variant="warning">
                    <AlertCircle />
                    <AlertTitle>Chứng nhận đã hết hạn</AlertTitle>
                    <AlertDescription>
                      Có thể xác thực tính chân thực, nhưng chứng nhận sẽ không hiển thị là đang đạt chuẩn.
                    </AlertDescription>
                  </Alert>
                )}

                {selected.verificationStatus === 'REJECTED' && selected.rejectionReason && (
                  <Alert variant="destructive">
                    <XCircle />
                    <AlertTitle>Lý do từ chối</AlertTitle>
                    <AlertDescription>{selected.rejectionReason}</AlertDescription>
                  </Alert>
                )}

                {selected.verificationStatus === 'VERIFIED' && (
                  <Alert variant="success">
                    <CheckCircle2 />
                    <AlertTitle>Đã xác thực</AlertTitle>
                    <AlertDescription>
                      {selected.reviewedBy?.fullName || 'Quản trị viên'} · {formatDate(selected.reviewedAt, true)}
                      {selected.reviewNote ? ` · ${selected.reviewNote}` : ''}
                    </AlertDescription>
                  </Alert>
                )}
              </CardContent>
              {selected.verificationStatus === 'PENDING' && (
                <CardFooter className="justify-end gap-2">
                  <Button variant="destructive" onClick={() => setRejectOpen(true)}>
                    <XCircle /> Từ chối
                  </Button>
                  <Button onClick={() => setVerifyOpen(true)} disabled={!selected.document}>
                    <CheckCircle2 /> Xác thực
                  </Button>
                </CardFooter>
              )}
            </>
          )}
        </Card>
      </div>

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
            <Button onClick={() => void handleVerify()} disabled={actionLoading}>
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

export default CertificateVerificationPage;
