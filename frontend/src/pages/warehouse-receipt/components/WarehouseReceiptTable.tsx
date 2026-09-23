import { Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { StatusBadge } from '@/components/common/StatusBadge';
import type { WarehouseReceiptResponse } from '@/types/warehouseReceipt';
import { formatWarehouseReceiptDate } from '../warehouseReceiptFormatters';

interface WarehouseReceiptTableProps {
  receipts: WarehouseReceiptResponse[];
  page: number;
  pageSize: number;
  onView: (receiptId: string) => void;
}

/** Tiêu đề bảng danh sách phiếu nhập kho. */
export function WarehouseReceiptTableHeader() {
  return (
    <>
      <TableHead className="w-12 text-center">STT</TableHead>
      <TableHead>Mã lô</TableHead>
      <TableHead>Tên lô</TableHead>
      <TableHead className="text-right">Số lượng KN</TableHead>
      <TableHead className="text-right">Thực nhận</TableHead>
      <TableHead className="text-right">Chênh lệch</TableHead>
      <TableHead className="text-center">%</TableHead>
      <TableHead>Ngày nhập</TableHead>
      <TableHead>Người ghi</TableHead>
      <TableHead className="text-center">Thao tác</TableHead>
    </>
  );
}

/** Các hàng dữ liệu của bảng phiếu nhập kho. */
export function WarehouseReceiptTableBody({
  receipts,
  page,
  pageSize,
  onView,
}: WarehouseReceiptTableProps) {
  return receipts.map((receipt, index) => (
    <TableRow key={receipt.id} className="transition-colors hover:bg-muted/40">
      <TableCell className="text-center font-medium text-muted-foreground">
        {page * pageSize + index + 1}
      </TableCell>
      <TableCell className="font-mono text-xs">{receipt.traceCode || '—'}</TableCell>
      <TableCell className="font-medium">{receipt.shipmentName}</TableCell>
      <TableCell className="text-right">
        {receipt.declaredQuantity?.toLocaleString('vi-VN')}
      </TableCell>
      <TableCell className="text-right">
        {receipt.receivedQuantity?.toLocaleString('vi-VN')}
      </TableCell>
      <TableCell className="text-right">
        <span
          className={receipt.discrepancy !== 0 ? 'font-medium text-red-600' : 'text-emerald-600'}
        >
          {(receipt.discrepancy ?? 0) >= 0 ? '+' : ''}
          {receipt.discrepancy?.toLocaleString('vi-VN')}
        </span>
      </TableCell>
      <TableCell className="text-center">
        <StatusBadge
          tone={receipt.isDiscrepancyExceeded ? 'danger' : 'success'}
          label={`${receipt.discrepancyPercent ?? 0}%`}
        />
      </TableCell>
      <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
        {formatWarehouseReceiptDate(receipt.receiptDate)}
      </TableCell>
      <TableCell className="text-sm">{receipt.recordedBy}</TableCell>
      <TableCell className="text-center">
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={() => onView(receipt.id)}
          className="hover:bg-muted"
          title="Xem chi tiết"
        >
          <Eye className="size-4" />
        </Button>
      </TableCell>
    </TableRow>
  ));
}
