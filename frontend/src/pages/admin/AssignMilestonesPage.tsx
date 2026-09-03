import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import {
  ArrowLeft,
  CalendarCheck,
  Loader2,
  RefreshCw,
  Save,
  Search,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
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
import { Pagination } from "@/components/common/Pagination";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { HelpButton } from "@/components/help/HelpButton";

import { getProductCategories } from "@/api/productCategoryApi";
import {
  assignProductCategoryMilestones,
  getCultivationMilestones,
  getProductCategoryMilestones,
} from "@/api/cultivationMilestoneApi";
import { getActiveStandards } from "@/api/standardApi";
import type { CultivationMilestone } from "@/types/cultivationMilestone";
import type { ProductCategory } from "@/types/productCategory";
import type { Standard } from "@/types/standard";

const PAGE_SIZE = 10;
const CATALOG_SIZE = 1000;

type AssignmentFilter = "all" | "assigned" | "unassigned";

const filterOptions: { value: AssignmentFilter; label: string }[] = [
  { value: "all", label: "Tất cả" },
  { value: "assigned", label: "Đã gán" },
  { value: "unassigned", label: "Chưa gán" },
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

export default function AssignMilestonesPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [category, setCategory] = useState<ProductCategory | null>(null);
  const [categoryLoading, setCategoryLoading] = useState(true);

  const [catalog, setCatalog] = useState<CultivationMilestone[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);

  const [standards, setStandards] = useState<Standard[]>([]);
  const [standardsLoading, setStandardsLoading] = useState(false);

  const [selectedStandardId, setSelectedStandardId] = useState<string>("");

  const [assignedIds, setAssignedIds] = useState<Set<number>>(new Set());
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [mandatoryIds, setMandatoryIds] = useState<Set<number>>(new Set());

  const [searchInput, setSearchInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<AssignmentFilter>("all");
  const [page, setPage] = useState(0);
  const [saving, setSaving] = useState(false);

  const fetchAssigned = useCallback(
    async (standardId: string, cancelled: { value: boolean }) => {
      try {
        const assigned = await getProductCategoryMilestones(id as string);
        if (cancelled.value) return;
        const scopeAssigned = assigned.filter((m) =>
          standardId ? m.standardId === standardId : m.standardId == null
        );
        setAssignedIds(new Set(scopeAssigned.map((m) => m.milestone.id)));
        setSelectedIds(new Set(scopeAssigned.map((m) => m.milestone.id)));
        setMandatoryIds(
          new Set(
            scopeAssigned
              .filter((m) => m.isMandatory)
              .map((m) => m.milestone.id)
          )
        );
      } catch (error: any) {
        if (!cancelled.value) {
          toast.error(
            error.response?.data?.message ||
              "Không thể tải mốc canh tác của loại nông sản"
          );
        }
      }
    },
    [id]
  );

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    const fetchCategory = async () => {
      try {
        const categories = await getProductCategories({});
        if (cancelled) return;
        const found = categories.find((c) => c.id === id) ?? null;
        if (cancelled) return;
        setCategory(found ?? null);
        if (!found) toast.error("Không tìm thấy loại nông sản.");
      } catch (error: any) {
        if (!cancelled) {
          toast.error(
            error.response?.data?.message ||
              "Không thể tải thông tin loại nông sản"
          );
        }
      } finally {
        if (!cancelled) setCategoryLoading(false);
      }
    };

    void fetchCategory();
    void fetchAssigned("", { value: false });

    return () => {
      cancelled = true;
    };
  }, [id, fetchAssigned]);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    setStandardsLoading(true);
    getActiveStandards()
      .then((data) => {
        if (!cancelled) setStandards(data || []);
      })
      .catch(() => {
        if (!cancelled) setStandards([]);
      })
      .finally(() => {
        if (!cancelled) setStandardsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  // Reload assigned list when the selected standard scope changes
  useEffect(() => {
    if (!id) return;
    let cancelled = { value: false };
    setPage(0);
    const cancelledRef = cancelled;
    void fetchAssigned(selectedStandardId, cancelledRef);
    return () => {
      cancelledRef.value = true;
    };
  }, [id, selectedStandardId, fetchAssigned]);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    setCatalogLoading(true);
    getCultivationMilestones({
      keyword: keyword || undefined,
      status: "ACTIVE",
      page: 0,
      size: CATALOG_SIZE,
    })
      .then((data) => {
        if (!cancelled) setCatalog(data.items);
      })
      .catch((error: any) => {
        if (!cancelled) {
          toast.error(
            error.response?.data?.message ||
              "Không thể tải danh sách mốc canh tác"
          );
        }
      })
      .finally(() => {
        if (!cancelled) setCatalogLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id, keyword]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const next = searchInput.trim();
    if (next === keyword) return;
    setKeyword(next);
    setPage(0);
  };

  const handleRefresh = () => {
    setPage(0);
    void getCultivationMilestones({
      keyword: keyword || undefined,
      status: "ACTIVE",
      page: 0,
      size: CATALOG_SIZE,
    }).then((data) => setCatalog(data.items));
    void fetchAssigned(selectedStandardId, { value: false });
  };

  const handleToggle = (milestoneId: number, checked: boolean) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(milestoneId);
      else {
        next.delete(milestoneId);
        setMandatoryIds((prevM) => {
          const nm = new Set(prevM);
          nm.delete(milestoneId);
          return nm;
        });
      }
      return next;
    });
  };

  const handleToggleMandatory = (milestoneId: number, checked: boolean) => {
    setMandatoryIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(milestoneId);
      else next.delete(milestoneId);
      return next;
    });
  };

  const handleSave = async () => {
    if (!category || saving) return;
    setSaving(true);
    try {
      const saved = await assignProductCategoryMilestones(category.id, {
        milestoneIds: Array.from(selectedIds),
        standardId: selectedStandardId || undefined,
        mandatoryMilestoneIds: Array.from(mandatoryIds),
      });
      toast.success(`Đã cập nhật mốc canh tác cho "${category.name}"`);
      const scopeAssigned = saved.filter((m) =>
        selectedStandardId
          ? m.standardId === selectedStandardId
          : m.standardId == null
      );
      setAssignedIds(new Set(scopeAssigned.map((m) => m.milestone.id)));
      setSelectedIds(new Set(scopeAssigned.map((m) => m.milestone.id)));
      setMandatoryIds(
        new Set(
          scopeAssigned
            .filter((m) => m.isMandatory)
            .map((m) => m.milestone.id)
        )
      );
      setPage(0);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || "Không thể cập nhật mốc canh tác"
      );
    } finally {
      setSaving(false);
    }
  };

  const visibleList = catalog.filter((milestone) => {
    if (filter === "assigned") return assignedIds.has(milestone.id);
    if (filter === "unassigned") return !assignedIds.has(milestone.id);
    return true;
  });

  const totalElements = visibleList.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
  const currentSafePage = Math.min(page, totalPages - 1);
  const startIndex = currentSafePage * PAGE_SIZE;
  const paginated = visibleList.slice(startIndex, startIndex + PAGE_SIZE);

  useSetBreadcrumb(
    category
      ? [
          { label: "Dashboard", href: "/dashboard" },
          { label: "Danh mục nông sản", href: "/admin/product-categories" },
          { label: `Gán mốc canh tác — ${category.name}` },
        ]
      : null
  );

  if (categoryLoading && !category) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
        Đang tải thông tin loại nông sản...
      </div>
    );
  }

  if (!category) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Gán mốc canh tác
        </h1>
        <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
          <CardContent className="p-8 text-center text-muted-foreground">
            Loại nông sản không tồn tại hoặc đã bị xóa.
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <CalendarCheck className="size-6 text-emerald-600" />
            Gán mốc canh tác bắt buộc
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Loại nông sản:{" "}
            <span className="font-semibold text-slate-900">{category.name}</span>{" "}
            — Gán các mốc canh tác theo phạm vi tiêu chuẩn.
          </p>
        </div>
        <HelpButton screenKey="admin-cultivation-milestones" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <CardTitle className="text-xl font-bold text-slate-900">
              Danh sách mốc canh tác
            </CardTitle>
            <div className="flex flex-wrap items-center gap-2">
              <Select
                value={selectedStandardId}
                onValueChange={(v) => setSelectedStandardId(v || "")}
              >
                <SelectTrigger size="sm" className="w-[240px]">
                  <SelectValue placeholder="Phạm vi tiêu chuẩn" />
                </SelectTrigger>
                <SelectContent className="max-h-[220px]">
                  <SelectItem value="">Áp dụng cho mọi tiêu chuẩn (GLOBAL)</SelectItem>
                  {standards.map((s) => (
                    <SelectItem key={s.id} value={s.id}>
                      {s.name}
                    </SelectItem>
                  ))}
                  {!standardsLoading && standards.length === 0 && (
                    <div className="px-3 py-2 text-sm text-muted-foreground">
                      Chưa có tiêu chuẩn chất lượng
                    </div>
                  )}
                </SelectContent>
              </Select>
              <form onSubmit={handleSearch} className="relative w-full sm:w-64">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Tìm theo tên mốc..."
                  className="h-9 pl-9"
                />
              </form>
              <Select
                value={filter}
                onValueChange={(v) => {
                  setFilter(v as AssignmentFilter);
                  setPage(0);
                }}
              >
                <SelectTrigger size="sm" className="w-[160px]">
                  <SelectValue placeholder="Trạng thái" />
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
                onClick={handleRefresh}
                disabled={catalogLoading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-1 ${catalogLoading ? "animate-spin" : ""}`}
                />
                Làm mới
              </Button>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-4 space-y-4">
          <div className="rounded-md border overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="w-12 text-center">STT</TableHead>
                  <TableHead>Tên mốc</TableHead>
                  <TableHead>Loại hoạt động</TableHead>
                  <TableHead className="text-center">Ngày dự kiến</TableHead>
                  <TableHead className="text-center">Bắt buộc</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-center">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {catalogLoading ? (
                  <TableRow>
                    <TableCell
                      colSpan={7}
                      className="h-32 text-center text-muted-foreground"
                    >
                      <div className="flex flex-col items-center justify-center gap-2">
                        <RefreshCw className="w-6 h-6 animate-spin text-emerald-600" />
                        <span>Đang tải danh sách mốc canh tác...</span>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : paginated.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={7}
                      className="h-32 text-center text-muted-foreground"
                    >
                      Không có mốc canh tác phù hợp.
                    </TableCell>
                  </TableRow>
                ) : (
                  paginated.map((milestone, index) => {
                    const isAssigned = assignedIds.has(milestone.id);
                    const isSelected = selectedIds.has(milestone.id);
                    const isMandatory = mandatoryIds.has(milestone.id);
                    return (
                      <TableRow
                        key={milestone.id}
                        className="hover:bg-muted/40 transition-colors"
                      >
                        <TableCell className="text-center font-medium text-muted-foreground">
                          {index + 1 + currentSafePage * PAGE_SIZE}
                        </TableCell>
                        <TableCell className="font-medium">
                          {milestone.name}
                        </TableCell>
                        <TableCell>
                          {ACTIVITY_LABELS[milestone.activityType] ||
                            milestone.activityType}
                        </TableCell>
                        <TableCell className="text-center">
                          {milestone.expectedDaysFromPlanting ?? "—"}
                        </TableCell>
                        <TableCell className="text-center">
                          <div
                            className="flex items-center justify-center gap-1.5"
                            aria-disabled={!isSelected}
                          >
                            <Switch
                              checked={isMandatory}
                              size="sm"
                              disabled={!isSelected}
                              onCheckedChange={(checked) =>
                                handleToggleMandatory(milestone.id, checked)
                              }
                              aria-label={`Bắt buộc ${milestone.name}`}
                            />
                            <span className="text-xs text-muted-foreground w-10 text-left">
                              {isMandatory ? "Bắt buộc" : "Tùy chọn"}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant={isAssigned ? "default" : "secondary"}>
                            {isAssigned ? "Đã gán" : "Chưa gán"}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex items-center justify-center gap-2">
                            <Switch
                              checked={isSelected}
                              size="sm"
                              onCheckedChange={(checked) =>
                                handleToggle(milestone.id, checked)
                              }
                              aria-label={`Chọn/bỏ ${milestone.name}`}
                            />
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          </div>
          <Pagination
            currentPage={currentSafePage}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={PAGE_SIZE}
            loading={catalogLoading}
            itemLabel="mốc canh tác"
            onPageChange={setPage}
          />

          {selectedIds.size < assignedIds.size && (
            <p className="text-xs text-amber-600">
              Bạn đang bỏ gán một số mốc canh tác đã lưu. Sau khi lưu, bộ mốc sẽ
              được cập nhật theo lựa chọn mới nhất.
            </p>
          )}
        </CardContent>
      </Card>

      <div className="flex items-center justify-between border-t pt-4">
        <Button
          variant="outline"
          onClick={() => navigate("/admin/product-categories")}
          disabled={saving}
        >
          <ArrowLeft className="h-4 w-4 mr-1" />
          Quay lại
        </Button>
        <Button onClick={handleSave} disabled={catalogLoading || saving}>
          {saving ? (
            <Loader2 className="h-4 w-4 mr-1 animate-spin" />
          ) : (
            <Save className="h-4 w-4 mr-1" />
          )}
          {saving ? "Đang lưu..." : "Lưu bộ mốc canh tác"}
        </Button>
      </div>
    </div>
  );
}
