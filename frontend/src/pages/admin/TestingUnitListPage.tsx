import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
  BadgeCheck,
  CheckSquare,
  Loader2,
  Pencil,
  Plus,
  PowerOff,
  RefreshCw,
  Search,
  ShieldCheck,
  Users,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
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
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Pagination } from "@/components/common/Pagination";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { HelpButton } from "@/components/help/HelpButton";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";

import { deactivateTestingUnit, getTestingUnits } from "@/api/certificationApi";
import type { TestingUnit } from "@/types/certification";

const PAGE_SIZE = 10;

/** Kích thước tải danh sách đơn vị (client-side search + pagination). */
const LIST_SIZE = 500;

/** Ngày hiện tại dạng YYYY-MM-DD (giờ địa phương). */
const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

type ActiveFilter = "all" | "active" | "inactive";

const filterOptions: { value: ActiveFilter; label: string }[] = [
  { value: "all", label: "Tất cả" },
  { value: "active", label: "Đang hoạt động" },
  { value: "inactive", label: "Ngừng hoạt động" },
];

/**
 * Trang danh sách đơn vị kiểm nghiệm (VT-01 quản lý; mọi vai trò xem được).
 * Route: /admin/testing-units
 */
export default function TestingUnitListPage() {
  const navigate = useNavigate();
  const canManage = usePermission(ROLE_ACCESS.testingUnitScopeManagement);

  const [units, setUnits] = useState<TestingUnit[]>([]);
  const [loading, setLoading] = useState(true);

  const [searchInput, setSearchInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<ActiveFilter>("all");
  const [page, setPage] = useState(0);

  const [deactivateTarget, setDeactivateTarget] = useState<TestingUnit | null>(null);
  const [deactivateSubmitting, setDeactivateSubmitting] = useState(false);

  const fetchUnits = async () => {
    setLoading(true);
    try {
      const data = await getTestingUnits({
        isActive:
          filter === "all" ? undefined : filter === "active",
        page: 0,
        size: LIST_SIZE,
      });
      setUnits(data.items);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể tải danh sách đơn vị kiểm nghiệm"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchUnits();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = searchInput.trim();
    if (next === keyword) return;
    setKeyword(next);
    setPage(0);
  };

  const handleDeactivate = async () => {
    if (!deactivateTarget || deactivateSubmitting) return;
    setDeactivateSubmitting(true);
    try {
      await deactivateTestingUnit(deactivateTarget.id);
      toast.success(
        `Đã ngừng hoạt động đơn vị "${deactivateTarget.name}"`
      );
      setDeactivateTarget(null);
      fetchUnits();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể ngừng hoạt động đơn vị kiểm nghiệm"
      );
    } finally {
      setDeactivateSubmitting(false);
    }
  };

  const visibleList = units.filter((unit) => {
    const kw = keyword.trim().toLowerCase();
    if (!kw) return true;
    return (
      unit.name.toLowerCase().includes(kw) ||
      unit.accreditationCode.toLowerCase().includes(kw)
    );
  });

  const totalElements = visibleList.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
  const currentSafePage = Math.min(page, totalPages - 1);
  const startIndex = currentSafePage * PAGE_SIZE;
  const paginated = visibleList.slice(startIndex, startIndex + PAGE_SIZE);

  const today = toISODate(new Date());

  const isExpired = (unit: TestingUnit) =>
    !!unit.accreditationExpiryDate && unit.accreditationExpiryDate < today;

  useSetBreadcrumb([
    { label: "Dashboard", href: "/dashboard" },
    { label: "Đơn vị kiểm nghiệm" },
  ]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <ShieldCheck className="size-6 text-emerald-600" />
            Đơn vị kiểm nghiệm
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Danh mục dùng chung — thêm, sửa, ngừng hoạt động và quản lý
            phạm vi công nhận của các phòng thí nghiệm / đơn vị kiểm nghiệm.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="testing-unit-management" />
          {canManage && (
            <Button
              variant="create"
              onClick={() => navigate("/admin/testing-units/create")}
            >
              <Plus className="h-4 w-4 mr-1" />
              Tạo đơn vị
            </Button>
          )}
        </div>
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <CardTitle className="text-xl font-bold text-slate-900">
              Danh sách đơn vị kiểm nghiệm
            </CardTitle>
            <div className="flex flex-wrap items-center gap-2">
              <form onSubmit={handleSearch} className="relative w-full sm:w-64">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Tìm theo tên hoặc mã công nhận..."
                  className="h-9 pl-9"
                />
              </form>
              <Select
                value={filter}
                onValueChange={(val) => {
                  setFilter(val as ActiveFilter);
                  setPage(0);
                }}
              >
                <SelectTrigger size="sm" className="w-[180px]">
                  <SelectValue placeholder="Trạng thái">
                    {filterOptions.find((opt) => opt.value === filter)?.label}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {filterOptions.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                variant="outline"
                size="sm"
                onClick={fetchUnits}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
                />
                Làm mới
              </Button>
            </div>
          </div>
        </CardHeader>


        <CardContent className="p-0">
          {loading ? (
            <div className="flex items-center justify-center py-16 text-muted-foreground">
              <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
              Đang tải danh sách đơn vị kiểm nghiệm...
            </div>
          ) : paginated.length === 0 ? (
            <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
              <p className="text-sm text-muted-foreground">
                Chưa có đơn vị kiểm nghiệm nào phù hợp.
              </p>
              {canManage && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate("/admin/testing-units/create")}
                >
                  <Plus className="h-4 w-4 mr-1" />
                  Tạo đơn vị
                </Button>
              )}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="w-12 text-center">STT</TableHead>
                  <TableHead>Tên đơn vị</TableHead>
                  <TableHead>Mã công nhận</TableHead>
                  <TableHead>Thông tin liên hệ</TableHead>
                  <TableHead>Ngày hết hạn</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginated.map((unit, index) => {
                  const expired = isExpired(unit);
                  return (
                    <TableRow key={unit.id}>
                      <TableCell className="text-center font-medium text-muted-foreground">
                        {startIndex + index + 1}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2.5">
                          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-700">
                            <Users className="h-4 w-4" />
                          </div>
                          <div className="min-w-0">
                            <p className="truncate text-xs font-semibold text-foreground">
                              {unit.name}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="inline-flex items-center gap-1 font-mono text-xs">
                          <BadgeCheck className="h-3 w-3 text-emerald-600" />
                          {unit.accreditationCode}
                        </span>
                      </TableCell>
                      <TableCell className="max-w-[220px]">
                        <p className="truncate text-xs text-muted-foreground" title={unit.contactInfo ?? undefined}>
                          {unit.contactInfo || "—"}
                        </p>
                      </TableCell>
                      <TableCell className="text-xs whitespace-nowrap">
                        {unit.accreditationExpiryDate ? (
                          <span className={expired ? "text-red-600 font-medium" : ""}>
                            {unit.accreditationExpiryDate}
                            {expired && " (đã hết hạn)"}
                          </span>
                        ) : (
                          "—"
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={unit.isActive ? "success" : "secondary"}
                          className="rounded-full"
                        >
                          {unit.isActive ? "Đang hoạt động" : "Ngừng hoạt động"}
                        </Badge>
                      </TableCell>


                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              navigate(`/admin/testing-units/${unit.id}/scopes`)
                            }
                            className="h-8 rounded-lg text-xs"
                          >
                            <CheckSquare className="h-3.5 w-3.5 mr-1 text-emerald-600" />
                            Phạm vi
                          </Button>
                          {canManage && (
                            <>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() =>
                                  navigate(`/admin/testing-units/${unit.id}/edit`)
                                }
                                className="h-8 rounded-lg text-xs"
                                title="Chỉnh sửa"
                              >
                                <Pencil className="h-3.5 w-3.5" />
                              </Button>
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() => setDeactivateTarget(unit)}
                                disabled={!unit.isActive}
                                className="h-8 rounded-lg text-xs text-destructive hover:text-destructive hover:bg-muted"
                                title={
                                  unit.isActive
                                    ? "Ngừng hoạt động"
                                    : "Đơn vị đã ngừng hoạt động"
                                }
                              >
                                <PowerOff className="h-3.5 w-3.5" />
                              </Button>
                            </>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
          <div className="p-4">
            <Pagination
              currentPage={currentSafePage}
              totalPages={totalPages}
              totalElements={totalElements}
              pageSize={PAGE_SIZE}
              loading={loading}
              itemLabel="đơn vị"
              onPageChange={setPage}
            />
          </div>
        </CardContent>
      </Card>


      {/* AlertDialog xác nhận ngừng hoạt động */}
      <AlertDialog
        open={!!deactivateTarget}
        onOpenChange={(open) => !open && setDeactivateTarget(null)}
      >
        <AlertDialogPopup>
          <AlertDialogHeader>
            <AlertDialogTitle>Ngừng hoạt động đơn vị kiểm nghiệm</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc muốn ngừng hoạt động đơn vị{" "}
              <strong>{deactivateTarget?.name}</strong> không? Đơn vị sẽ không
              còn xuất hiện trong danh sách lựa chọn khi tạo yêu cầu kiểm nghiệm.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setDeactivateTarget(null)}
              disabled={deactivateSubmitting}
            >
              Hủy
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeactivate}
              disabled={deactivateSubmitting}
              className="bg-red-600 hover:bg-red-700"
            >
              {deactivateSubmitting ? "Đang xử lý..." : "Ngừng hoạt động"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogPopup>
      </AlertDialog>
    </div>
  );
}

