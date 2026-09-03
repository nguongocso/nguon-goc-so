import { useEffect, useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Eye, EyeOff, Pencil, Plus, Trash2, CalendarCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { HelpButton } from "@/components/help/HelpButton";
import { Pagination } from "@/components/common/Pagination";
import { ListPageHeader } from "@/components/common/ListPageHeader";
import { ListCard } from "@/components/common/ListCard";
import { ListToolbar } from "@/components/common/ListToolbar";
import { SearchInput } from "@/components/common/SearchInput";
import { FilterSelect } from "@/components/common/FilterSelect";
import { RefreshButton } from "@/components/common/RefreshButton";
import { DataTableShell } from "@/components/common/DataTableShell";
import { StatusBadge } from "@/components/common/StatusBadge";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import {
  deleteCultivationMilestone,
  disableCultivationMilestone,
  enableCultivationMilestone,
  getCultivationMilestones,
} from "@/api/cultivationMilestoneApi";
import type { CultivationMilestone } from "@/types/cultivationMilestone";
import { CultivationMilestoneForm } from "@/components/admin/cultivation-milestone/CultivationMilestoneForm";

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: "ALL", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Đang hoạt động" },
  { value: "INACTIVE", label: "Ngừng sử dụng" },
];

const ACTIVITY_TYPE_OPTIONS = [
  { value: "ALL", label: "Tất cả hoạt động" },
  { value: "PLANTING", label: "Gieo trồng" },
  { value: "WATERING", label: "Tưới nước" },
  { value: "FERTILIZING", label: "Bón phân" },
  { value: "PESTICIDE", label: "Phun thuốc" },
  { value: "WEEDING", label: "Làm cỏ" },
  { value: "HARVESTING", label: "Thu hoạch" },
  { value: "OTHER", label: "Khác" },
];

const ACTIVITY_LABELS: Record<string, string> = {
  PLANTING: "Gieo trồng",
  WATERING: "Tưới nước",
  FERTILIZING: "Bón phân",
  PESTICIDE: "Phun thuốc",
  WEEDING: "Làm cỏ",
  HARVESTING: "Thu hoạch",
  OTHER: "Khác",
};

export default function CultivationMilestoneManagementPage() {
  const navigate = useNavigate();
  const canManage = usePermission(ROLE_ACCESS.cultivationMilestoneManagement);
  const [milestones, setMilestones] = useState<CultivationMilestone[]>([]);
  const [loading, setLoading] = useState(true);

  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [activityType, setActivityType] = useState("ALL");
  const [page, setPage] = useState(0);

  const [formOpen, setFormOpen] = useState(false);
  const [editingMilestone, setEditingMilestone] = useState<CultivationMilestone | null>(null);
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CultivationMilestone | null>(null);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const data = await getCultivationMilestones({ page: 0, size: 1000 });
      setMilestones(data.items);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể tải danh sách mốc canh tác"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim();
    return milestones.filter((m) => {
      const matchKeyword = !q || m.name.toLowerCase().includes(q);
      const matchStatus = status === "ALL" || m.status === status;
      const matchActivity = activityType === "ALL" || m.activityType === activityType;
      return matchKeyword && matchStatus && matchActivity;
    });
  }, [milestones, search, status, activityType]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = useMemo(() => {
    const start = safePage * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, safePage]);

  const openEditDialog = (milestone: CultivationMilestone) => {
    setEditingMilestone(milestone);
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditingMilestone(null);
  };

  const handleToggleStatus = async (milestone: CultivationMilestone) => {
    if (togglingId !== null) return;
    setTogglingId(milestone.id);
    try {
      if (milestone.status === "ACTIVE") {
        await disableCultivationMilestone(milestone.id);
        toast.success(`Đã ngừng sử dụng mốc "${milestone.name}"`);
      } else {
        await enableCultivationMilestone(milestone.id);
        toast.success(`Đã kích hoạt lại mốc "${milestone.name}"`);
      }
      fetchAll();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Không thể cập nhật trạng thái");
    } finally {
      setTogglingId(null);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget || deleteSubmitting) return;
    setDeleteSubmitting(true);
    try {
      await deleteCultivationMilestone(deleteTarget.id);
      toast.success("Xóa mốc canh tác thành công");
      setDeleteTarget(null);
      fetchAll();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Không thể xóa mốc canh tác");
    } finally {
      setDeleteSubmitting(false);
    }
  };

  useSetBreadcrumb([
    { label: "Dashboard", href: "/dashboard" },
    { label: "Mốc canh tác" },
  ]);

  const header = (
    <>
      <TableHead className="w-12 text-center">STT</TableHead>
      <TableHead>Tên mốc</TableHead>
      <TableHead>Loại hoạt động</TableHead>
      <TableHead className="text-center">Ngày dự kiến</TableHead>
      <TableHead>Mô tả</TableHead>
      <TableHead>Trạng thái</TableHead>
      {canManage && <TableHead className="text-center">Thao tác</TableHead>}
    </>
  );

  const body = paginated.map((milestone, index) => (
    <TableRow key={milestone.id} className="hover:bg-muted/40 transition-colors">
      <TableCell className="text-center font-medium text-muted-foreground">
        {safePage * PAGE_SIZE + index + 1}
      </TableCell>
      <TableCell className="font-medium text-foreground">{milestone.name}</TableCell>
      <TableCell>{ACTIVITY_LABELS[milestone.activityType] || milestone.activityType}</TableCell>
      <TableCell className="text-center">
        {milestone.expectedDaysFromPlanting ?? "—"}
      </TableCell>
      <TableCell className="max-w-[200px] truncate text-muted-foreground">
        {milestone.description || "—"}
      </TableCell>
      <TableCell className="text-center">
        {milestone.status === "ACTIVE" ? (
          <StatusBadge label="Đang hoạt động" tone="success" />
        ) : (
          <StatusBadge label="Ngừng sử dụng" tone="neutral" />
        )}
      </TableCell>
      {canManage && (
        <TableCell className="text-center">
          <div className="flex justify-center items-center gap-1">
            <Button
              variant="ghost"
              size="icon-sm"
              onClick={() => handleToggleStatus(milestone)}
              disabled={togglingId === milestone.id}
              title={
                milestone.status === "ACTIVE"
                  ? "Ngừng sử dụng"
                  : "Kích hoạt lại"
              }
            >
              {milestone.status === "ACTIVE" ? (
                <EyeOff className="h-4 w-4" />
              ) : (
                <Eye className="h-4 w-4" />
              )}
            </Button>
            {!milestone.referenced && (
              <>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  onClick={() => openEditDialog(milestone)}
                  title="Sửa mốc canh tác"
                >
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  onClick={() => setDeleteTarget(milestone)}
                  title="Xóa mốc canh tác"
                  className="text-destructive hover:text-destructive hover:bg-muted"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </>
            )}
          </div>
        </TableCell>
      )}
    </TableRow>
  ));

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={CalendarCheck}
        title="Quản lý mốc canh tác bắt buộc"
        description="Thêm, sửa, ngừng sử dụng các mốc canh tác bắt buộc theo loại nông sản"
        actions={
          <>
            <HelpButton screenKey="admin-cultivation-milestones" />
            {canManage && (
              <Button variant="create" size="sm" onClick={() => navigate("/admin/cultivation-milestones/create")}>
                <Plus className="h-4 w-4 mr-1" /> Thêm mốc canh tác
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
                placeholder="Tìm theo tên mốc..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
              <FilterSelect
                value={status}
                onValueChange={(val) => {
                  setStatus(val || "ALL");
                  setPage(0);
                }}
                options={STATUS_OPTIONS}
              />
              <FilterSelect
                value={activityType}
                onValueChange={(val) => {
                  setActivityType(val || "ALL");
                  setPage(0);
                }}
                options={ACTIVITY_TYPE_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={fetchAll} loading={loading} />}
        />

        <DataTableShell
          header={header}
          body={body}
          loading={loading}
          empty={!loading && filtered.length === 0}
          colSpan={canManage ? 7 : 6}
          loadingMessage="Đang tải danh sách mốc canh tác..."
          emptyMessage="Chưa có mốc canh tác nào."
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="mốc canh tác"
          onPageChange={setPage}
        />
      </ListCard>

      <CultivationMilestoneForm
        open={formOpen}
        onClose={closeForm}
        onSuccess={fetchAll}
        milestone={editingMilestone}
      />

      <AlertDialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa mốc canh tác</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc muốn xóa mốc <strong>{deleteTarget?.name}</strong>{" "}
              không? Hành động này không thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setDeleteTarget(null)} disabled={deleteSubmitting}>
              Hủy
            </AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} disabled={deleteSubmitting} className="bg-red-600 hover:bg-red-700">
              {deleteSubmitting ? "Đang xóa..." : "Xóa"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>
    </div>
  );
}
