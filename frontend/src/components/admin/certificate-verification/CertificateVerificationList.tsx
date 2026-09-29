import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertCircle, ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getCertificateVerifications } from '@/api/certificateVerificationApi';
import { toApiError } from '@/api/apiError';
import { CertificateVerificationTable } from '@/components/admin/certificate-verification/CertificateVerificationTable';
import { FilterSelect } from '@/components/common/FilterSelect';
import { ListCard } from '@/components/common/ListCard';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListToolbar } from '@/components/common/ListToolbar';
import { Pagination } from '@/components/common/Pagination';
import { RefreshButton } from '@/components/common/RefreshButton';
import { SearchInput } from '@/components/common/SearchInput';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
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

export const CertificateVerificationList = () => {
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

      <ListCard>
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

        <CertificateVerificationTable
          items={items}
          loading={loading}
          startIndex={page * PAGE_SIZE}
          onViewDetail={(item) => navigate(`/admin/certifications/${item.id}`)}
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

export default CertificateVerificationList;
