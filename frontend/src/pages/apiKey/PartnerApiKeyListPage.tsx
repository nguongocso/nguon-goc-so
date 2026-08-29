import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import {
  Key,
  PlusCircle,
  RefreshCw,
  Search,
  ShieldCheck,
  Ban,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Card, CardContent } from '@/components/ui/card';
import { getApiKeys } from '@/api/apiKeyApi';
import type { PartnerApiKeyResponse, PartnerApiKeyStatus } from '@/types/apiKey';
import { ApiKeyStatusBadge } from '@/components/apiKey/ApiKeyStatusBadge';
import { RawApiKeyModal } from '@/components/apiKey/RawApiKeyModal';
import { RevokeApiKeyDialog } from '@/components/apiKey/RevokeApiKeyDialog';
import { usePermission } from '@/hooks/usePermission';
import { HelpButton } from '@/components/help/HelpButton';

const STATUS_LABELS: Record<string, string> = {
  ALL: 'Tất cả trạng thái',
  ACTIVE: 'Đang hoạt động',
  REVOKED: 'Đã thu hồi',
  EXPIRED: 'Hết hạn',
};

export const PartnerApiKeyListPage: React.FC = () => {
  const navigate = useNavigate();
  const canManage = usePermission(['VT-01', 'VT-02']);

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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 bg-emerald-500/10 rounded-lg text-emerald-600 dark:text-emerald-400">
              <Key className="w-6 h-6" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Khóa API bên thứ ba (Partner API Keys)
            </h1>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Quản lý cấp khóa truy cập, hạn mức gọi API và thu hồi quyền tích hợp dữ liệu của các doanh nghiệp thu mua.
          </p>
        </div>

        <div className="flex items-center gap-2">
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
        </div>
      </div>

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

      {/* Thanh bộ lọc & Tìm kiếm */}
      <Card>
        <CardContent className="p-4 space-y-4">
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
            <div className="flex flex-1 flex-col sm:flex-row items-stretch sm:items-center gap-3">
              {/* Ô tìm kiếm */}
              <div className="relative flex-1 max-w-md">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="Tìm theo tên đối tác hoặc tiền tố khóa..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-9"
                />
              </div>

              {/* Lọc theo trạng thái */}
              <div className="w-full sm:w-48">
                <Select value={statusFilter} onValueChange={(val) => { if (val) setStatusFilter(val); setPage(0); }}
                  items={[
                    { value: 'ALL', label: 'Tất cả trạng thái' },
                    { value: 'ACTIVE', label: 'Đang hoạt động' },
                    { value: 'REVOKED', label: 'Đã thu hồi' },
                    { value: 'EXPIRED', label: 'Hết hạn' },
                  ]}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Lọc trạng thái">
                      {STATUS_LABELS[statusFilter] || statusFilter}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
                    <SelectItem value="ACTIVE">Đang hoạt động</SelectItem>
                    <SelectItem value="REVOKED">Đã thu hồi</SelectItem>
                    <SelectItem value="EXPIRED">Hết hạn</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Nút làm mới */}
            <Button variant="outline" onClick={fetchApiKeys} disabled={loading}>
              <RefreshCw className={`size-4 ${loading ? 'animate-spin' : ''}`} />
              Làm mới
            </Button>
          </div>

          {/* Bảng danh sách khóa API */}
          <div className="rounded-md border overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="w-12 text-center">STT</TableHead>
                  <TableHead>Tên đối tác / Doanh nghiệp</TableHead>
                  <TableHead>Mã nhận diện (Prefix)</TableHead>
                  <TableHead className="text-center">Hạn mức (lượt/h)</TableHead>
                  <TableHead className="text-center">Lượt gọi (Tổng / Lỗi)</TableHead>
                  <TableHead>Thời hạn hết hạn</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  {canManage && <TableHead className="text-center">Thao tác</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="h-32 text-center text-muted-foreground">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <RefreshCw className="w-6 h-6 animate-spin text-emerald-600" />
                        <span>Đang tải danh sách khóa API...</span>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : filteredKeys.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="h-32 text-center text-muted-foreground">
                      Không tìm thấy khóa truy cập nào.
                    </TableCell>
                  </TableRow>
                ) : (
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
                )}
              </TableBody>
            </Table>
          </div>

          {/* Controls phân trang */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-2 text-xs sm:text-sm text-muted-foreground">
              <div>
                Hiển thị {page * pageSize + 1} - {Math.min((page + 1) * pageSize, totalElements)} trên tổng số {totalElements} khóa
              </div>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0 || loading}
                >
                  <ChevronLeft className="w-4 h-4 mr-1" />
                  Trang trước
                </Button>
                <span className="px-2 font-medium">
                  {page + 1} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1 || loading}
                >
                  Trang sau
                  <ChevronRight className="w-4 h-4 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

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
