import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Warehouse, Eye, LoaderCircle, FileText } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';
import { WarehouseReceiptCreateDialog } from './components/WarehouseReceiptCreateDialog';

export default function WarehouseReceiptPage() {
  const { list, pageData, isLoadingList, error, fetchList } = useWarehouseReceipt();
  const [createOpen, setCreateOpen] = useState(false);
  const [page, setPage] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    fetchList(page, 10);
  }, [page, fetchList]);

  const handleCreated = () => {
    setCreateOpen(false);
    setPage(0);
    fetchList(0, 10);
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const formatDate = (iso: string) => {
    try {
      return new Date(iso).toLocaleDateString('vi-VN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
      });
    } catch {
      return iso;
    }
  };

  const totalPages = pageData?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Nhập kho</h1>
          <p className="mt-1 text-sm text-slate-500">
            Theo dõi và ghi nhận nhập kho cho các lô hàng đã thu mua.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="warehouse-receipt" />
          <Button variant="create" onClick={() => setCreateOpen(true)}>
            <Plus className="size-4 mr-1" />
            Nhập kho
          </Button>
        </div>
      </div>

      {/* Error */}
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Table Card */}
      <Card className="border-slate-200 bg-white shadow-sm rounded-xl">
        <CardHeader className="pb-3">
          <CardTitle className="flex items-center gap-2 text-lg">
            <Warehouse className="size-5 text-blue-700" />
            Danh sách sự kiện nhập kho
          </CardTitle>
          <CardDescription>
            {pageData ? `${pageData.totalElements} sự kiện` : ''}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoadingList ? (
            <div className="flex items-center justify-center py-12">
              <LoaderCircle className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : list.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
              <FileText className="mb-2 size-10" />
              <p className="text-sm">Chưa có sự kiện nhập kho nào.</p>
              <Button variant="create" className="mt-4" onClick={() => setCreateOpen(true)}>
                <Plus className="size-4 mr-1" />
                Nhập kho
              </Button>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-slate-50">
                      <TableHead className="font-semibold text-slate-700">Mã lô</TableHead>
                      <TableHead className="font-semibold text-slate-700">Tên lô</TableHead>
                      <TableHead className="font-semibold text-slate-700 text-right">Số lượng KN</TableHead>
                      <TableHead className="font-semibold text-slate-700 text-right">Thực nhận</TableHead>
                      <TableHead className="font-semibold text-slate-700 text-right">Chênh lệch</TableHead>
                      <TableHead className="font-semibold text-slate-700 text-center">%</TableHead>
                      <TableHead className="font-semibold text-slate-700">Ngày nhập</TableHead>
                      <TableHead className="font-semibold text-slate-700">Người ghi</TableHead>
                      <TableHead className="font-semibold text-slate-700 text-center">Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {list.map((receipt) => (
                      <TableRow key={receipt.id} className="hover:bg-slate-50/80">
                        <TableCell className="font-mono text-xs">
                          {receipt.traceCode || '—'}
                        </TableCell>
                        <TableCell className="font-medium">{receipt.shipmentName}</TableCell>
                        <TableCell className="text-right">
                          {receipt.declaredQuantity?.toLocaleString('vi-VN')}
                        </TableCell>
                        <TableCell className="text-right">
                          {receipt.receivedQuantity?.toLocaleString('vi-VN')}
                        </TableCell>
                        <TableCell className="text-right">
                          <span className={receipt.discrepancy !== 0 ? 'font-medium text-red-600' : 'text-emerald-600'}>
                            {(receipt.discrepancy ?? 0) >= 0 ? '+' : ''}{receipt.discrepancy?.toLocaleString('vi-VN')}
                          </span>
                        </TableCell>
                        <TableCell className="text-center">
                          {receipt.isDiscrepancyExceeded ? (
                            <Badge variant="destructive" className="text-xs">
                              {receipt.discrepancyPercent ?? 0}%
                            </Badge>
                          ) : (
                            <Badge variant="outline" className="text-xs text-emerald-700 border-emerald-300">
                              {receipt.discrepancyPercent ?? 0}%
                            </Badge>
                          )}
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                          {formatDate(receipt.receiptDate)}
                        </TableCell>
                        <TableCell className="text-sm">{receipt.recordedBy}</TableCell>
                        <TableCell className="text-center">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => navigate(`/warehouse-receipt/${receipt.id}`)}
                            className="hover:bg-muted"
                            title="Xem chi tiết"
                          >
                            <Eye className="size-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between pt-4">
                  <p className="text-sm text-muted-foreground">
                    Trang {page + 1} / {totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page === 0}
                      onClick={() => handlePageChange(page - 1)}
                    >
                      Trước
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page >= totalPages - 1}
                      onClick={() => handlePageChange(page + 1)}
                    >
                      Sau
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Create Dialog */}
      <WarehouseReceiptCreateDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onCreated={handleCreated}
      />
    </div>
  );
}
