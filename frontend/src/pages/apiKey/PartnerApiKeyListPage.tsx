import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Key, PlusCircle, ShieldCheck, Ban } from 'lucide-react';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { getApiKeys } from '@/api/apiKeyApi';
import type { PartnerApiKeyResponse, PartnerApiKeyStatus } from '@/types/apiKey';
import { ApiKeyStatusBadge } from '@/components/apiKey/ApiKeyStatusBadge';
import { RawApiKeyModal } from '@/components/apiKey/RawApiKeyModal';
import { RevokeApiKeyDialog } from '@/components/apiKey/RevokeApiKeyDialog';
import { usePermission } from '@/hooks/usePermission';
import { HelpButton } from '@/components/help/HelpButton';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Pagination } from '@/components/common/Pagination';

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'REVOKED', label: 'Đã thu hồi' },
  { value: 'EXPIRED', label: 'Hết hạn' },
];

export const PartnerApiKeyListPage: React.FC = () => {
  const navigate = useNavigate();
  const canManage = usePermission(['VT-01', 'VT-02']);

  useSetBreadcrumb([
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Khóa API đối tác' },
  ]);

  const [keys, setKeys] = useState<PartnerApiKeyResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Phân trang
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  // States quản lý Modal
  const [newlyCreatedKey, setNewlyCreatedKey] = useState<PartnerApiKeyResponse | null>(null);
  const [revokeKeyTarget, setRevokeKeyTarget] = useState<PartnerApiKeyResponse | null>(null);

  const fetchApiKeys = async () => {
    try {
      setLoading(true);
      const filterStatus = statusFilter === 'ALL' ? undefined : (statusFilter as PartnerApiKeyStatus);
      const data = await getApiKeys(filterStatus, page, pageSize);
      setKeys(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách khóa truy cập đối tác');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApiKeys();
  }, [statusFilter, page]);

  // Lọc dữ liệu client side theo từ khóa tìm kiếm (partnerName hoặc keyPrefix)
  const filteredKeys = useMemo(() => {
    if (!search.trim()) return keys;
    const q = search.toLowerCase().trim();
    return keys.filter(
      (k) =>
        k.partnerName.toLowerCase().includes(q) ||
        k.keyPrefix.toLowerCase().includes(q) ||
        (k.createdByFullName && k.createdByFullName.toLowerCase().includes(q))
    );
  }, [keys, search]);

  const handleRevokeSuccess = () => {
    fetchApiKeys();
  };

  return (
    <div className="space-y-6">
      {/* Header trang */}
      <ListPageHeader
        icon={Key}
        iconBoxClassName="bg-emerald-500/10"
        title="Khóa API bên thứ ba (Partner API Keys)"
        description="Quản lý cấp khóa truy cập, hạn mức gọi API và thu hồi quyền tích hợp dữ liệu của các doanh nghiệp thu mua."
        actions={
          <>
            <HelpButton screenKey="admin-api-keys" />
            {canManage && (
              <Button
                variant="create"
                onClick={() => navigate('/integration/api-keys/create')}
                className="shrink-0 gap-2 shadow-sm"
              >
                <PlusCircle className="w-4 h-4" />
                <span>Cấp khóa mới</span>
              </Button>
            )}
          </>
        }
      />

      {/* Thẻ thống kê tổng quan */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-card">
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-muted-foreground">Tổng số khóa API</p>
              <h3 className="text-2xl font-bold mt-1 text-foreground">{totalElements}</h3>
            </div>
            <div className="p-3 bg-blue-500/10 text-blue-600 dark:text-blue-400 rounded-full">
              <Key className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card">
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-muted-foreground">Đang hoạt động</p>
              <h3 className="text-2xl font-bold mt-1 text-emerald-600 dark:text-emerald-400">
                {keys.filter((k) => k.status === 'ACTIVE').length}
              </h3>
            </div>
            <div className="p-3 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 rounded-full">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card">
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-muted-foreground">Đã thu hồi / Hết hạn</p>
              <h3 className="text-2xl font-bold mt-1 text-rose-600 dark:text-rose-400">
                {keys.filter((k) => k.status === 'REVOKED' || k.status === 'EXPIRED').length}
              </h3>
            </div>
            <div className="p-3 bg-rose-500/10 text-rose-600 dark:text-rose-400 rounded-full">
              <Ban className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Bộ lọc, tìm kiếm và bảng danh sách khóa API */}
      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tên đối tác hoặc tiền tố khóa..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              <FilterSelect
                value={statusFilter}
                onValueChange={(val) => { setStatusFilter(val || 'ALL'); setPage(0); }}
                options={STATUS_OPTIONS}
                placeholder="Lọc trạng thái"
              />
            </>
          }
          right={<RefreshButton onClick={fetchApiKeys} loading={loading} />}
        />

        <DataTableShell
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Tên đối tác / Doanh nghiệp</TableHead>
              <TableHead>Mã nhận diện (Prefix)</TableHead>
              <TableHead className="text-center">Hạn mức (lượt/h)</TableHead>
              <TableHead className="text-center">Lượt gọi (Tổng / Lỗi)</TableHead>
              <TableHead>Thời hạn hết hạn</TableHead>
              <TableHead>Trạng thái</TableHead>
              {canManage && <TableHead className="text-center">Thao tác</TableHead>}
            </>
          }
          body={
            filteredKeys.map((item, index) => (
                    <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
                      <TableCell className="text-center font-medium text-muted-foreground">
                        {page * pageSize + index + 1}
                      </TableCell>
                      <TableCell>
                        <div className="font-semibold text-foreground">{item.partnerName}</div>
                        <div className="text-xs text-muted-foreground mt-0.5">
                          Tạo bởi: {item.createdByFullName || 'Hệ thống'} • {new Date(item.createdAt).toLocaleDateString('vi-VN')}
                        </div>
                      </TableCell>
                      <TableCell>
                        <code className="px-2 py-1 bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 rounded font-mono text-xs border">
                          {item.keyPrefix}
                        </code>
                      </TableCell>
                      <TableCell className="text-center font-medium">
                        <span className="px-2 py-0.5 rounded bg-muted text-foreground text-xs font-semibold">
                          {item.rateLimitPerHour} /h
                        </span>
                      </TableCell>
                      <TableCell className="text-center">
                        <div className="text-sm font-medium">
                          {item.totalCalls} <span className="text-muted-foreground">lượt</span>
                        </div>
                        {item.failedCalls > 0 && (
                          <div className="text-xs text-rose-500 font-medium">
                            {item.failedCalls} lỗi
                          </div>
                        )}
                      </TableCell>
                      <TableCell>
                        <div className="text-xs space-y-0.5">
                          <div className="font-medium text-foreground">
                            {new Date(item.expiresAt).toLocaleDateString('vi-VN')}
                          </div>
                          <div className="text-muted-foreground">
                            {new Date(item.expiresAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <ApiKeyStatusBadge status={item.status} />
                      </TableCell>
                      {canManage && (
                        <TableCell className="text-center">
                          {item.status === 'ACTIVE' ? (
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => setRevokeKeyTarget(item)}
                              title="Thu hồi"
                              className="text-destructive hover:text-destructive hover:bg-muted"
                            >
                              <Ban className="h-4 w-4" />
                            </Button>
                          ) : (
                            <span className="text-xs text-muted-foreground italic">Không có thao tác</span>
                          )}
                        </TableCell>
                      )}
                    </TableRow>
                  ))
            }
            loading={loading}
            empty={!loading && filteredKeys.length === 0}
            colSpan={canManage ? 8 : 7}
            loadingMessage="Đang tải danh sách khóa API..."
            emptyMessage="Không tìm thấy khóa truy cập nào."
          />

          {/* Controls phân trang */}
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            loading={loading}
            itemLabel="khóa"
            onPageChange={setPage}
          />
      </ListCard>

      {/* Modals & Dialogs */}
      <RawApiKeyModal
        open={!!newlyCreatedKey}
        apiKeyData={newlyCreatedKey}
        onClose={() => setNewlyCreatedKey(null)}
      />

      <RevokeApiKeyDialog
        open={!!revokeKeyTarget}
        apiKeyData={revokeKeyTarget}
        onClose={() => setRevokeKeyTarget(null)}
        onSuccess={handleRevokeSuccess}
      />
    </div>
  );
};

export default PartnerApiKeyListPage;
