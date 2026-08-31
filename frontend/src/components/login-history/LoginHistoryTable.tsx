import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { DataTableShell } from "@/components/common/DataTableShell";
import { getRoleLabel } from "@/config/roleAccess";
import type { LoginHistoryItem } from "@/types/loginHistory";

interface Props {
  records: LoginHistoryItem[];
  loading?: boolean;
  /** Chỉ số bản ghi đầu tiên của trang hiện tại (page * size) để đánh số STT. */
  startIndex?: number;
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

const getResultBadgeClass = (result: string) => {
  if (result === "SUCCESS") {
    return "bg-emerald-100 text-emerald-800 hover:bg-emerald-100";
  }

  return "bg-red-100 text-red-800 hover:bg-red-100";
};

const getResultLabel = (result: string) => {
  if (result === "SUCCESS") return "Thành công";
  if (result === "FAILED") return "Thất bại";
  return result;
};

export const LoginHistoryTable = ({
  records,
  loading = false,
  startIndex = 0,
}: Props) => {
  return (
    <DataTableShell
      colSpan={8}
      loading={loading}
      empty={!loading && records.length === 0}
      loadingMessage="Đang tải lịch sử đăng nhập..."
      emptyMessage="Chưa có lịch sử đăng nhập. Không tìm thấy dữ liệu phù hợp với bộ lọc hiện tại."
      header={
        <>
          <TableHead className="w-12 text-center">STT</TableHead>
          <TableHead>Thời gian</TableHead>
          <TableHead>Tài khoản</TableHead>
          <TableHead>Vai trò</TableHead>
          <TableHead>Kết quả</TableHead>
          <TableHead>IP</TableHead>
          <TableHead>Quốc gia</TableHead>
          <TableHead>Địa điểm mới</TableHead>
        </>
      }
      body={records.map((record, index) => (
        <TableRow key={record.id}>
          <TableCell className="text-center font-medium text-muted-foreground">
            {startIndex + index + 1}
          </TableCell>
          <TableCell className="whitespace-nowrap text-sm">
            {formatDate(record.createdAt)}
          </TableCell>
          <TableCell>
            <div>
              <div className="font-medium">{record.usernameInput || "—"}</div>
            </div>
          </TableCell>
          <TableCell>
            {record.roleCode ? getRoleLabel(record.roleCode) : "—"}
          </TableCell>
          <TableCell>
            <Badge className={getResultBadgeClass(record.result)}>
              {getResultLabel(record.result)}
            </Badge>
          </TableCell>
          <TableCell className="font-mono text-xs">
            {record.ipAddress || "—"}
          </TableCell>
          <TableCell>{record.countryCode || "—"}</TableCell>
          <TableCell>
            {record.isNewCountry ? (
              <Badge
                variant="secondary"
                className="bg-amber-100 text-amber-800 hover:bg-amber-100"
              >
                Có
              </Badge>
            ) : (
              <span className="text-muted-foreground">Không</span>
            )}
          </TableCell>
        </TableRow>
      ))}
    />
  );
};
