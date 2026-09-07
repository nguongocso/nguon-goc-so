import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { HelpButton } from '@/components/help/HelpButton';
import { AdministrativeUnitCascadeSelect } from '@/components/common/AdministrativeUnitCascadeSelect';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { RefreshButton } from '@/components/common/RefreshButton';
import { StatCard } from '@/components/common/StatCard';
import { Pagination } from '@/components/common/Pagination';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { assignAreas, getAssignableUsers, getUserAreas, unassignArea } from '@/api/areaAssignmentApi';
import { useAdministrativeUnits } from '@/hooks/useAdministrativeUnits';
import type { AssignedArea, UserOption } from '@/types/areaAssignment';
import { Mail, MapPin, MapPinOff, Phone, RefreshCw, Search, UserCheck, UserRound, X } from 'lucide-react';

const ROLE_VT05 = 'VT-05';
const USER_PAGE_SIZE = 5;

export function AreaAssignmentPage() {
  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Phân công địa bàn' },
  ]);

  const [users, setUsers] = useState<UserOption[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [userPage, setUserPage] = useState(0);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  const [assignedAreas, setAssignedAreas] = useState<AssignedArea[]>([]);
  const [areaCountMap, setAreaCountMap] = useState<Record<string, number>>({});
  const [areasLoading, setAreasLoading] = useState(false);
  const [pendingUnitIds, setPendingUnitIds] = useState<string[]>([]);
  const [assigning, setAssigning] = useState(false);
  const [unassigningId, setUnassigningId] = useState<string | null>(null);

  const { units, loading: unitsLoading, reload: reloadUnits } = useAdministrativeUnits();

  const selectedUser = useMemo(
    () => users.find((user) => user.userId === selectedUserId) ?? null,
    [users, selectedUserId],
  );

  const filteredUsers = useMemo(() => filterUsers(users, keyword), [users, keyword]);

  // Reset trang khi thay đổi từ khóa tìm kiếm
  useEffect(() => {
    setUserPage(0);
  }, [keyword]);

  const totalUserPages = Math.ceil(filteredUsers.length / USER_PAGE_SIZE);
  const pagedUsers = useMemo(() => {
    const start = userPage * USER_PAGE_SIZE;
    return filteredUsers.slice(start, start + USER_PAGE_SIZE);
  }, [filteredUsers, userPage]);

  const loadUsers = useCallback(async () => {
    try {
      setUsersLoading(true);
      const result = await getAssignableUsers({ role: ROLE_VT05 });
      setUsers(result);
      setSelectedUserId((current) =>
        current && result.some((user) => user.userId === current) ? current : null,
      );
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Không thể tải danh sách cán bộ.');
    } finally {
      setUsersLoading(false);
    }
  }, []);

  const loadUserAreas = useCallback(async (userId: string) => {
    try {
      setAreasLoading(true);
      const result = await getUserAreas(userId);
      setAssignedAreas(result);
      setAreaCountMap((prev) => ({ ...prev, [userId]: result.length }));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Không thể tải địa bàn đã gán.');
      setAssignedAreas([]);
    } finally {
      setAreasLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    if (selectedUserId) {
      loadUserAreas(selectedUserId);
    } else {
      setAssignedAreas([]);
    }
  }, [selectedUserId, loadUserAreas]);

  const handleAssign = async () => {
    if (!selectedUserId || pendingUnitIds.length === 0) return;
    try {
      setAssigning(true);
      const result = await assignAreas(selectedUserId, { unitIds: pendingUnitIds });
      const summary =
        result.message ||
        `Đã gán ${result.assignedCount} địa bàn cho tài khoản.`;
      toast.success(summary);
      setPendingUnitIds([]);
      await loadUserAreas(selectedUserId);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Không thể gán địa bàn.');
    } finally {
      setAssigning(false);
    }
  };

  const handleUnassign = async (unitId: string) => {
    if (!selectedUserId) return;
    try {
      setUnassigningId(unitId);
      const result = await unassignArea(selectedUserId, unitId);
      toast.success(result.message || 'Đã gỡ địa bàn khỏi tài khoản.');
      await loadUserAreas(selectedUserId);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Không thể gỡ địa bàn.');
    } finally {
      setUnassigningId(null);
    }
  };

  const handleRefreshAll = () => {
    reloadUnits();
    loadUsers();
    if (selectedUserId) {
      loadUserAreas(selectedUserId);
    }
  };

  const canAssign = Boolean(selectedUserId) && pendingUnitIds.length > 0;
  const isRefreshing = usersLoading || unitsLoading || areasLoading;

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={MapPin}
        title="Phân công địa bàn quản lý"
        description="Gán hoặc gỡ địa bàn phụ trách (tỉnh/xã) cho cán bộ quản lý ngành"
        actions={
          <>
            <HelpButton screenKey="admin-account-areas" />
            <RefreshButton
              onClick={handleRefreshAll}
              loading={isRefreshing}
            />
          </>
        }
      />

      {/* ── Cụm Thống kê Tổng quan ── */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          label="Tổng số cán bộ quản lý ngành"
          value={usersLoading ? '...' : users.length}
          icon={UserRound}
          iconClassName="bg-blue-500/10 text-blue-600 dark:text-blue-400"
        />
        <StatCard
          label="Cán bộ đang chọn"
          value={
            selectedUser ? (
              <span className="truncate block max-w-full text-lg" title={selectedUser.fullName}>
                {selectedUser.fullName}
              </span>
            ) : (
              <span className="text-muted-foreground text-base font-normal">Chưa chọn</span>
            )
          }
          icon={UserCheck}
          iconClassName="bg-purple-500/10 text-purple-600 dark:text-purple-400"
        />
        <StatCard
          label="Địa bàn đã gán"
          value={
            selectedUser ? (
              areasLoading ? (
                '...'
              ) : (
                assignedAreas.length
              )
            ) : (
              <span className="text-muted-foreground text-base font-normal">—</span>
            )
          }
          icon={MapPin}
          iconClassName="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* ── Cột trái: chọn cán bộ + địa bàn đã gán ── */}
        <Card className="flex flex-col shadow-xs">
          <CardHeader className="pb-3 border-b">
            <CardTitle className="text-base font-semibold">Chọn cán bộ</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col space-y-4 pt-4">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                aria-label="Tìm kiếm cán bộ"
                placeholder="Tìm theo tên hoặc tên đăng nhập..."
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                className="pl-9"
              />
            </div>

            {usersLoading ? (
              <div className="flex justify-center py-10">
                <RefreshCw className="h-7 w-7 animate-spin text-primary" />
              </div>
            ) : filteredUsers.length === 0 ? (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Không tìm thấy cán bộ quản lý ngành phù hợp.
              </p>
            ) : (
              <div className="space-y-2">
                <ul className="max-h-56 space-y-1.5 overflow-y-auto rounded-lg border p-1.5" data-testid="user-list">
                  {pagedUsers.map((user) => {
                    const isSelected = user.userId === selectedUserId;
                    const count = isSelected ? assignedAreas.length : areaCountMap[user.userId];
                    return (
                      <li key={user.userId}>
                        <button
                          type="button"
                          onClick={() => setSelectedUserId(user.userId)}
                          className={`w-full rounded-md px-3 py-2 text-left transition-colors flex items-center justify-between gap-2 ${
                            isSelected
                              ? 'bg-emerald-50 border border-emerald-200 text-emerald-900 dark:bg-emerald-950/40 dark:border-emerald-800 dark:text-emerald-100 shadow-2xs'
                              : 'hover:bg-muted/60 border border-transparent'
                          }`}
                        >
                          <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2">
                              <span className="truncate text-sm font-medium">{user.fullName}</span>
                              {isSelected && (
                                <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4 border-emerald-300 bg-emerald-100/60 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-200">
                                  Đang chọn
                                </Badge>
                              )}
                            </div>
                            <span className="block text-xs text-muted-foreground truncate">
                              @{user.username} {user.organizationName ? `· ${user.organizationName}` : ''}
                            </span>
                          </div>
                          {count !== undefined && (
                            <Badge
                              variant="secondary"
                              className={`text-[11px] px-1.5 py-0.5 shrink-0 font-normal ${
                                count > 0
                                  ? 'border border-emerald-300 bg-emerald-100/70 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-200'
                                  : 'bg-muted text-muted-foreground'
                              }`}
                            >
                              {count} địa bàn
                            </Badge>
                          )}
                        </button>
                      </li>
                    );
                  })}
                </ul>

                <Pagination
                  currentPage={userPage}
                  totalPages={totalUserPages}
                  totalElements={filteredUsers.length}
                  pageSize={USER_PAGE_SIZE}
                  loading={usersLoading}
                  itemLabel="cán bộ"
                  onPageChange={setUserPage}
                />
              </div>
            )}

            {selectedUser && (
              <div className="space-y-4 rounded-lg border bg-muted/30 p-4">
                <div className="flex items-start gap-3">
                  <div className="rounded-full bg-emerald-100 p-2 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                    <UserRound className="h-5 w-5" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-foreground">{selectedUser.fullName}</p>
                    <p className="truncate text-xs text-muted-foreground">
                      @{selectedUser.username} · {selectedUser.organizationName}
                    </p>
                    <div className="mt-1.5 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
                      {selectedUser.email && (
                        <span className="inline-flex items-center gap-1">
                          <Mail className="h-3.5 w-3.5" />
                          {selectedUser.email}
                        </span>
                      )}
                      {selectedUser.phone && (
                        <span className="inline-flex items-center gap-1">
                          <Phone className="h-3.5 w-3.5" />
                          {selectedUser.phone}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-foreground">Địa bàn đã gán</p>
                    <span className="text-xs text-muted-foreground">
                      {assignedAreas.length} địa bàn
                    </span>
                  </div>

                  {areasLoading ? (
                    <div className="flex justify-center py-4">
                      <RefreshCw className="h-5 w-5 animate-spin text-primary" />
                    </div>
                  ) : assignedAreas.length === 0 ? (
                    <div
                      className="flex flex-col items-center rounded-lg border border-dashed py-6 text-center"
                      data-testid="empty-assigned-areas"
                    >
                      <MapPinOff className="mb-2 h-8 w-8 text-muted-foreground" />
                      <p className="text-sm font-medium text-foreground">
                        Chưa được phân công địa bàn nào.
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        Chọn địa bàn ở khung bên phải rồi bấm Gán địa bàn.
                      </p>
                    </div>
                  ) : (
                    <ul className="flex flex-wrap gap-2" data-testid="assigned-area-list">
                      {assignedAreas.map((area) => (
                        <li key={area.assignmentId}>
                          <Badge
                            variant="secondary"
                            className="flex items-center gap-1.5 py-1 pl-2.5 pr-1 border border-emerald-200 bg-emerald-50 text-emerald-900 dark:bg-emerald-950/40 dark:border-emerald-800 dark:text-emerald-200"
                          >
                            <MapPin className="h-3.5 w-3.5 text-emerald-600 shrink-0" />
                            <span className="text-xs font-medium">
                              {area.unitName}
                              <span className="ml-1 font-normal text-muted-foreground">
                                ({area.provinceName})
                              </span>
                            </span>
                            <Button
                              size="icon-xs"
                              variant="ghost"
                              aria-label={`Gỡ địa bàn ${area.unitName}`}
                              title="Gỡ địa bàn"
                              disabled={unassigningId === area.unitId}
                              onClick={() => handleUnassign(area.unitId)}
                              className="h-5 w-5 p-0 ml-0.5 rounded-full hover:bg-rose-100 hover:text-rose-700 dark:hover:bg-rose-950/60 transition-colors"
                            >
                              <X className="size-3.5" />
                            </Button>
                          </Badge>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* ── Cột phải: gán địa bàn mới ── */}
        <Card className="flex flex-col shadow-xs">
          <CardHeader className="pb-3 border-b">
            <CardTitle className="text-base font-semibold">Gán địa bàn mới</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col space-y-4 pt-4">
            <AdministrativeUnitCascadeSelect
              units={units}
              value={pendingUnitIds}
              onChange={setPendingUnitIds}
              disabled={unitsLoading}
              loading={unitsLoading}
            />

            {!selectedUser && (
              <p className="text-xs text-muted-foreground">
                Chọn một cán bộ ở cột bên trái để gán địa bàn.
              </p>
            )}

            <Button
              variant="create"
              className="self-end"
              disabled={!canAssign || assigning}
              onClick={handleAssign}
            >
              <RefreshCw className={`h-4 w-4 ${assigning ? 'animate-spin' : ''}`} />
              Gán địa bàn
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function filterUsers(users: UserOption[], keyword: string): UserOption[] {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) return users;
  return users.filter(
    (user) =>
      user.fullName.toLowerCase().includes(normalized) ||
      user.username.toLowerCase().includes(normalized),
  );
}

export default AreaAssignmentPage;

