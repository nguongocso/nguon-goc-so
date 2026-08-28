import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import {
  ArrowLeft,
  BadgeCheck,
  ListChecks,
  Loader2,
  RefreshCw,
  Save,
  Search,
  ShieldCheck,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
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

import { getTestingUnits, getAccreditationScopes, updateAccreditationScopes } from "@/api/certificationApi";
import { getInspectionCriteria } from "@/api/inspectionCriterionApi";
import type { TestingUnit } from "@/types/certification";
import type { InspectionCriterion } from "@/types/inspectionCriterion";

/** Số chỉ tiêu hiển thị trên mỗi trang (client-side pagination). */
const PAGE_SIZE = 10;

/** Kích thước tải danh mục chỉ tiêu ACTIVE (đồng bộ convention AssignInspectionCriteriaPage). */
const CATALOG_SIZE = 1000;

type ScopeFilter = "all" | "accredited" | "not_accredited";

const filterOptions: { value: ScopeFilter; label: string }[] = [
  { value: "all", label: "Tất cả" },
  { value: "accredited", label: "Đã công nhận" },
  { value: "not_accredited", label: "Chưa công nhận" },
];

/**
 * Trang quản lý phạm vi công nhận của một đơn vị kiểm nghiệm.
 * Route: /admin/testing-units/:id/scopes
 *
 * Semantics: PUT /api/v1/testing-units/{id}/accreditation-scopes — REPLACE toàn bộ.
 * Người quản trị (VT-01) chọn các chỉ tiêu mà đơn vị được công nhận thực hiện.
 */
export default function TestingUnitScopeManagerPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [unit, setUnit] = useState<TestingUnit | null>(null);
  const [unitLoading, setUnitLoading] = useState(true);

  const [catalog, setCatalog] = useState<InspectionCriterion[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);

  /** Tập chỉ tiêu ĐÃ LƯU trong phạm vi công nhận (baseline). */
  const [accreditedIds, setAccreditedIds] = useState<Set<number>>(new Set());
  /** Lựa chọn HIỆN TẠI của người quản trị (đang chuẩn bị lưu). */
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const [searchInput, setSearchInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<ScopeFilter>("all");
  const [page, setPage] = useState(0);
  const [saving, setSaving] = useState(false);

  // Load đơn vị kiểm nghiệm + phạm vi công nhận đã lưu (baseline).
  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    const fetchUnit = async () => {
      try {
        // Tái sử dụng API list sẵn có (giống AssignInspectionCriteriaPage).
        const unitPage = await getTestingUnits({ page: 0, size: 500 });
        if (cancelled) return;
        const found = unitPage.items.find((u) => u.id === id) ?? null;
        if (cancelled) return;
        setUnit(found ?? null);
        if (!found) {
          toast.error("Không tìm thấy đơn vị kiểm nghiệm.");
        }
      } catch (error: any) {
        if (!cancelled) {
          toast.error(
            error.response?.data?.message ||
              "Không thể tải thông tin đơn vị kiểm nghiệm"
          );
        }
      } finally {
        if (!cancelled) setUnitLoading(false);
      }
    };

    const fetchScopes = async () => {
      try {
        const summary = await getAccreditationScopes(id);
        if (cancelled) return;
        const ids = new Set(summary.accreditedCriteria.map((c) => c.id));
        setAccreditedIds(ids);
        setSelectedIds(ids);
      } catch (error: any) {
        if (!cancelled) {
          toast.error(
            error.response?.data?.message ||
              "Không thể tải phạm vi công nhận của đơn vị"
          );
        }
      }
    };

    void fetchUnit();
    void fetchScopes();

    return () => {
      cancelled = true;
    };
  }, [id]);

  // Load danh mục chỉ tiêu ACTIVE khớp keyword (search server-side).
  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    setCatalogLoading(true);
    getInspectionCriteria({
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
              "Không thể tải danh sách chỉ tiêu kiểm nghiệm"
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
    void getInspectionCriteria({
      keyword: keyword || undefined,
      status: "ACTIVE",
      page: 0,
      size: CATALOG_SIZE,
    })
      .then((data) => setCatalog(data.items))
      .catch((error: any) =>
        toast.error(
          error.response?.data?.message ||
            "Không thể tải danh sách chỉ tiêu kiểm nghiệm"
        )
      );
  };

  const handleFilterChange = (value: ScopeFilter | null) => {
    if (!value) return;
    setFilter(value);
    setPage(0);
  };

  const handleToggle = (criterionId: number, checked: boolean) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(criterionId);
      } else {
        next.delete(criterionId);
      }
      return next;
    });
  };

  const handleSave = async () => {
    if (!unit || saving) return;
    setSaving(true);
    try {
      // REPLACE-ALL: gửi TẬP đầy đủ selectedIds hiện tại.
      const saved = await updateAccreditationScopes(unit.id, {
        criterionDefinitionIds: Array.from(selectedIds),
      });
      toast.success(`Đã cập nhật phạm vi công nhận cho "${unit.name}"`);
      const ids = new Set(saved.accreditedCriteria.map((c) => c.id));
      setAccreditedIds(ids);
      setSelectedIds(ids);
      setPage(0);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể cập nhật phạm vi công nhận"
      );
    } finally {
      setSaving(false);
    }
  };

  const visibleList = catalog.filter((criterion) => {
    if (filter === "accredited") return accreditedIds.has(criterion.id);
    if (filter === "not_accredited") return !accreditedIds.has(criterion.id);
    return true;
  });

  const totalElements = visibleList.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
  const currentSafePage = Math.min(page, totalPages - 1);
  const startIndex = currentSafePage * PAGE_SIZE;
  const paginated = visibleList.slice(startIndex, startIndex + PAGE_SIZE);

  useSetBreadcrumb(
    unit
      ? [
          { label: "Dashboard", href: "/dashboard" },
          { label: "Đơn vị kiểm nghiệm", href: "/admin/testing-units" },
          { label: `Phạm vi công nhận — ${unit.name}` },
        ]
      : null
  );

  if (unitLoading && !unit) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
        Đang tải thông tin đơn vị kiểm nghiệm...
      </div>
    );
  }

  if (!unit) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Quản lý phạm vi công nhận
          </h1>
        </div>
        <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
          <CardContent className="p-8 text-center text-muted-foreground">
            Đơn vị kiểm nghiệm không tồn tại hoặc đã bị xóa.
          </CardContent>
        </Card>
      </div>
    );
  }


  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <ShieldCheck className="size-6 text-emerald-600" />
            Phạm vi công nhận
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Đơn vị kiểm nghiệm:{" "}
            <span className="font-semibold text-slate-900">{unit.name}</span>
          </p>
          <div className="flex items-center gap-2 mt-1.5">
            <Badge
              variant="outline"
              className="rounded-full border-emerald-300 bg-emerald-50 text-xs text-emerald-800"
            >
              <BadgeCheck className="h-3 w-3 mr-1" />
              {unit.accreditationCode}
            </Badge>
            {unit.accreditationExpiryDate && (
              <span className="text-xs text-muted-foreground">
                Hết hạn: {unit.accreditationExpiryDate}
              </span>
            )}
          </div>
        </div>
        <div>
          <HelpButton screenKey="testing-unit-accreditation-scope" />
        </div>
      </div>

      {/* Card danh sách chỉ tiêu */}
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-border bg-muted/40 p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <ListChecks className="h-5 w-5" />
              </div>
              <div>
                <CardTitle className="text-base font-semibold text-foreground">
                  Chỉ tiêu được công nhận
                </CardTitle>
                <CardDescription className="text-xs text-muted-foreground mt-0.5">
                  Chọn các chỉ tiêu mà đơn vị được phép thực hiện kiểm nghiệm.
                  Khi tạo yêu cầu với chỉ tiêu ngoài phạm vi, hệ thống sẽ cảnh báo.
                </CardDescription>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Badge
                variant="outline"
                className="rounded-full border-emerald-300 bg-emerald-50 text-xs font-semibold text-emerald-800"
              >
                Đã công nhận: {accreditedIds.size}/{catalog.length} chỉ tiêu
              </Badge>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-5 space-y-4">
          {/* Toolbar Tìm kiếm & Lọc */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-1 flex-wrap items-center gap-2">
              <form onSubmit={handleSearch} className="flex flex-1 min-w-[200px] max-w-xs">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    value={searchInput}
                    onChange={(e) => setSearchInput(e.target.value)}
                    placeholder="Tìm theo tên chỉ tiêu..."
                    className="h-9 pl-9 text-xs rounded-xl border-border"
                  />
                </div>
              </form>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleRefresh}
                className="h-9 rounded-xl text-xs"
              >
                <RefreshCw className="h-3.5 w-3.5 mr-1" />
                Làm mới
              </Button>

              <Select value={filter} onValueChange={handleFilterChange}>
                <SelectTrigger className="h-9 w-auto min-w-[150px] rounded-xl text-xs">
                  <SelectValue>
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
            </div>
          </div>


          {/* Bảng danh sách chỉ tiêu */}
          {catalogLoading ? (
            <div className="flex items-center justify-center py-12 text-muted-foreground">
              <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
              Đang tải danh sách chỉ tiêu...
            </div>
          ) : paginated.length === 0 ? (
            <div className="rounded-xl border border-dashed border-border p-8 text-center text-xs text-muted-foreground">
              Không có chỉ tiêu nào phù hợp với bộ lọc hiện tại.
            </div>
          ) : (
            <div className="rounded-md border overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/50">
                    <TableHead className="w-12 text-center">STT</TableHead>
                    <TableHead>Tên chỉ tiêu</TableHead>
                    <TableHead>Đơn vị</TableHead>
                    <TableHead>Ngưỡng tối đa</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Công nhận</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginated.map((criterion, index) => {
                    const isAccredited = accreditedIds.has(criterion.id);
                    const isSelected = selectedIds.has(criterion.id);
                    return (
                      <TableRow key={criterion.id}>
                        <TableCell className="text-center font-medium text-muted-foreground">
                          {startIndex + index + 1}
                        </TableCell>
                        <TableCell>
                          <p className="text-xs font-semibold leading-snug text-foreground">
                            {criterion.name}
                          </p>
                          {criterion.referenceStandard && (
                            <p className="mt-0.5 text-[11px] text-muted-foreground">
                              {criterion.referenceStandard}
                            </p>
                          )}
                        </TableCell>
                        <TableCell className="text-xs">{criterion.unit}</TableCell>
                        <TableCell className="text-xs">
                          {Number(criterion.maxThreshold)}
                        </TableCell>
                        <TableCell>
                          <Badge variant={isAccredited ? "default" : "secondary"}>
                            {isAccredited ? "Đã công nhận" : "Chưa công nhận"}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            <span className="text-xs text-muted-foreground">
                              {isSelected ? "Công nhận" : "Không"}
                            </span>
                            <Switch
                              checked={isSelected}
                              size="sm"
                              onCheckedChange={(checked) =>
                                handleToggle(criterion.id, checked)
                              }
                              aria-label={`Chọn/bỏ ${criterion.name}`}
                            />
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
          <Pagination
            currentPage={currentSafePage}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={PAGE_SIZE}
            loading={catalogLoading}
            itemLabel="chỉ tiêu"
            onPageChange={setPage}
          />

          {selectedIds.size < accreditedIds.size && (
            <p className="text-xs text-amber-600">
              Bạn đang bỏ công nhận một số chỉ tiêu đã lưu. Sau khi lưu,
              phạm vi công nhận sẽ được cập nhật theo lựa chọn mới nhất.
            </p>
          )}
        </CardContent>
      </Card>

      {/* Footer actions */}
      <div className="flex items-center justify-between border-t pt-4">
        <Button
          variant="outline"
          onClick={() => navigate("/admin/testing-units")}
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
          {saving ? "Đang lưu..." : "Lưu phạm vi công nhận"}
        </Button>
      </div>
    </div>
  );
}

