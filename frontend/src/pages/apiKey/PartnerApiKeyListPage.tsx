import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Key, PlusCircle, ShieldCheck, Ban, FlaskConical, BookOpen, CalendarPlus, TrendingUp, Webhook, History } from 'lucide-react';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { getApiKeys } from '@/api/apiKeyApi';
import { toApiError } from '@/api/apiError';
import type { PartnerApiKeyResponse, PartnerApiKeyStatus } from '@/types/apiKey';
import { ApiKeyStatusBadge } from '@/components/apiKey/ApiKeyStatusBadge';
import { RawApiKeyModal } from '@/components/apiKey/RawApiKeyModal';
import { RevokeApiKeyDialog } from '@/components/apiKey/RevokeApiKeyDialog';
import { RenewApiKeyDialog } from '@/components/apiKey/RenewApiKeyDialog';
import { UpdateApiKeyQuotaDialog } from '@/components/apiKey/UpdateApiKeyQuotaDialog';
import { WebhookConfigModal } from '@/components/apiKey/WebhookConfigModal';

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
  const [searchParams, setSearchParams] = useSearchParams();
  // Chỉ Quản lý HTX (VT-02) và Quản trị viên (VT-01) mới có quyền quản lý và cấp khóa (TC-04)
  const canManage = usePermission(['VT-01', 'VT-02']);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
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
  const [renewKeyTarget, setRenewKeyTarget] = useState<PartnerApiKeyResponse | null>(null);
  const [quotaKeyTarget, setQuotaKeyTarget] = useState<PartnerApiKeyResponse | null>(null);
  const [webhookConfigTarget, setWebhookConfigTarget] = useState<PartnerApiKeyResponse | null>(null);


  const fetchApiKeys = async () => {
    try {
      setLoading(true);
      const filterStatus = statusFilter === 'ALL' ? undefined : (statusFilter as PartnerApiKeyStatus);
      const data = await getApiKeys(filterStatus, page, pageSize);
      setKeys(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (error: unknown) {
      // Chuẩn hoá lỗi API và luôn reset trạng thái tải để tránh treo UI
      toast.error(toApiError(error, 'Không thể tải danh sách khóa truy cập đối tác').message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApiKeys();
  }, [statusFilter, page]);

  // NCL-12-CN-005: lối tắt từ thông báo/cảnh báo tổng hợp dạng
  // /integration/api-keys?keyId=...&action=renew|quota → tự mở đúng dialog cho key theo keyId.
  // Nếu key chưa nằm trong trang đang hiển thị, tra cứu thêm trên toàn bộ khóa của tổ chức.
  // Xử lý xong xoá params để tránh effect chạy lại (refresh/back không mở trùng dialog).
  useEffect(() => {
    const keyId = searchParams.get('keyId');
    const action = searchParams.get('action');
    if (!keyId) return;
    if (!canManage || (action !== 'renew' && action !== 'quota')) {
      setSearchParams({}, { replace: true });
      return;
    }

    let cancelled = false;
    const openFor = (target: PartnerApiKeyResponse | null) => {
      if (cancelled) return;
      if (target) {
        if (action === 'renew') setRenewKeyTarget(target);
        else setQuotaKeyTarget(target);
      }
      setSearchParams({}, { replace: true });
    };

    const target = keys.find((k) => k.id === keyId);
    if (target) {
      openFor(target);
      return;
    }

    // Key chưa nằm trong trang hiện tại → tra cứu toàn bộ khóa của tổ chức (giới hạn 500) rồi tìm đúng keyId
    (async () => {
      try {
        const data = await getApiKeys(undefined, 0, 500);
        openFor(data.content?.find((k) => k.id === keyId) ?? null);
      } catch {
        openFor(null);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [keys, canManage, searchParams, setSearchParams]);

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
            <Button
              variant="outline"
              onClick={() => window.open('/portal', '_blank')}
              className="shrink-0 gap-2"
              title="Mở trang tài liệu Cổng dữ liệu Nguồn Gốc Số dành cho bên thứ ba"
            >
              <BookOpen className="w-4 h-4 text-primary" />
              <span className="hidden sm:inline">Tài liệu cổng dữ liệu</span>
            </Button>
            {canManage && (
              <>
                <Button
                  variant="outline"
                  onClick={() => navigate('/integration/api-keys/create-test')}
                  className="shrink-0 gap-2 border-primary/40 text-primary hover:bg-primary/10"
                >
                  <FlaskConical className="w-4 h-4 text-primary" />
                  <span>Cấp khóa thử nghiệm</span>
                </Button>

                <Button
                  variant="create"
                  onClick={() => navigate('/integration/api-keys/create')}
                  className="shrink-0 gap-2 shadow-sm"
                >
                  <PlusCircle className="w-4 h-4" />
                  <span>Cấp khóa mới</span>
                </Button>
              </>
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
              <TableHead className="text-center">Lượt gọi hôm nay</TableHead>
              <TableHead className="text-center">Lượt gọi (Tổng / Lỗi)</TableHead>
              <TableHead className="whitespace-nowrap">Thời hạn hết hạn</TableHead>
              <TableHead className="whitespace-nowrap">Trạng thái</TableHead>
              <TableHead className="text-center align-middle whitespace-nowrap min-w-[144px]">Thao tác</TableHead>
              <TableHead className="whitespace-nowrap">Kênh nhận tin thu hồi</TableHead>
            </>
          }
          body={
            filteredKeys.map((item, index) => (
                    <TableRow key={item.id} className="hover:bg-muted/40 transition-colors align-middle">
                      <TableCell className="text-center align-middle font-medium text-muted-foreground">
                        {page * pageSize + index + 1}
                      </TableCell>
                      <TableCell className="align-middle">
                        <div className="flex items-center gap-2 leading-tight">
                          <span className="font-semibold text-foreground">{item.partnerName}</span>
                          {(item.isTest || item.is_test) && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-blue-50 text-blue-700 dark:bg-blue-950/70 dark:text-blue-300 border border-blue-200 dark:border-blue-800 whitespace-nowrap">
                              <FlaskConical className="w-3 h-3" />
                              Thử nghiệm
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-muted-foreground mt-0.5 leading-tight">
                          Tạo bởi: {item.createdByFullName || item.createdByName || 'Hệ thống'} • {new Date(item.createdAt).toLocaleDateString('vi-VN')}
                        </div>
                      </TableCell>
                      <TableCell className="align-middle">
                        <code className="px-2 py-1 bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 rounded font-mono text-xs border whitespace-nowrap">
                          {item.keyPrefix}
                        </code>
                      </TableCell>
                      <TableCell className="text-center align-middle font-medium">
                        <span className="inline-block px-2 py-0.5 rounded bg-muted text-foreground text-xs font-semibold whitespace-nowrap leading-tight">
                          {item.rateLimitPerHour} /h
                        </span>
                      </TableCell>
                      <TableCell className="text-center align-middle">
                        <div className="flex flex-col items-center gap-0.5 leading-tight">
                          <div className="text-sm font-medium whitespace-nowrap">{item.usedCallsToday ?? 0} <span className="text-muted-foreground">hôm nay</span></div>
                          <div className="text-[11px] text-muted-foreground whitespace-nowrap">{item.currentHourCalls ?? 0} lượt giờ này</div>
                          {/* Badge cảnh báo hạn mức theo giờ (NCL-12-CN-005) */}
                          {item.quotaWarningThreshold != null && item.quotaWarningThreshold > 0 && (item.currentHourCalls ?? 0) >= item.quotaWarningThreshold && (
                            <div className="mt-0.5 leading-tight">
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-700 dark:bg-amber-950/70 dark:text-amber-300 border border-amber-200 dark:border-amber-800 whitespace-nowrap">
                                Sắp chạm hạn mức
                              </span>
                            </div>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-center align-middle">
                        <div className="flex flex-col items-center gap-0.5 leading-tight">
                          <div className="text-sm font-medium whitespace-nowrap">
                            {item.totalCalls} <span className="text-muted-foreground">lượt</span>
                          </div>
                          {item.failedCalls > 0 && (
                            <div className="text-xs text-rose-500 font-medium whitespace-nowrap">
                              {item.failedCalls} lỗi
                            </div>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="align-middle">
                        <div className="flex flex-col gap-0.5 text-xs leading-tight">
                          <div className="font-medium text-foreground whitespace-nowrap">
                            {new Date(item.expiresAt).toLocaleDateString('vi-VN')}
                          </div>
                          <div className="text-muted-foreground whitespace-nowrap">
                            {new Date(item.expiresAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="align-middle">
                        <ApiKeyStatusBadge status={item.status} />
                      </TableCell>
                      <TableCell className="text-center align-middle">
                        {(item.status === 'ACTIVE' || item.status === 'EXPIRED') ? (
                          <div className="flex items-center justify-center gap-1">
                            {canManage && (
                              <>
                                <Button
                                  variant="outline"
                                  size="icon-sm"
                                  onClick={() => setRenewKeyTarget(item)}
                                  title="Gia hạn"
                                  aria-label="Gia hạn khóa API"
                                  className="size-8 shrink-0 rounded-full border-amber-300 bg-white text-amber-700 hover:text-amber-700 hover:bg-amber-50 dark:bg-transparent dark:hover:bg-amber-950/30 inline-flex items-center justify-center"
                                >
                                  <CalendarPlus className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="outline"
                                  size="icon-sm"
                                  onClick={() => setQuotaKeyTarget(item)}
                                  title="Nâng hạn mức"
                                  aria-label="Nâng hạn mức khóa API"
                                  className="size-8 shrink-0 rounded-full border-emerald-300 bg-white text-emerald-700 hover:text-emerald-700 hover:bg-emerald-50 dark:bg-transparent dark:hover:bg-emerald-950/30 inline-flex items-center justify-center"
                                >
                                  <TrendingUp className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon-sm"
                                  onClick={() => setWebhookConfigTarget(item)}
                                  title="Khai báo thông tin nhận thông báo thu hồi"
                                  className="text-primary hover:text-primary hover:bg-primary/10"
                                >
                                  <Webhook className="h-4 w-4" />
                                </Button>
                              </>
                            )}
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => navigate(`/integration/api-keys/${item.id}/notifications`, { state: { apiKey: item } })}
                              title="Lịch sử gửi thông báo thu hồi"
                              className="text-muted-foreground hover:text-foreground hover:bg-muted"
                            >
                              <History className="h-4 w-4" />
                            </Button>
                            {canManage && item.status === 'ACTIVE' && (
                              <Button
                                variant="outline"
                                size="icon-sm"
                                onClick={() => setRevokeKeyTarget(item)}
                                title="Thu hồi"
                                aria-label="Thu hồi khóa API"
                                className="size-8 shrink-0 rounded-full border-destructive/50 bg-white text-destructive hover:text-destructive hover:bg-destructive/10 dark:bg-transparent inline-flex items-center justify-center"
                              >
                                <Ban className="h-4 w-4" />
                              </Button>
                            )}
                          </div>
                        ) : item.status === 'REVOKED' ? (
                          <div className="flex min-h-8 items-center justify-center">
                            <span className="text-xs text-muted-foreground italic whitespace-nowrap">Đã thu hồi</span>
                          </div>
                        ) : (
                          <div className="flex min-h-8 items-center justify-center">
                            <span className="text-xs text-muted-foreground italic whitespace-nowrap">Không có thao tác</span>
                          </div>
                        )}
                      </TableCell>
                      <TableCell className="align-middle">
                        {item.webhookUrl ? (
                          <div className="space-y-0.5">
                            <div className="flex items-center gap-1.5">
                              <span
                                className={`inline-block w-2 h-2 rounded-full shrink-0 ${
                                  item.isWebhookActive !== false ? 'bg-emerald-500' : 'bg-amber-500'
                                }`}
                              />
                              <span
                                className="font-mono text-xs text-foreground truncate max-w-[130px]"
                                title={item.webhookUrl}
                              >
                                {item.webhookUrl.replace(/^https?:\/\//, '')}
                              </span>
                            </div>
                            <div className="text-[11px]">
                              {item.isWebhookActive !== false ? (
                                <span className="text-emerald-600 dark:text-emerald-400 font-medium">Đang nhận tin</span>
                              ) : (
                                <span className="text-amber-600 dark:text-amber-400 font-medium">Tạm dừng</span>
                              )}
                            </div>
                          </div>
                        ) : (
                          <span className="text-xs text-muted-foreground italic">Chưa cấu hình</span>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
            }
            loading={loading}
            empty={!loading && filteredKeys.length === 0}
            colSpan={10}
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

      <RenewApiKeyDialog
        open={!!renewKeyTarget}
        apiKeyData={renewKeyTarget}
        onClose={() => setRenewKeyTarget(null)}
        onSuccess={() => {
          fetchApiKeys();
        }}
      />

      <UpdateApiKeyQuotaDialog
        open={!!quotaKeyTarget}
        apiKeyData={quotaKeyTarget}
        onClose={() => setQuotaKeyTarget(null)}
        onSuccess={() => {
          fetchApiKeys();
        }}
      />

      <WebhookConfigModal
        open={!!webhookConfigTarget}
        apiKey={webhookConfigTarget}
        onClose={() => setWebhookConfigTarget(null)}
        onSuccess={fetchApiKeys}
      />
    </div>
  );
};

export default PartnerApiKeyListPage;

