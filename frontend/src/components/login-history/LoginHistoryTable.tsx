import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { DataTableShell } from "@/components/common/DataTableShell";
import { getRoleLabel } from "@/config/roleAccess";
import type { LoginHistoryItem } from "@/types/loginHistory";

interface Props {
  records: LoginHistoryItem[];
  loading?: boolean;
}

const formatDate = (iso: string) => {
  try {
    return new Date(iso).toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  } catch {
    return iso;
  }
};

export const LoginHistoryTable = ({ records, loading }: Props) => {
  return (
    <DataTableShell
      loading={loading}
      empty={!loading && records.length === 0}
      colSpan={7}
      loadingMessage="Đang tải lịch sử đăng nhập..."
      emptyMessage="Chưa có lịch sử đăng nhập nào được ghi nhận."
      header={
        <>
          <TableHead className="w-[180px]">Thời gian</TableHead>
          <TableHead className="w-[160px]">Tài khoản</TableHead>
          <TableHead className="w-[180px]">Vai trò</TableHead>
          <TableHead className="w-[140px]">Kết quả</TableHead>
          <TableHead className="w-[140px]">IP</TableHead>
          <TableHead className="w-[120px]">Quốc gia</TableHead>
          <TableHead className="w-[120px]">Địa điểm mới</TableHead>
        </>
      }
      body={
        <>
          {records.map((record) => (
            <TableRow key={record.id} className="transition-colors hover:bg-muted/40">
              <TableCell className="whitespace-nowrap font-mono text-sm text-muted-foreground">
                {formatDate(record.createdAt)}
              </TableCell>
              <TableCell>
                <div className="font-medium text-foreground">{record.usernameInput || "—"}</div>
              </TableCell>
              <TableCell>
                <span className="text-sm text-foreground">
                  {record.roleCode ? getRoleLabel(record.roleCode) : "—"}
                </span>
              </TableCell>
              <TableCell>
                {record.result === "SUCCESS" ? (
                  <Badge variant="outline" className="border-emerald-200 bg-emerald-50 text-emerald-700">
                    Thành công
                  </Badge>
                ) : (
                  <Badge variant="outline" className="border-rose-200 bg-rose-50 text-rose-700">
                    Thất bại
                  </Badge>
                )}
              </TableCell>
              <TableCell className="font-mono text-xs text-muted-foreground">
                {record.ipAddress || "—"}
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {record.countryCode || "—"}
              </TableCell>
              <TableCell>
                {record.isNewCountry ? (
                  <Badge variant="outline" className="border-amber-200 bg-amber-50 text-amber-700">
                    Có
                  </Badge>
                ) : (
                  <span className="text-sm text-muted-foreground">Không</span>
                )}
              </TableCell>
            </TableRow>
          ))}
        </>
      }
    />
  );
};

