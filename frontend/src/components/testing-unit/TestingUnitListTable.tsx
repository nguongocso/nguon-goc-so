import { BadgeCheck, Users } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { TestingUnitRowActions } from "@/components/testing-unit/TestingUnitRowActions";
import type { TestingUnit } from "@/types/certification";

/** Lấy ngày hiện tại dạng YYYY-MM-DD (giờ địa phương). */
const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

interface TestingUnitListTableProps {
  /** Các đơn vị của trang hiện tại đã lọc và phân trang. */
  units: TestingUnit[];
  /** Số thứ tự bắt đầu của trang hiện tại (dùng cho cột STT). */
  startIndex: number;
  /** Người dùng có quyền sửa / ngừng hoạt động hay không. */
  canManage: boolean;
  /** Mở màn hình quản lý phạm vi công nhận của đơn vị. */
  onManageScopes: (unit: TestingUnit) => void;
  /** Mở màn hình chỉnh sửa đơn vị. */
  onEdit: (unit: TestingUnit) => void;
  /** Mở hộp thoại xác nhận ngừng hoạt động đơn vị. */
  onDeactivate: (unit: TestingUnit) => void;
}

/**
 * Bảng danh sách đơn vị kiểm nghiệm (chỉ hiển thị, không tự tải dữ liệu).
 * Cột hành động luôn căn giữa và dùng chung một chuẩn giao diện nút.
 */
export const TestingUnitListTable = ({
  units,
  startIndex,
  canManage,
  onManageScopes,
  onEdit,
  onDeactivate,
}: TestingUnitListTableProps) => {
  const today = toISODate(new Date());

  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200">
      <Table>
        <TableHeader>
          <TableRow className="bg-slate-50/80">
            <TableHead className="w-12 text-center font-semibold text-slate-700">
              STT
            </TableHead>
            <TableHead className="font-semibold text-slate-700">
              Tên đơn vị
            </TableHead>
            <TableHead className="font-semibold text-slate-700">
              Mã công nhận
            </TableHead>
            <TableHead className="font-semibold text-slate-700">
              Thông tin liên hệ
            </TableHead>
            <TableHead className="font-semibold text-slate-700">
              Ngày hết hạn
            </TableHead>
            <TableHead className="font-semibold text-slate-700">
              Trạng thái
            </TableHead>
            <TableHead className="text-center font-semibold text-slate-700">
              Hành động
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {units.map((unit, index) => {
            const expired =
              !!unit.accreditationExpiryDate &&
              unit.accreditationExpiryDate < today;
            return (
              <TableRow
                key={unit.id}
                className="hover:bg-slate-50/60 transition-colors"
              >
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
                  <p
                    className="truncate text-xs text-muted-foreground"
                    title={unit.contactInfo ?? undefined}
                  >
                    {unit.contactInfo || "—"}
                  </p>
                </TableCell>
                <TableCell className="text-xs whitespace-nowrap">
                  {unit.accreditationExpiryDate ? (
                    <span
                      className={
                        expired ? "text-red-600 font-medium" : ""
                      }
                    >
                      {unit.accreditationExpiryDate}
                      {expired && " (đã hết hạn)"}
                    </span>
                  ) : (
                    "—"
                  )}
                </TableCell>
                <TableCell>
                  <Badge
                    variant={unit.isActive ? "success" : "outline"}
                    className={`rounded-full ${
                      unit.isActive
                        ? ""
                        : "border-slate-300 bg-slate-50/60 text-slate-400 font-normal"
                    }`}
                  >
                    {unit.isActive ? "Đang hoạt động" : "Ngừng hoạt động"}
                  </Badge>
                </TableCell>
                <TableCell className="text-center">
                  <TestingUnitRowActions
                    unit={unit}
                    canManage={canManage}
                    onManageScopes={() => onManageScopes(unit)}
                    onEdit={() => onEdit(unit)}
                    onDeactivate={() => onDeactivate(unit)}
                  />
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
};
