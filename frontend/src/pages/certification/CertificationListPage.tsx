import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Award, PlusCircle } from 'lucide-react';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Pagination } from '@/components/common/Pagination';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { getCertifications } from '@/api/certificationApi';
import type { CertificationResponse } from '@/types/certification';
import type { PageResponse } from '@/types/common';
import { CertificationStatusBadge } from '@/components/certification/CertificationStatusBadge';
import { CertificationDetailDialog } from '@/components/certification/CertificationDetailDialog';
import { usePermission } from '@/hooks/usePermission';
import { HelpButton } from '@/components/help/HelpButton';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';

type SortField = 'name' | 'issueDate' | 'expiryDate' | 'status';

const PAGE_SIZE = 10;
const SEARCH_DEBOUNCE_MS = 400;

const STATUS_FILTER_OPTIONS = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'valid', label: 'Còn hiệu lực' },
  { value: 'expiring', label: 'Sắp hết hạn' },
  { value: 'expired', label: 'Hết hạn' },
];

const CertificationListPage = () => {
  const navigate = useNavigate();
  const canCreate = usePermission(['VT-02']);

  useSetBreadcrumb([
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Chứng nhận' },
  ]);

  const [data, setData] = useState<PageResponse<CertificationResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchInput, setSearchInput] = useState('');
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [sortField, setSortField] = useState<SortField>('issueDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const [selectedCertId, setSelectedCertId] = useState<string | null>(null);

  // Debounce ô tìm kiếm: chỉ gọi API sau khi người dùng ngừng gõ
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(searchInput.trim());
      setPage(0);
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const fetchCertifications = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getCertifications({
        keyword: query || undefined,
        status: statusFilter !== 'all' ? (statusFilter as 'valid' | 'expiring' | 'expired') : undefined,
        sortBy: sortField === 'status' ? 'expiryDate' : sortField,
        sortDir,
        page,
        size: PAGE_SIZE,
      });
      setData(result);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách chứng nhận');
    } finally {
      setLoading(false);
    }
  }, [query, statusFilter, sortField, sortDir, page]);

  useEffect(() => {
    fetchCertifications();
  }, [fetchCertifications]);

  const items = data?.items ?? [];
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const totalElements = data?.totalElements ?? 0;

  const toggleSortDir = (field: SortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('asc');
    }
    setPage(0);
  };

  const sortIndicator = (field: SortField) => {
    if (sortField !== field) return '';
    return sortDir === 'asc' ? ' ↑' : ' ↓';
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr + 'T00:00:00').toLocaleDateString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header trang */}
      <ListPageHeader
        icon={Award}
        title="Quản lý chứng nhận"
        description="Danh sách các chứng nhận của tổ chức bạn"
        actions={
          <>
            <HelpButton screenKey="certification-list" />
            {canCreate && (
              <Button variant="create" onClick={() => navigate('/certifications/create')}>
                <PlusCircle className="h-4 w-4 mr-1" />
                Tạo chứng nhận
              </Button>
            )}
          </>
        }
      />

      {/* Card chính: toolbar + bảng + phân trang */}
      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên, mã hoặc cơ quan cấp..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
              />
              <FilterSelect
                value={statusFilter}
                onValueChange={(v) => {
                  setStatusFilter(v ?? 'all');
                  setPage(0);
                }}
                options={STATUS_FILTER_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={fetchCertifications} loading={loading} />}
        />

        {/* Bảng */}
        <DataTableShell
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead
                className="cursor-pointer select-none"
                onClick={() => toggleSortDir('name')}
              >
                Tên chứng nhận{sortIndicator('name')}
              </TableHead>
              <TableHead>Mã</TableHead>
              <TableHead>Cơ quan cấp</TableHead>
              <TableHead
                className="cursor-pointer select-none"
                onClick={() => toggleSortDir('issueDate')}
              >
                Ngày cấp{sortIndicator('issueDate')}
              </TableHead>
              <TableHead
                className="cursor-pointer select-none"
                onClick={() => toggleSortDir('expiryDate')}
              >
                Ngày hết hạn{sortIndicator('expiryDate')}
              </TableHead>
              <TableHead
                className="cursor-pointer select-none"
                onClick={() => toggleSortDir('status')}
              >
                Trạng thái{sortIndicator('status')}
              </TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={items.map((cert, index) => (
            <TableRow key={cert.id} className="hover:bg-muted/40 transition-colors">
              <TableCell className="text-center font-medium text-muted-foreground">
                {page * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell className="max-w-[280px] truncate font-medium" title={cert.name}>
                {cert.name}
              </TableCell>
              <TableCell className="whitespace-nowrap font-mono text-sm">{cert.code}</TableCell>
              <TableCell className="max-w-[220px] truncate" title={cert.issuedBy || undefined}>
                {cert.issuedBy || '—'}
              </TableCell>
              <TableCell className="whitespace-nowrap">{formatDate(cert.issueDate)}</TableCell>
              <TableCell className="whitespace-nowrap">{formatDate(cert.expiryDate)}</TableCell>
              <TableCell>
                <CertificationStatusBadge
                  isValid={cert.isValid}
                  expiryDate={cert.expiryDate}
                />
              </TableCell>
              <TableCell className="text-center">
                <Button
                  variant="view"
                  size="sm"
                  onClick={() => setSelectedCertId(cert.id)}
                >
                  Xem
                </Button>
              </TableCell>
            </TableRow>
          ))}
          loading={loading}
          empty={!loading && items.length === 0}
          colSpan={8}
          loadingMessage="Đang tải dữ liệu..."
          emptyMessage={
            query || statusFilter !== 'all'
              ? 'Không tìm thấy chứng nhận nào phù hợp với bộ lọc.'
              : 'Chưa có chứng nhận nào. Nhấn "Tạo chứng nhận" để thêm mới.'
          }
        />

        {/* Phân trang */}
        <Pagination
          currentPage={page}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="chứng nhận"
          onPageChange={setPage}
        />
      </ListCard>

      {/* Detail Dialog */}
      <CertificationDetailDialog
        certification={selectedCertId ? items.find((c) => c.id === selectedCertId) ?? null : null}
        open={!!selectedCertId}
        onClose={() => setSelectedCertId(null)}
      />
    </div>
  );
};

export default CertificationListPage;
