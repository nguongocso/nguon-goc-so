import { useEffect, useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { CalendarCheck, Pencil, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
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
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { getCultivationMilestones } from "@/api/cultivationMilestoneApi";
import type { CultivationMilestone } from "@/types/cultivationMilestone";

const PAGE_SIZE = 10;

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
  const [activityType, setActivityType] = useState("ALL");
  const [page, setPage] = useState(0);

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
      const matchActivity = activityType === "ALL" || m.activityType === activityType;
      return matchKeyword && matchActivity;
    });
  }, [milestones, search, activityType]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = useMemo(() => {
    const start = safePage * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, safePage]);

  const openEditPage = (milestone: CultivationMilestone) => {
    navigate(`/admin/cultivation-milestones/${milestone.id}/edit`);
  };

  useSetBreadcrumb([
    { label: "Dashboard", href: "/dashboard" },
    { label: "Mốc canh tác" },
  ]);

  const header = (
    <>
      <TableHead className="w-12 text-center">STT</TableHead>
      <TableHead>Tên mốc</TableHead>
      <TableHead>Loại nông sản</TableHead>
      <TableHead>Tiêu chuẩn</TableHead>
      <TableHead>Loại hoạt động</TableHead>
      <TableHead className="text-center">Ngày dự kiến</TableHead>
      <TableHead className="text-center">Bắt buộc</TableHead>
      {canManage && <TableHead className="text-center">Thao tác</TableHead>}
    </>
  );

  const body = paginated.map((milestone, index) => (
    <TableRow key={milestone.id} className="hover:bg-muted/40 transition-colors">
      <TableCell className="text-center font-medium text-muted-foreground">
        {safePage * PAGE_SIZE + index + 1}
      </TableCell>
      <TableCell className="font-medium text-foreground">{milestone.name}</TableCell>
      <TableCell>{milestone.productCategoryName ?? "Tất cả"}</TableCell>
      <TableCell>{milestone.standardName ?? "Tất cả"}</TableCell>
      <TableCell>{ACTIVITY_LABELS[milestone.activityType] || milestone.activityType}</TableCell>
      <TableCell className="text-center">
        {milestone.expectedDaysFromPlanting ?? "—"}
      </TableCell>
      <TableCell className="text-center">
        {milestone.isMandatory ? (
          <Badge variant="default">Bắt buộc</Badge>
        ) : (
          <Badge variant="secondary">Không bắt buộc</Badge>
        )}
      </TableCell>
      {canManage && (
        <TableCell className="text-center">
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => openEditPage(milestone)}
            title="Sửa mốc canh tác"
          >
            <Pencil className="h-4 w-4" />
          </Button>
        </TableCell>
      )}
    </TableRow>
  ));

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={CalendarCheck}
        title="Quản lý mốc canh tác bắt buộc"
        description="Bộ mốc canh tác được áp dụng theo tiêu chuẩn và loại nông sản gắn cho lô"
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
          colSpan={canManage ? 8 : 7}
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
    </div>
  );
}
