import { getCodeRangeStatus } from "@/api/codeRangeApi";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { CodeRangeStatusResponse } from "@/types/codeRange";
import { Plus } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";

const getStatusConfig = (status: string) => {
  switch (status) {
    case "OK":
      return { label: "OK", variant: "success" as const };
    case "NEARLY_EXHAUSTED":
      return { label: "Gần hết", variant: "warning" as const };
    case "EXHAUSTED":
      return { label: "Đã hết", variant: "destructive" as const };
    default:
      return { label: status, variant: "secondary" as const };
  }
};

const CodeRangeListPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [ranges, setRanges] = useState<CodeRangeStatusResponse[]>([]);

  const canCreate = usePermission(ROLE_ACCESS.codeRangeList);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await getCodeRangeStatus();
        setRanges(data);
      } catch (error) {
        toast.error("Không thể tải danh sách dải mã");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-emerald-500 border-t-transparent" />
          <p className="mt-4 text-sm text-muted-foreground">Đang tải...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Quản lý dải mã truy xuất</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Quản lý các dải mã truy xuất đã cấp cho tổ chức
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="admin-code-range-list" />
          {canCreate && (
            <Link to="/admin/code-ranges/create">
              {/* CHANGED: thêm variant="create" */}
              <Button className="shrink-0" variant="create">
                <Plus className="h-4 w-4 mr-2" />
                Cấp dải mã mới
              </Button>
            </Link>
          )}
        </div>
      </div>

      {/* Card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">
            Danh sách dải mã
            <span className="ml-2 text-sm font-normal text-muted-foreground">
              ({ranges.length})
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {ranges.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <div className="rounded-full bg-muted p-4 mb-4">
                <Plus className="h-8 w-8 text-muted-foreground" />
              </div>
              <p className="text-muted-foreground">Chưa có dải mã nào</p>
              <p className="text-sm text-muted-foreground/70 mt-1">
                Nhấn "Cấp dải mã mới" để tạo dải mã cho tổ chức
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="py-3">Tổ chức</TableHead>
                    <TableHead className="py-3">Tiền tố</TableHead>
                    <TableHead className="py-3 text-right">Hạn mức</TableHead>
                    <TableHead className="py-3 text-right">Đã dùng</TableHead>
                    <TableHead className="py-3 text-right">% sử dụng</TableHead>
                    <TableHead className="py-3 text-center">Trạng thái</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {ranges.map((range) => {
                    const config = getStatusConfig(range.status);
                    return (
                      <TableRow
                        key={range.id}
                        className="transition-colors hover:bg-muted/50"
                      >
                        <TableCell className="font-medium">
                          {range.organizationName}
                        </TableCell>
                        <TableCell>
                          <code className="rounded bg-muted px-2 py-0.5 text-sm font-mono">
                            {range.prefix}
                          </code>
                        </TableCell>
                        <TableCell className="text-right font-medium">
                          {range.totalLimit.toLocaleString()}
                        </TableCell>
                        <TableCell className="text-right">
                          {range.usedCount.toLocaleString()}
                        </TableCell>
                        <TableCell className="text-right">
                          <span
                            className={
                              range.usagePercent > 80
                                ? "text-red-600 font-semibold"
                                : range.usagePercent > 50
                                ? "text-yellow-600 font-semibold"
                                : "text-green-600"
                            }
                          >
                            {range.usagePercent.toFixed(1)}%
                          </span>
                        </TableCell>
                        <TableCell className="text-center">
                          <Badge
                            variant={
                              range.status === "OK"
                                ? "default"
                                : range.status === "NEARLY_EXHAUSTED"
                                ? "default"
                                : "destructive"
                            }
                            className={
                              range.status === "NEARLY_EXHAUSTED"
                                ? "bg-yellow-500 hover:bg-yellow-600 text-white"
                                : range.status === "OK"
                                ? "bg-emerald-500 hover:bg-emerald-600 text-white"
                                : ""
                            }
                          >
                            {config.label}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default CodeRangeListPage;