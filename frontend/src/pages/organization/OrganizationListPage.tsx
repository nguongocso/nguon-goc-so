import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Building2, PlusCircle } from 'lucide-react';
import { toast } from 'sonner';

import { getOrganizations } from '@/api/organizationApi';
import { DataTableShell } from '@/components/common/DataTableShell';
import { FilterSelect } from '@/components/common/FilterSelect';
import { ListCard } from '@/components/common/ListCard';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListToolbar } from '@/components/common/ListToolbar';
import { Pagination } from '@/components/common/Pagination';
import { RefreshButton } from '@/components/common/RefreshButton';
import { SearchInput } from '@/components/common/SearchInput';
import { StatusBadge } from '@/components/common/StatusBadge';
import { HelpButton } from '@/components/help/HelpButton';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { ROLE_ACCESS } from '@/config/roleAccess';
import { usePermission } from '@/hooks/usePermission';
import type { Organization } from '@/types/organization';
import { ORGANIZATION_TYPES } from '@/utils/constants';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'INACTIVE', label: 'Ngừng hoạt động' },
];

/**
 * Trang danh sách tổ chức trong hệ thống (dành cho Admin VT-01).
 */
export function OrganizationListPage() {
  const navigate = useNavigate();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);

  const canCreate = usePermission(ROLE_ACCESS.organizationCreate);

  // Tìm kiếm, lọc trạng thái & phân trang (client-side)
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const data = await getOrganizations();

      const mappedData: Organization[] = data.map((item: unknown) => {
        const org = item as {
          organizationID?: string;
          id?: string;
          organizationName?: string;
          name?: string;
          organizationCode?: string;
          code?: string;
          organizationType?: string;
          type?: string;
          status?: 'ACTIVE' | 'INACTIVE';
          createdAt?: string;
          updatedAt?: string;
        };
        return {
          id: org.organizationID || org.id || '',
          name: org.organizationName || org.name || '',
          code: org.organizationCode || org.code || '',
          type: (org.organizationType || org.type || 'COOPERATIVE') as Organization['type'],
          status: org.status || 'ACTIVE',
          createdAt: org.createdAt || '',
          updatedAt: org.updatedAt,
        };
      });

      setOrganizations(mappedData);
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      toast.error(axiosError.response?.data?.message || 'Không thể tải danh sách tổ chức');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchOrganizations();
  }, []);

  // Lọc theo từ khóa + trạng thái (client-side)
  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim();
    return organizations.filter((org) => {
      const matchKeyword =
        !q ||
        org.name.toLowerCase().includes(q) ||
        org.code.toLowerCase().includes(q);
      const matchStatus = statusFilter === 'ALL' || org.status === statusFilter;
      return matchKeyword && matchStatus;
    });
  }, [organizations, search, statusFilter]);

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);

  // Phân trang client-side
  const paginated = useMemo(() => {
    const start = page * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  const getTypeLabel = (type: string) => {
    return ORGANIZATION_TYPES[type as keyof typeof ORGANIZATION_TYPES] || type;
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <StatusBadge label="Đang hoạt động" tone="success" />;
      case 'INACTIVE':
        return <StatusBadge label="Ngừng hoạt động" tone="neutral" />;
      default:
        return <StatusBadge label={status} tone="neutral" />;
    }
  };

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={Building2}
        title="Quản lý tổ chức"
        description="Danh sách tất cả tổ chức tham gia hệ thống."
        actions={
          <>
            <HelpButton screenKey="organization-list" />
            {canCreate && (
              <Button onClick={() => navigate('/organizations/create')}>
                <PlusCircle className="mr-2 h-4 w-4" />
                Tạo tổ chức
              </Button>
            )}
          </>
        }
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm kiếm theo tên, mã tổ chức..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
              <FilterSelect
                placeholder="Tất cả trạng thái"
                value={statusFilter}
                onValueChange={(val) => {
                  setStatusFilter(val || 'ALL');
                  setPage(0);
                }}
                options={STATUS_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={() => void fetchOrganizations()} loading={loading} />}
        />

        <DataTableShell
          loading={loading}
          loadingMessage="Đang tải danh sách tổ chức..."
          empty={!loading && paginated.length === 0}
          emptyMessage="Không tìm thấy tổ chức nào."
          colSpan={6}
          header={
            <>
              <TableHead className="w-12">#</TableHead>
              <TableHead>Mã tổ chức</TableHead>
              <TableHead>Tên tổ chức</TableHead>
              <TableHead>Loại tổ chức</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </>
          }
          body={
            <>
              {paginated.map((org, index) => (
                <TableRow key={org.id} className="hover:bg-muted/50">
                  <TableCell className="font-medium text-muted-foreground">
                    {page * PAGE_SIZE + index + 1}
                  </TableCell>
                  <TableCell>
                    <span className="font-mono text-xs font-semibold bg-muted px-2 py-1 rounded">
                      {org.code}
                    </span>
                  </TableCell>
                  <TableCell className="font-medium">{org.name}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{getTypeLabel(org.type)}</Badge>
                  </TableCell>
                  <TableCell>{getStatusBadge(org.status)}</TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => navigate(`/organizations/${org.id}`)}
                    >
                      Chi tiết
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </>
          }
        />

        {totalPages > 1 && (
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={filtered.length}
            pageSize={PAGE_SIZE}
            onPageChange={setPage}
          />
        )}
      </ListCard>
    </div>
  );
}

export default OrganizationListPage;
