import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
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

export const LoginHistoryTable = ({ records, loading }: Props) => {
  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (records.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        <p className="text-lg font-semibold">Chưa có lịch sử đăng nhập</p>
        <p className="text-sm">Không tìm thấy dữ liệu phù hợp với bộ lọc hiện tại.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Thời gian</TableHead>
            <TableHead>Tài khoản</TableHead>
            <TableHead>Vai trò</TableHead>
            <TableHead>Kết quả</TableHead>
            <TableHead>IP</TableHead>
            <TableHead>Quốc gia</TableHead>
            <TableHead>Địa điểm mới</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {records.map((record) => (
            <TableRow key={record.id}>
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
              <TableCell className="font-mono text-xs">{record.ipAddress || "—"}</TableCell>
              <TableCell>{record.countryCode || "—"}</TableCell>
              <TableCell>
                {record.isNewCountry ? (
                  <Badge variant="secondary" className="bg-amber-100 text-amber-800 hover:bg-amber-100">
                    Có
                  </Badge>
                ) : (
                  <span className="text-muted-foreground">Không</span>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};
