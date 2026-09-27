import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Loader2, Plus, ShieldCheck } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { DeactivateTestingUnitDialog } from "@/components/testing-unit/DeactivateTestingUnitDialog";
import { TestingUnitListTable } from "@/components/testing-unit/TestingUnitListTable";
import {
  TestingUnitListToolbar,
  type TestingUnitStatusFilter,
} from "@/components/testing-unit/TestingUnitListToolbar";
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
  const [filter, setFilter] = useState<TestingUnitStatusFilter>("all");
  const [page, setPage] = useState(0);

  const [deactivateTarget, setDeactivateTarget] = useState<TestingUnit | null>(
    null,
  );
  const [deactivateSubmitting, setDeactivateSubmitting] = useState(false);

  const fetchUnits = async () => {
    setLoading(true);
    try {
      const data = await getTestingUnits({
        isActive: filter === "all" ? undefined : filter === "active",
        page: 0,
        size: LIST_SIZE,
      });
      setUnits(data.items);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể tải danh sách đơn vị kiểm nghiệm",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchUnits();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const handleSearch = (e: FormEvent) => {
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
      toast.success(`Đã ngừng hoạt động đơn vị "${deactivateTarget.name}"`);
      setDeactivateTarget(null);
      fetchUnits();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          "Không thể ngừng hoạt động đơn vị kiểm nghiệm",
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

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
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
            Thêm, sửa, ngừng hoạt động và quản lý phạm vi công nhận của các phòng
            thí nghiệm / đơn vị kiểm nghiệm.
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
        <TestingUnitListToolbar
          searchInput={searchInput}
          onSearchInputChange={setSearchInput}
          onSearchSubmit={handleSearch}
          filter={filter}
          onFilterChange={(value) => {
            setFilter(value);
            setPage(0);
          }}
          loading={loading}
          onRefresh={fetchUnits}
        />

        <CardContent className="p-4 space-y-4">
          {loading ? (
            <div className="flex items-center justify-center py-16 text-muted-foreground">
              <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
              Đang tải danh sách đơn vị kiểm nghiệm...
            </div>
          ) : paginated.length === 0 ? (
            <div className="flex items-center justify-center px-4 py-16 text-center text-muted-foreground">
              <p>Chưa có đơn vị kiểm nghiệm nào phù hợp.</p>
            </div>
          ) : (
            <TestingUnitListTable
              units={paginated}
              startIndex={startIndex}
              canManage={canManage}
              onManageScopes={(unit) =>
                navigate(`/admin/testing-units/${unit.id}/scopes`)
              }
              onEdit={(unit) =>
                navigate(`/admin/testing-units/${unit.id}/edit`)
              }
              onDeactivate={setDeactivateTarget}
            />
          )}
          <div className="border-t border-slate-200 px-4 py-3">
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

      <DeactivateTestingUnitDialog
        target={deactivateTarget}
        submitting={deactivateSubmitting}
        onClose={() => setDeactivateTarget(null)}
        onConfirm={handleDeactivate}
      />
    </div>
  );
}
