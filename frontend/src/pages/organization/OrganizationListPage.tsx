import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { PlusCircle, RefreshCw, Search } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { HelpButton } from '@/components/help/HelpButton';
import { DataTablePagination } from '@/components/common/DataTablePagination';
import { getOrganizations } from '@/api/organizationApi';
import { type Organization } from '@/types/organization';
import { ORGANIZATION_TYPES } from '@/utils/constants';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';

const PAGE_SIZE = 10;

export function OrganizationListPage() {
  const navigate = useNavigate();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const canCreate = usePermission(ROLE_ACCESS.organizationCreate);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const data = await getOrganizations();

      const mappedData: Organization[] = data.map((item: any) => ({
        id: item.organizationID,
        name: item.organizationName,
        code: item.organizationCode,
        type: item.organizationType,
        status: item.status,
        createdAt: item.createdAt,
        updatedAt: item.updatedAt,
      }));

      setOrganizations(mappedData);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách tổ chức');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrganizations();
  }, []);

  const filteredOrganizations = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return organizations;
    return organizations.filter((org) => {
      const name = org.name.toLowerCase();
      const code = (org.code ?? '').toLowerCase();
      const type = (
        ORGANIZATION_TYPES[org.type as keyof typeof ORGANIZATION_TYPES] || org.type
      ).toLowerCase();
      return name.includes(keyword) || code.includes(keyword) || type.includes(keyword);
    });
  }, [organizations, search]);

  const totalPages = Math.max(1, Math.ceil(filteredOrganizations.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedOrganizations = filteredOrganizations.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const getStatusBadge = (status: string) => {
    const variants: Record<string, 'default' | 'destructive' | 'secondary'> = {
      ACTIVE: 'default',
      INACTIVE: 'destructive',
    };
    const labels: Record<string, string> = {
      ACTIVE: 'Đang hoạt động',
      INACTIVE: 'Ngừng hoạt động',
    };
    return <Badge variant={variants[status] || 'secondary'}>{labels[status] || status}</Badge>;
  };

  const getTypeLabel = (type: string) => {
    return ORGANIZATION_TYPES[type as keyof typeof ORGANIZATION_TYPES] || type;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Danh sách tổ chức
          </h1>
          <p className="text-sm text-muted-foreground">
            Quản lý các hợp tác xã, doanh nghiệp và tổ chức trong hệ thống.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="organization-list" />
          <Button
            variant="outline"
            size="sm"
            onClick={fetchOrganizations}
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
          {canCreate && (
            <Button variant="create" size="sm" onClick={() => navigate('/organizations/create')}>
              <PlusCircle className="h-4 w-4 mr-1" />
              Tạo tổ chức
            </Button>
          )}
        </div>
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-3">
          <div className="flex min-w-0 items-center justify-between gap-3 sm:justify-start sm:gap-6">
            <span className="text-sm font-medium text-slate-500">Tổng số: {organizations.length}</span>
          </div>
          <div className="relative w-full max-w-xs shrink-0">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              className="pl-9"
              placeholder="Tìm theo mã, tên hoặc loại tổ chức..."
              aria-label="Tìm kiếm tổ chức"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>
        <CardContent className="p-0">
          {loading ? (
            <div className="text-center py-12 text-muted-foreground">Đang tải...</div>
          ) : filteredOrganizations.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              {search.trim()
                ? 'Không tìm thấy tổ chức phù hợp.'
                : 'Chưa có tổ chức nào. Nhấn "Tạo tổ chức" để thêm mới.'}
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50/80">
                    <TableHead className="font-semibold text-slate-700">Mã</TableHead>
                    <TableHead className="font-semibold text-slate-700">Tên tổ chức</TableHead>
                    <TableHead className="font-semibold text-slate-700">Loại</TableHead>
                    <TableHead className="font-semibold text-slate-700">Trạng thái</TableHead>
                    <TableHead className="font-semibold text-slate-700">Ngày tạo</TableHead>
                    <TableHead className="text-right font-semibold text-slate-700">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedOrganizations.map((org) => (
                    <TableRow key={org.id} className="hover:bg-slate-50/60">
                      <TableCell className="font-medium text-slate-900">{org.code}</TableCell>
                      <TableCell className="font-medium text-slate-900">{org.name}</TableCell>
                      <TableCell>{getTypeLabel(org.type)}</TableCell>
                      <TableCell>{getStatusBadge(org.status)}</TableCell>
                      <TableCell>{new Date(org.createdAt).toLocaleDateString('vi-VN')}</TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/organizations/${org.id}`)}
                          className="h-8 text-xs"
                        >
                          Xem
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              </div>
              <DataTablePagination
                page={safePage}
                pageSize={PAGE_SIZE}
                totalElements={filteredOrganizations.length}
                onPageChange={setPage}
                itemLabel="tổ chức"
              />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}