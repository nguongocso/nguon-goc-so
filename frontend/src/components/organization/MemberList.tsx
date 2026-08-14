import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  assignMemberRole,
  getOrganizationMembers,
  getRoles,
} from "@/api/memberApi";
import type { OrganizationMember, RoleOption } from "@/types/member";
import { Search, ShieldCheck, UserRoundCog, X, MailPlus } from "lucide-react";
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

  useEffect(() => {
    const loadData = async () => {
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
    void loadData();
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

  const getRoleFilterLabel = () => {
    if (roleFilter === "ALL") return "Tất cả vai trò";
    if (roleFilter === "NONE") return "Chưa cấp quyền";
    return getRoleLabel(roleFilter);
  };

  const getStatusFilterLabel = () => {
    if (statusFilter === "ALL") return "Tất cả trạng thái";
    if (statusFilter === "ACTIVE") return "Đang hoạt động";
    return "Đã vô hiệu hóa";
  };

  const getSelectedRoleLabel = () => {
    if (!selectedRoleId) return "Chọn vai trò";
    const role = assignableRoles.find(r => r.roleId === Number(selectedRoleId));
    return role ? getRoleLabel(role.code) : "Chọn vai trò";
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50/50 via-white to-green-50/30 px-4 py-8 md:px-8">
      <div className="mx-auto max-w-7xl space-y-6">
        {/* Header */}
        <header className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
              Quản lý truy cập
            </p>
            <h1 className="text-2xl font-bold tracking-tight text-emerald-800 md:text-3xl">
              Cấp quyền cho thành viên
            </h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Gán hoặc thu vai trò của thành viên trong tổ chức.
            </p>
          </div>
          <div className="flex items-center gap-3 rounded-xl border border-emerald-200 bg-white/80 px-4 py-3 shadow-sm backdrop-blur-sm">
            <ShieldCheck className="size-5 text-emerald-600" />
            <div>
              <p className="text-xs font-semibold text-emerald-800">
                Phạm vi tổ chức
              </p>
              <p className="text-xs text-emerald-600">
                Đang thao tác với quyền {user?.roleCode}
              </p>
            </div>
          </div>
        </header>

        {/* Card chính */}
        <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
          <CardHeader className="border-b border-emerald-100">
            <div className="flex items-start justify-between gap-4">
              <div>
                <CardTitle className="text-lg font-bold text-emerald-800">
                  Thành viên tổ chức
                </CardTitle>
                <CardDescription>
                  Danh sách thành viên hiện tại cùng vai trò và trạng thái.
                </CardDescription>
              </div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
                  {filteredMembers.length} kết quả
                </span>
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
                    className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
                    onClick={() => navigate("/invitations/create")}
                  >
                    <MailPlus className="h-4 w-4 mr-1" />
                    Mời thành viên
                  </Button>
                )}
              </div>
            </div>
          </CardHeader>

          {/* ... phần còn lại của MemberList giữ nguyên ... */}
          <CardContent className="p-0">
            {/* Bộ lọc */}
            <div className="grid gap-3 border-b border-emerald-100 bg-emerald-50/50 p-4 md:grid-cols-[1fr_220px_200px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  className="bg-white pl-9 border-emerald-200 focus-visible:ring-emerald-100"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Tìm kiếm thành viên..."
                />
              </div>

              <Select
                value={roleFilter}
                onValueChange={(value) => setRoleFilter(value ?? '')}
              >
                <SelectTrigger className="border-emerald-200 focus:ring-emerald-100">
                  {getRoleFilterLabel()}
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Tất cả vai trò</SelectItem>
                  <SelectItem value="VT-02">Quản lý hợp tác xã</SelectItem>
                  <SelectItem value="VT-03">Người ghi sự kiện</SelectItem>
                  <SelectItem value="NONE">Chưa cấp quyền</SelectItem>
                </SelectContent>
              </Select>

              <Select
                value={statusFilter}
                onValueChange={(value) => setStatusFilter(value ?? '')}
              >
                <SelectTrigger className="border-emerald-200 focus:ring-emerald-100">
                  {getStatusFilterLabel()}
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
                  <SelectItem value="ACTIVE">Đang hoạt động</SelectItem>
                  <SelectItem value="INACTIVE">Đã vô hiệu hóa</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Bảng */}
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-emerald-50/50">
                    {[
                      "Tài khoản",
                      "Họ và tên",
                      "Email",
                      "Số điện thoại",
                      "Vai trò",
                      "Trạng thái",
                      ...(canCreate ? ["Thao tác"] : []),
                    ].map((title) => (
                      <TableHead
                        key={title}
                        className="text-emerald-800 font-semibold"
                      >
                        {title}
                      </TableHead>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {isLoading && (
                    <TableRow>
                      <TableCell
                        colSpan={canCreate ? 7 : 6}
                        className="py-12 text-center text-muted-foreground"
                      >
                        Đang tải danh sách thành viên...
                      </TableCell>
                    </TableRow>
                  )}
                  {!isLoading &&
                    filteredMembers.map((member) => {
                      const inactive = member.status === "INACTIVE";
                      return (
                        <TableRow
                          key={member.id}
                          className={
                            inactive
                              ? "bg-slate-50 opacity-70"
                              : "hover:bg-emerald-50/30"
                          }
                        >
                          <TableCell className="font-semibold text-emerald-700">
                            @{member.username}
                          </TableCell>
                          <TableCell className="font-medium">
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
                            <span
                              className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-xs font-semibold ${
                                inactive
                                  ? "bg-red-50 text-red-600"
                                  : "bg-emerald-50 text-emerald-700"
                              }`}
                            >
                              <span
                                className={`size-2 shrink-0 rounded-full ${
                                  inactive ? "bg-red-500" : "bg-emerald-600"
                                }`}
                              />
                              {inactive ? "Đã vô hiệu hóa" : "Đang hoạt động"}
                            </span>
                          </TableCell>
                          {canCreate && (
                            <TableCell>
                              <Button
                                size="sm"
                                variant="outline"
                                disabled={inactive}
                                onClick={() => openRoleDialog(member)}
                                className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
                              >
                                {member.roleCode ? "Đổi vai trò" : "Cấp quyền"}
                              </Button>
                            </TableCell>
                          )}
                        </TableRow>
                      );
                    })}
                </TableBody>
              </Table>
              {!isLoading && !filteredMembers.length && (
                <div className="grid place-items-center px-4 py-16 text-center">
                  <UserRoundCog className="mb-3 size-9 text-emerald-300" />
                  <p className="font-semibold text-emerald-800">
                    Không tìm thấy thành viên
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Hãy thử thay đổi từ khóa hoặc bộ lọc.
                  </p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Dialog cấp vai trò */}
      {editingMember && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 backdrop-blur-sm">
          <form
            className="w-full max-w-lg overflow-hidden rounded-xl border border-emerald-100 bg-white shadow-2xl"
            onSubmit={saveRole}
          >
            <div className="flex justify-between border-b border-emerald-100 p-5">
              <div>
                <h2 className="text-lg font-bold text-emerald-800">
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
                className="text-muted-foreground hover:text-emerald-700"
              >
                <X className="size-5" />
              </button>
            </div>
            <div className="space-y-5 p-5">
              <div>
                <p className="mb-2 text-sm font-semibold text-emerald-800">
                  Thành viên
                </p>
                <div className="rounded-lg border border-emerald-100 bg-emerald-50/50 px-3 py-2">
                  <p className="font-semibold">{editingMember.fullName}</p>
                  <p className="text-xs text-muted-foreground">
                    @{editingMember.username}
                    {editingMember.email ? ` · ${editingMember.email}` : ""}
                  </p>
                </div>
              </div>
              <div>
                <p className="mb-2 text-sm font-semibold text-emerald-800">
                  Vai trò hiện tại
                </p>
                <div className="rounded-lg border border-emerald-100 bg-white px-3 py-3 text-sm">
                  {getRoleLabel(editingMember.roleCode || '') ?? "Chưa cấp quyền"}
                </div>
              </div>
              <div>
                <label className="block text-sm font-semibold text-emerald-800 mb-2">
                  Vai trò mới <span className="text-red-500">*</span>
                </label>
                <Select
                  value={selectedRoleId}
                  onValueChange={(value) => setSelectedRoleId(value ?? '')}
                >
                  <SelectTrigger className="border-emerald-200">
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
              <p className="rounded-lg bg-emerald-50 p-3 text-xs leading-5 text-emerald-800">
                {selectedRole?.code === "VT-02"
                  ? "Quản lý dữ liệu và thành viên trong đúng phạm vi tổ chức."
                  : "Ghi nhật ký và sự kiện; không thể tự cấp quyền cho người khác."}
              </p>
            </div>
            <div className="flex justify-end gap-2 border-t border-emerald-100 p-5">
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