import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertCircle, Eye, ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getCertificateVerifications } from '@/api/certificateVerificationApi';
import { toApiError } from '@/api/apiError';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { DataTableShell } from '@/components/common/DataTableShell';
import { FilterSelect } from '@/components/common/FilterSelect';
import { ListCard } from '@/components/common/ListCard';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListToolbar } from '@/components/common/ListToolbar';
import { Pagination } from '@/components/common/Pagination';
import { RefreshButton } from '@/components/common/RefreshButton';
import { SearchInput } from '@/components/common/SearchInput';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
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

export const CertificateVerificationPage = () => {
  const navigate = useNavigate();
  const [items, setItems] = useState<CertificateVerification[]>([]);
  const [status, setStatus] = useState<CertificateVerificationStatus>('PENDING');
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
      setLoading(true);
      setError(null);
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
    } catch (loadError: unknown) {
      setError(toApiError(loadError, 'Không thể tải danh sách chứng nhận.').message);
      setItems([]);
      setTotalElements(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [keyword, page, status]);

  useEffect(() => {
    void loadList();
  }, [loadList]);

  const description = useMemo(
    () => status === 'PENDING'
      ? `${totalElements} chứng nhận đang chờ kiểm tra.`
      : 'Kiểm tra trạng thái xác thực chứng nhận trên toàn hệ thống.',
    [status, totalElements],
  );

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={ShieldCheck}
        title="Xác thực chứng nhận"
        description={description}
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
              ariaLabel="Lọc theo trạng thái xác thực"
              onValueChange={(value) => {
                setStatus((value || 'PENDING') as CertificateVerificationStatus);
                setPage(0);
              }}
              options={STATUS_OPTIONS}
            />
          </>
        }
        right={<RefreshButton onClick={() => void loadList()} loading={loading} />}
      />

      {error && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Không tải được danh sách</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <ListCard>
        <DataTableShell
          loading={loading}
          empty={!loading && items.length === 0}
          colSpan={8}
          loadingMessage="Đang tải chứng nhận..."
          emptyMessage="Không có chứng nhận phù hợp."
          header={
            <>
              <TableHead className="w-16">STT</TableHead>
              <TableHead>Tiêu chuẩn</TableHead>
              <TableHead>Số hiệu</TableHead>
              <TableHead>Tổ chức</TableHead>
              <TableHead>Cơ quan cấp</TableHead>
              <TableHead>Ngày nộp</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </>
          }
          body={items.map((item, index) => (
            <TableRow key={item.id}>
              <TableCell>{page * PAGE_SIZE + index + 1}</TableCell>
              <TableCell className="font-medium">{item.standardName}</TableCell>
              <TableCell className="font-mono text-xs">{item.code}</TableCell>
              <TableCell className="max-w-64 truncate" title={item.organizationName}>
                {item.organizationName}
              </TableCell>
              <TableCell className="max-w-64 truncate" title={item.issuedBy || undefined}>
                {item.issuedBy || '—'}
              </TableCell>
              <TableCell>{formatDate(item.createdAt, true)}</TableCell>
              <TableCell>
                <StatusBadge
                  label={STATUS_LABEL[item.verificationStatus]}
                  tone={STATUS_TONE[item.verificationStatus]}
                  className={item.verificationStatus === 'PENDING' ? 'text-amber-800' : undefined}
                />
              </TableCell>
              <TableCell className="text-right">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate(`/admin/certifications/${item.id}`)}
                >
                  <Eye /> Chi tiết
                </Button>
              </TableCell>
            </TableRow>
          ))}
        />

        <Pagination
          currentPage={page}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={PAGE_SIZE}
          itemLabel="chứng nhận"
          loading={loading}
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
};

export default CertificateVerificationPage;
