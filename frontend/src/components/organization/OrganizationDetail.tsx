import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  getOrganizationDetail,
  createOrganizationMember,
} from "@/api/organizationApi";
import type { OrganizationDetailResponse } from "@/types/organization";
import { Plus, CircleUserRound } from "lucide-react";
import type { AddMemberRequest } from "@/types/organization";
import {
  CreateOrganizationMemberForm,
  type CreateOrganizationMemberFormData,
} from "./CreateOrganizationMemberFrom";
import { toast } from "sonner";
import { getRoleLabel } from "@/config/roleAccess";
import { AddExistingUserDialog } from "./AddExistingUserDialog";
import { HelpButton } from "@/components/help/HelpButton";
import { ListPageHeader } from "@/components/common/ListPageHeader";
import { ListCard } from "@/components/common/ListCard";
import { ListToolbar } from "@/components/common/ListToolbar";
import { SearchInput } from "@/components/common/SearchInput";
import { FilterSelect } from "@/components/common/FilterSelect";
import { RefreshButton } from "@/components/common/RefreshButton";
import { DataTableShell } from "@/components/common/DataTableShell";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Pagination } from "@/components/common/Pagination";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const PAGE_SIZE = 10;

const STATUS_FILTER_OPTIONS = [
  { value: "ALL", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Đang hoạt động" },
  { value: "INACTIVE", label: "Ngừng hoạt động" },
];

// Hàm lấy danh sách role theo loại tổ chức
const getAvailableRolesForType = (type: string) => {
  if (type === "COOPERATIVE") {
    return [
      { id: 2, code: "VT-02", name: "Quản lý hợp tác xã" },
      { id: 3, code: "VT-03", name: "Người ghi sự kiện" },
    ];
  } else if (type === "ENTERPRISE") {
    return [{ id: 4, code: "VT-04", name: "Doanh nghiệp thu mua" }];
  } else if (type === "GOVERNMENT") {
    return [{ id: 5, code: "VT-05", name: "Cán bộ ngành" }];
  } else if (type === "SYSTEM") {
    return [{ id: 6, code: "VT-06", name: "Người dùng hệ thống" }];
  }
  return [];
};

// Helper để render badge trạng thái tổ chức với màu sắc và nhãn tiếng Việt
const ProfileStatusBadge = ({ status }: { status: string }) => {
  const normalized = status.toUpperCase();
  const isActive = normalized === "ACTIVE";

  const label = isActive ? "Đang hoạt động" : "Không hoạt động";
  const colorClasses = isActive
    ? "bg-green-500 hover:bg-green-600 text-white"
    : "bg-gray-300 hover:bg-gray-400 text-gray-700";

  return <Badge className={`${colorClasses} ml-2`}>{label}</Badge>;
};

const MemberStatusBadge = ({ status }: { status: string }) => {
  const active = status.toUpperCase() === "ACTIVE";
  return (
    <StatusBadge
      label={active ? "Đang hoạt động" : "Ngừng hoạt động"}
      tone={active ? "success" : "danger"}
    />
  );
};

export function OrganizationDetail() {
  const { id } = useParams();

  const [data, setData] = useState<OrganizationDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [openCreate, setOpenCreate] = useState(false);
  const [openAddExisting, setOpenAddExisting] = useState(false);

  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);

  // Hàm fetch dữ liệu
  const fetchOrganizationDetail = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const detail = await getOrganizationDetail(id);
      setData(detail);
    } catch (error) {
      toast.error("Không thể tải thông tin tổ chức");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrganizationDetail();
  }, [id]);

  const handleCreateMember = async (
    values: CreateOrganizationMemberFormData,
  ) => {
    if (!id) return;

    const payload: AddMemberRequest = {
      username: values.username,
      password: values.password,
      fullName: values.fullName,
      phone: values.phone?.trim() ? values.phone.trim() : undefined,
      email: values.email?.trim() ? values.email.trim() : undefined,
      roleId: values.roleId,
    };

    try {
      setSubmitting(true);
      await createOrganizationMember(id, payload);
      await fetchOrganizationDetail();
      setOpenCreate(false);
      toast.success("Thêm tài khoản thành công");
    } catch (error: any) {
      const message =
        error?.response?.data?.message || "Không thể thêm tài khoản";
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const ORGANIZATION_TYPE_LABELS: Record<string, string> = {
    COOPERATIVE: "Hợp tác xã",
    ENTERPRISE: "Doanh nghiệp",
    GOVERNMENT: "Cán bộ ngành",
    SYSTEM: "Tổ chức hệ thống",
  };

  const availableRoles = getAvailableRolesForType(data?.profile.type ?? "");

  const roleFilterOptions = useMemo(
    () => [
      { value: "ALL", label: "Tất cả vai trò" },
      ...availableRoles.map((role) => ({
        value: role.code,
        label: role.name,
      })),
    ],
    [availableRoles],
  );

  const filteredMembers = useMemo(() => {
    if (!data) return [];
    const keyword = search.trim().toLowerCase();
    return data.members.filter((member) => {
      const matchesSearch =
        !keyword ||
        [member.username, member.fullName, member.email, member.phone].some(
          (value) => (value ?? "").toLowerCase().includes(keyword),
        );
      const matchesRole =
        roleFilter === "ALL" || member.roleCode === roleFilter;
      const matchesStatus =
        statusFilter === "ALL" || member.status === statusFilter;
      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [data, search, roleFilter, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredMembers.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedMembers = filteredMembers.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  if (loading) return <div>Đang tải...</div>;
  if (!data) return <div>Không tìm thấy tổ chức</div>;

  const isSystem = data.profile.type === "SYSTEM";

  return (
    <div className="space-y-6">
      {/* Header trang */}
      <ListPageHeader
        icon={CircleUserRound}
        title="Chi tiết tổ chức"
        description={`Quản lý tài khoản của ${data.profile.name}.`}
        actions={
          <>
            <HelpButton screenKey="organization-detail" />
            <DropdownMenu>
              <DropdownMenuTrigger className="bg-green-600 text-white hover:bg-green-700 dark:bg-green-500 dark:hover:bg-green-600 h-9 gap-1.5 px-3 text-sm">
                <Plus className="w-4 h-4" />
                Thêm tài khoản
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => setOpenCreate(true)}>
                  Thêm mới
                </DropdownMenuItem>
                {!isSystem && (
                  <DropdownMenuItem onClick={() => setOpenAddExisting(true)}>
                    Thêm tài khoản đã có
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          </>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Thông tin tổ chức</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <div>
            <b>Mã:</b> {data.profile.code}
          </div>
          <div>
            <b>Tên:</b> {data.profile.name}
          </div>
          <div>
            <b>Loại:</b>{" "}
            {ORGANIZATION_TYPE_LABELS[data.profile.type] ?? data.profile.type}
          </div>
          <div>
            <b>Email:</b> {data.profile.email}
          </div>
          <div>
            <b>SĐT:</b> {data.profile.phone}
          </div>
          <div>
            <b>Địa chỉ:</b> {data.profile.address}
          </div>
          <div>
            <b>Trạng thái:</b>
            <ProfileStatusBadge status={data.profile.status} />
          </div>
        </CardContent>
      </Card>

      {/* Card chính: danh sách tài khoản */}
      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo tài khoản, họ tên, email..."
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
                options={roleFilterOptions}
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
          right={<RefreshButton onClick={fetchOrganizationDetail} loading={loading} />}
        />

        {/* Bảng */}
        <DataTableShell
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Tài khoản</TableHead>
              <TableHead>Họ tên</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Vai trò</TableHead>
              <TableHead>Trạng thái</TableHead>
            </>
          }
          body={paginatedMembers.map((m, index) => (
            <TableRow key={m.id} className="hover:bg-muted/40 transition-colors">
              <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell className="font-semibold text-slate-900">
                @{m.username}
              </TableCell>
              <TableCell className="font-medium text-slate-900">
                {m.fullName}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {m.email || "—"}
              </TableCell>
              <TableCell>
                <span className="rounded-full px-2.5 py-1 text-xs font-semibold bg-slate-100 text-slate-700">
                  {getRoleLabel(m.roleCode)}
                </span>
              </TableCell>
              <TableCell>
                <MemberStatusBadge status={m.status} />
              </TableCell>
            </TableRow>
          ))}
          loading={loading}
          empty={!loading && filteredMembers.length === 0}
          colSpan={6}
          loadingMessage="Đang tải danh sách tài khoản..."
          emptyMessage="Không tìm thấy tài khoản nào."
        />

        {/* Phân trang */}
        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filteredMembers.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="tài khoản"
          onPageChange={setPage}
        />
      </ListCard>

      <Dialog open={openCreate} onOpenChange={setOpenCreate}>
        <DialogContent className="max-w-2xl lg:max-w-4xl xl:max-w-6xl w-full p-4 sm:p-6 lg:p-8 max-h-[90vh] overflow-y-auto">
          <DialogHeader className="border-b pb-4 mb-4">
            <DialogTitle className="text-2xl font-bold">
              Thêm tài khoản mới
            </DialogTitle>
          </DialogHeader>
          <CreateOrganizationMemberForm
            onSubmit={handleCreateMember}
            loading={submitting}
            organizationType={data.profile.type}
          />
        </DialogContent>
      </Dialog>

      {/* Dialog thêm tài khoản đã có (chỉ hiển thị với non-SYSTEM) */}
      {!isSystem && (
        <AddExistingUserDialog
          open={openAddExisting}
          onOpenChange={setOpenAddExisting}
          organizationId={data.profile.organizationId}
          onSuccess={fetchOrganizationDetail}
          availableRoles={availableRoles}
        />
      )}
    </div>
  );
}
