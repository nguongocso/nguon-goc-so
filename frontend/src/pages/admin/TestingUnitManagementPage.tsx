import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { BadgeCheck, Loader2, ShieldCheck, Users } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
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

import { getTestingUnits } from "@/api/certificationApi";
import type { TestingUnit } from "@/types/certification";

const PAGE_SIZE = 10;

/** Ngày hiện tại dạng YYYY-MM-DD (giờ địa phương). */
const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

/**
 * Trang danh sách đơn vị kiểm nghiệm trong danh mục dùng chung.
 * Route: /admin/testing-units
 * Người quản trị (VT-01) xem danh sách và quản lý phạm vi công nhận.
 */
export default function TestingUnitManagementPage() {
  const navigate = useNavigate();

  const [units, setUnits] = useState<TestingUnit[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [isActiveFilter, setIsActiveFilter] = useState<
    boolean | undefined
  >(undefined);

  const fetchUnits = async () => {
    setLoading(true);
    try {
      const data = await getTestingUnits({
        isActive: isActiveFilter,
        page: currentPage,
        size: PAGE_SIZE,
      });
      setUnits(data.items);
      setTotalElements(data.totalElements);
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
  }, [currentPage, isActiveFilter]);

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
            Danh mục dùng chung — quản lý thông tin và phạm vi công nhận
            của các phòng thí nghiệm / đơn vị kiểm nghiệm.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="testing-unit-management" />
        </div>
      </div>

      {/* Bộ lọc trạng thái */}
      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant={isActiveFilter === undefined ? "default" : "outline"}
          size="sm"
          onClick={() => setIsActiveFilter(undefined)}
          className="h-8 rounded-lg text-xs"
        >
          Tất cả
        </Button>
        <Button
          type="button"
          variant={isActiveFilter === true ? "default" : "outline"}
          size="sm"
          onClick={() => setIsActiveFilter(true)}
          className="h-8 rounded-lg text-xs"
        >
          Đang hoạt động
        </Button>
        <Button
          type="button"
          variant={isActiveFilter === false ? "default" : "outline"}
          size="sm"
          onClick={() => setIsActiveFilter(false)}
          className="h-8 rounded-lg text-xs"
        >
          Ngừng hoạt động
        </Button>
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-border bg-muted/40 p-5">
          <CardTitle className="text-base font-semibold text-foreground">
            Danh sách đơn vị kiểm nghiệm
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex items-center justify-center py-16 text-muted-foreground">
              <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
              Đang tải danh sách đơn vị kiểm nghiệm...
            </div>
          ) : units.length === 0 ? (
            <div className="p-8 text-center text-xs text-muted-foreground">
              Chưa có đơn vị kiểm nghiệm nào trong danh mục.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead>Tên đơn vị</TableHead>
                  <TableHead>Mã công nhận</TableHead>
                  <TableHead>Hạn công nhận</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {units.map((unit) => {
                  const expired = isExpired(unit);
                  return (
                    <TableRow key={unit.id}>
                      <TableCell>
                        <div className="flex items-center gap-2.5">
                          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-700">
                            <Users className="h-4 w-4" />
                          </div>
                          <div className="min-w-0">
                            <p className="truncate text-xs font-semibold text-foreground">
                              {unit.name}
                            </p>
                            {unit.contactInfo && (
                              <p className="truncate text-[11px] text-muted-foreground">
                                {unit.contactInfo}
                              </p>
                            )}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="inline-flex items-center gap-1 font-mono text-xs">
                          <BadgeCheck className="h-3 w-3 text-emerald-600" />
                          {unit.accreditationCode}
                        </span>
                      </TableCell>
                      <TableCell className="text-xs">
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
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            navigate(`/admin/testing-units/${unit.id}/scopes`)
                          }
                          className="h-8 rounded-lg text-xs"
                        >
                          <ShieldCheck className="h-3.5 w-3.5 mr-1 text-emerald-600" />
                          Phạm vi công nhận
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
          <div className="p-4">
            <Pagination
              currentPage={currentPage}
              totalPages={Math.max(1, Math.ceil(totalElements / PAGE_SIZE))}
              totalElements={totalElements}
              pageSize={PAGE_SIZE}
              loading={loading}
              itemLabel="đơn vị"
              onPageChange={setCurrentPage}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

