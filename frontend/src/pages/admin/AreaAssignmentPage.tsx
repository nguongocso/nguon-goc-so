import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import {
  HelpButton,
} from '@/components/help/HelpButton';
import { AdministrativeUnitCascadeSelect } from '@/components/common/AdministrativeUnitCascadeSelect';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { assignAreas, getAssignableUsers, getUserAreas, unassignArea } from '@/api/areaAssignmentApi';
import { useAdministrativeUnits } from '@/hooks/useAdministrativeUnits';
import type { AssignedArea, UserOption } from '@/types/areaAssignment';
import { Mail, MapPin, MapPinOff, Phone, RefreshCw, Search, UserRound, X } from 'lucide-react';

const ROLE_VT05 = 'VT-05';

export function AreaAssignmentPage() {
  const [users, setUsers] = useState<UserOption[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  const [assignedAreas, setAssignedAreas] = useState<AssignedArea[]>([]);
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

  return (
    <div className="container mx-auto space-y-6 py-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-emerald-100 p-2.5 text-emerald-700">
            <MapPin className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Phân công địa bàn quản lý</h1>
            <p className="text-sm text-muted-foreground">
              Gán hoặc gỡ địa bàn phụ trách (tỉnh/xã) cho cán bộ quản lý ngành
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="admin-account-areas" />
          <Button variant="outline" onClick={handleRefreshAll} disabled={usersLoading}>
            <RefreshCw className={`h-4 w-4 ${usersLoading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* ── Cột trái: chọn cán bộ + địa bàn đã gán ── */}
        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle className="text-base">Chọn cán bộ</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col space-y-4">
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
              <ul className="max-h-52 space-y-1 overflow-y-auto rounded-lg border p-1.5" data-testid="user-list">
                {filteredUsers.map((user) => (
                  <li key={user.userId}>
                    <button
                      type="button"
                      onClick={() => setSelectedUserId(user.userId)}
                      className={`w-full rounded-md px-3 py-2 text-left transition-colors ${
                        user.userId === selectedUserId
                          ? 'bg-emerald-100 text-emerald-900'
                          : 'hover:bg-muted/60'
                      }`}
                    >
                      <span className="block text-sm font-medium">{user.fullName}</span>
                      <span className="block text-xs text-muted-foreground">
                        @{user.username}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {selectedUser && (
              <div className="space-y-4 rounded-lg border bg-muted/30 p-3">
                <div className="flex items-start gap-3">
                  <div className="rounded-full bg-emerald-100 p-2 text-emerald-700">
                    <UserRound className="h-5 w-5" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold">{selectedUser.fullName}</p>
                    <p className="truncate text-xs text-muted-foreground">
                      @{selectedUser.username} · {selectedUser.organizationName}
                    </p>
                    <div className="mt-1 flex flex-wrap gap-x-3 gap-y-0.5 text-xs text-muted-foreground">
                      {selectedUser.email && (
                        <span className="inline-flex items-center gap-1">
                          <Mail className="h-3 w-3" />
                          {selectedUser.email}
                        </span>
                      )}
                      {selectedUser.phone && (
                        <span className="inline-flex items-center gap-1">
                          <Phone className="h-3 w-3" />
                          {selectedUser.phone}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <p className="text-sm font-medium">Địa bàn đã gán</p>
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
                    <ul className="space-y-1.5" data-testid="assigned-area-list">
                      {assignedAreas.map((area) => (
                        <li key={area.assignmentId}>
                          <Badge variant="outline" className="max-w-full gap-1 pr-1">
                            <MapPin className="h-3 w-3 text-emerald-500" />
                            <span className="truncate">
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
                              className="h-4 w-4 p-0"
                            >
                              <X className="size-3" />
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
        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle className="text-base">Gán địa bàn mới</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col space-y-4">
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
