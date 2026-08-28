import { Button } from "@/components/ui/button";
import { HelpButton } from "@/components/help/HelpButton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from "@/components/ui/select";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { Pagination } from "@/components/common/Pagination";
import { ListPageHeader } from "@/components/common/ListPageHeader";
import { ListCard } from "@/components/common/ListCard";
import { ListToolbar } from "@/components/common/ListToolbar";
import { SearchInput } from "@/components/common/SearchInput";
import { FilterSelect } from "@/components/common/FilterSelect";
import { RefreshButton } from "@/components/common/RefreshButton";
import { DataTableShell } from "@/components/common/DataTableShell";
import { StatusBadge } from "@/components/common/StatusBadge";
import {
  assignMemberRole,
  getOrganizationMembers,
  getRoles,
} from "@/api/memberApi";
import type { OrganizationMember, RoleOption } from "@/types/member";
import { UserRoundCog, X, MailPlus } from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogPopup,
} from "@/components/ui/alert-dialog";
import { getRoleLabel } from "@/config/roleAccess";

const PAGE_SIZE = 10;

const ROLE_FILTER_OPTIONS = [
  { value: "ALL", label: "Tất cả vai trò" },
  { value: "VT-02", label: "Quản lý hợp tác xã" },
  { value: "VT-03", label: "Người ghi sự kiện" },
  { value: "NONE", label: "Chưa cấp quyền" },
];

const STATUS_FILTER_OPTIONS = [
  { value: "ALL", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Đang hoạt động" },
  { value: "INACTIVE", label: "Đã vô hiệu hóa" },
];

const roleBadgeClasses: Record<string, string> = {
  "VT-02": "bg-blue-100 text-blue-700",
  "VT-03": "bg-purple-100 text-purple-700",
  "VT-04": "bg-orange-100 text-orange-700",
};

const getRoleBadgeClass = (roleCode: string | null) => {
  if (!roleCode) return "bg-slate-100 text-slate-500";
  return roleBadgeClasses[roleCode] ?? "bg-amber-100 text-amber-700";
};

export const MemberList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canCreate = usePermission(ROLE_ACCESS.memberManagement);
  const canInvite = user?.roleCode === "VT-02"; // quyền mời thành viên

  const [members, setMembers] = useState<OrganizationMember[]>([]);
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const [editingMember, setEditingMember] = useState<OrganizationMember | null>(null);
  const [selectedRoleId, setSelectedRoleId] = useState("");

  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [pendingMember, setPendingMember] = useState<OrganizationMember | null>(null);
  const [pendingRoleId, setPendingRoleId] = useState<number | null>(null);
  const [oldManager, setOldManager] = useState<OrganizationMember | null>(null);

  const assignableRoles = useMemo(
    () =>
      roles.filter((role) => role.code === "VT-03"),
    [roles],
  );

  const selectedRole = roles.find(
    (role) => role.roleId === Number(selectedRoleId),
  );

  const fetchMembers = async () => {
    try {
      setIsLoading(true);
      const [memberData, roleData] = await Promise.all([
        getOrganizationMembers(),
        getRoles(),
      ]);
      setMembers(memberData);
      setRoles(roleData);
    } catch {
      toast.error("Không thể tải danh sách thành viên");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void fetchMembers();
  }, []);

  const findCurrentManager = (excludeUserId?: string) => {
    return members.find(
      (m) => m.roleCode === "VT-02" && m.userId !== excludeUserId,
    );
  };

  const filteredMembers = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return members.filter((member) => {
      if (user && member.userId === user.userId) return false;

      const matchesSearch =
        !keyword ||
        [
          member.username,
          member.fullName,
          member.email ?? "",
          member.phone ?? "",
        ].some((value) => value.toLowerCase().includes(keyword));
      const matchesRole =
        roleFilter === "ALL" ||
        (roleFilter === "NONE"
          ? member.roleCode === null
          : member.roleCode === roleFilter);
      const matchesStatus =
        statusFilter === "ALL" || member.status === statusFilter;
      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [members, roleFilter, search, statusFilter, user]);

  const totalPages = Math.max(1, Math.ceil(filteredMembers.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedMembers = filteredMembers.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const openRoleDialog = (member: OrganizationMember) => {
    setEditingMember(member);
    setSelectedRoleId(String(member.roleId));
    setOldManager(null);
  };

  const handleConfirmAssign = async () => {
    if (!pendingMember || !pendingRoleId) return;
    try {
      setIsSaving(true);
      const updatedMember = await assignMemberRole({
        userId: pendingMember.userId,
        roleId: pendingRoleId,
      });
      setMembers((current) =>
        current.map((member) =>
          member.id === updatedMember.id ? updatedMember : member,
        ),
      );
      toast.success(`Đã cập nhật vai trò cho ${pendingMember.fullName}`);
      setEditingMember(null);
      setPendingMember(null);
      setPendingRoleId(null);
      setOldManager(null);
    } catch {
      toast.error("Không thể cập nhật vai trò");
    } finally {
      setIsSaving(false);
      setConfirmDialogOpen(false);
    }
  };

  const saveRole = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!editingMember || !selectedRoleId) return;

    const roleId = Number(selectedRoleId);
    const role = roles.find((r) => r.roleId === roleId);

    if (role?.code === "VT-02") {
      const currentManager = findCurrentManager(editingMember.userId);
      setOldManager(currentManager || null);
      setPendingMember(editingMember);
      setPendingRoleId(roleId);
      setConfirmDialogOpen(true);
      return;
    }

    try {
      setIsSaving(true);
      const updatedMember = await assignMemberRole({
        userId: editingMember.userId,
        roleId,
      });
      setMembers((current) =>
        current.map((member) =>
          member.id === updatedMember.id ? updatedMember : member,
        ),
      );
      toast.success(`Đã cập nhật vai trò cho ${editingMember.fullName}`);
      setEditingMember(null);
    } catch {
      toast.error("Không thể cập nhật vai trò");
    } finally {
      setIsSaving(false);
    }
  };

  const getSelectedRoleLabel = () => {
    if (!selectedRoleId) return "Chọn vai trò";
    const role = assignableRoles.find(r => r.roleId === Number(selectedRoleId));
    return role ? getRoleLabel(role.code) : "Chọn vai trò";
  };

  return (
    <div className="space-y-6">
      {/* Header trang */}
      <ListPageHeader
        icon={UserRoundCog}
        title="Cấp quyền cho thành viên"
        description="Gán hoặc thu vai trò của thành viên trong tổ chức."
        actions={
          <>
            <HelpButton screenKey="member-permissions" />
            {canCreate && (
              <Button
                size="sm"
                variant="create"
                onClick={() => navigate("/members/create")}
              >
                Thêm thành viên
              </Button>
            )}
            {canInvite && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => navigate("/invitations/create")}
              >
                <MailPlus className="h-4 w-4 mr-1" />
                Mời thành viên
              </Button>
            )}
          </>
        }
      />

      {/* Card chính */}
      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm kiếm thành viên..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
              />
              <FilterSelect
                value={roleFilter}
                onValueChange={(value) => {
                  setRoleFilter(value ?? "ALL");
                  setPage(0);
                }}
                options={ROLE_FILTER_OPTIONS}
              />
              <FilterSelect
                value={statusFilter}
                onValueChange={(value) => {
                  setStatusFilter(value ?? "ALL");
                  setPage(0);
                }}
                options={STATUS_FILTER_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={fetchMembers} loading={isLoading} />}
        />

          {/* Bảng */}
          <DataTableShell
            header={
              <>
                <TableHead className="w-12 text-center">STT</TableHead>
                <TableHead>Tài khoản</TableHead>
                <TableHead>Họ và tên</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Số điện thoại</TableHead>
                <TableHead>Vai trò</TableHead>
                <TableHead>Trạng thái</TableHead>
                {canCreate && (
                  <TableHead className="text-center">Thao tác</TableHead>
                )}
              </>
            }
            body={paginatedMembers.map((member, index) => {
              const inactive = member.status === "INACTIVE";
              return (
                <TableRow
                  key={member.id}
                  className={
                    inactive
                      ? "bg-slate-50 opacity-70"
                      : "hover:bg-muted/40 transition-colors"
                  }
                >
                  <TableCell className="text-center font-medium text-muted-foreground">
                    {safePage * PAGE_SIZE + index + 1}
                  </TableCell>
                  <TableCell className="font-semibold text-slate-900">
                    @{member.username}
                  </TableCell>
                  <TableCell className="font-medium text-slate-900">
                    {member.fullName}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {member.email ?? "—"}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {member.phone ?? "—"}
                  </TableCell>
                  <TableCell>
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${getRoleBadgeClass(member.roleCode)}`}
                    >
                      {getRoleLabel(member.roleCode || '') ?? "Chưa cấp quyền"}
                    </span>
                  </TableCell>
                  <TableCell>
                    <StatusBadge
                      label={inactive ? "Đã vô hiệu hóa" : "Đang hoạt động"}
                      tone={inactive ? "danger" : "success"}
                    />
                  </TableCell>
                  {canCreate && (
                    <TableCell className="text-center">
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={inactive}
                        onClick={() => openRoleDialog(member)}
                        className="h-8 text-xs"
                      >
                        {member.roleCode ? "Đổi vai trò" : "Cấp quyền"}
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              );
            })}
            loading={isLoading}
            empty={!isLoading && filteredMembers.length === 0}
            colSpan={canCreate ? 8 : 7}
            loadingMessage="Đang tải danh sách thành viên..."
            emptyMessage="Không tìm thấy thành viên nào."
          />

          {/* Phân trang */}
          <Pagination
            currentPage={safePage}
            totalPages={totalPages}
            totalElements={filteredMembers.length}
            pageSize={PAGE_SIZE}
            loading={isLoading}
            itemLabel="thành viên"
            onPageChange={setPage}
          />
        </ListCard>

      {/* Dialog cấp vai trò */}
      {editingMember && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm">
          <form
            className="w-full max-w-lg overflow-hidden rounded-xl border border-slate-200 bg-white shadow-2xl"
            onSubmit={saveRole}
          >
            <div className="flex justify-between border-b border-slate-200 p-5">
              <div>
                <h2 className="text-lg font-bold text-slate-900">
                  Cấp/đổi vai trò
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  Chọn vai trò phù hợp với phần việc được giao.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setEditingMember(null)}
                aria-label="Đóng"
                className="text-muted-foreground hover:text-slate-900"
              >
                <X className="size-5" />
              </button>
            </div>
            <div className="space-y-5 p-5">
              <div>
                <p className="mb-2 text-sm font-semibold text-slate-900">
                  Thành viên
                </p>
                <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                  <p className="font-semibold">{editingMember.fullName}</p>
                  <p className="text-xs text-muted-foreground">
                    @{editingMember.username}
                    {editingMember.email ? ` · ${editingMember.email}` : ""}
                  </p>
                </div>
              </div>
              <div>
                <p className="mb-2 text-sm font-semibold text-slate-900">
                  Vai trò hiện tại
                </p>
                <div className="rounded-lg border border-slate-200 bg-white px-3 py-3 text-sm">
                  {getRoleLabel(editingMember.roleCode || '') ?? "Chưa cấp quyền"}
                </div>
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-900 mb-2">
                  Vai trò mới <span className="text-red-500">*</span>
                </label>
                <Select
                  value={selectedRoleId}
                  onValueChange={(value) => setSelectedRoleId(value ?? '')}
                >
                  <SelectTrigger>
                    {getSelectedRoleLabel()}
                  </SelectTrigger>
                  <SelectContent>
                    {assignableRoles.map((role) => (
                      <SelectItem
                        key={role.roleId}
                        value={String(role.roleId)}
                      >
                        {getRoleLabel(role.code)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <p className="rounded-lg bg-slate-50 border border-slate-200 p-3 text-xs leading-5 text-slate-700">
                {selectedRole?.code === "VT-02"
                  ? "Quản lý dữ liệu và thành viên trong đúng phạm vi tổ chức."
                  : "Ghi nhật ký và sự kiện; không thể tự cấp quyền cho người khác."}
              </p>
            </div>
            <div className="flex justify-end gap-2 border-t border-slate-200 p-5">
              <Button
                type="button"
                variant="outline"
                onClick={() => setEditingMember(null)}
              >
                Hủy
              </Button>
              <Button
                type="submit"
                variant="create"
                disabled={isSaving || !selectedRoleId}
              >
                {isSaving ? "Đang lưu..." : "Lưu vai trò"}
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* Alert xác nhận */}
      <AlertDialog open={confirmDialogOpen} onOpenChange={setConfirmDialogOpen}>
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle>Xác nhận cấp quyền Quản lý HTX</AlertDialogTitle>
            <AlertDialogDescription className="space-y-2">
              <p>
                Bạn có chắc chắn muốn cấp quyền{" "}
                <strong>Quản lý hợp tác xã (VT-02)</strong> cho{" "}
                <strong>{pendingMember?.fullName}</strong>?
              </p>
              {oldManager && (
                <p className="text-amber-700">
                  <strong>Lưu ý:</strong> Quản lý hiện tại{" "}
                  <strong>{oldManager.fullName}</strong> sẽ tự động bị hạ xuống{" "}
                  <strong>Người ghi sự kiện (VT-03)</strong>.
                </p>
              )}
              <p className="text-slate-600">
                Người này sẽ có toàn quyền quản lý thành viên và dữ liệu trong tổ chức.
              </p>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setConfirmDialogOpen(false)}>
              Hủy
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmAssign}
              className="bg-blue-600 hover:bg-blue-700"
            >
              Xác nhận cấp quyền
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>
    </div>
  );
};